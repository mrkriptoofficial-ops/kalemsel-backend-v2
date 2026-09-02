import os
import secrets
import uvicorn
from fastapi import FastAPI, HTTPException, Header, Depends, Request
from fastapi.middleware.cors import CORSMiddleware
from motor.motor_asyncio import AsyncIOMotorClient
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime
from slugify import slugify
from bson import ObjectId
from bson.errors import InvalidId

app = FastAPI(title="Kalemsel v2 API - Kesin Cozum")

# MONGO BAGLANTISI
# Baglanti adresi artik kod icinde degil, MONGO_URL environment variable'indan okunuyor.
# Ornek: export MONGO_URL="mongodb+srv://<kullanici>:<sifre>@<cluster-adresi>/?retryWrites=true&w=majority"
MONGO_URL = os.environ.get("MONGO_URL")
if not MONGO_URL:
    raise RuntimeError("MONGO_URL environment variable tanimli degil")

client = AsyncIOMotorClient(MONGO_URL)
db = client.kalemsel_v2_database

# CORS - Sitenin her yerden erisilebilir olmasi icin
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- YETKILENDIRME (SADECE ANA KULLANICI PAYLASIM YAPABILIR) ---
# Sunucu ortaminda ADMIN_PASSWORD environment variable'i tanimlanmali.
# Ornek: export ADMIN_PASSWORD="cok-guclu-bir-sifre"
ADMIN_PASSWORD = os.environ.get("ADMIN_PASSWORD")

async def verify_admin(authorization: Optional[str] = Header(None)):
    """
    Paylasim (post olusturma/duzenleme) islemlerini sadece ana kullaniciyla
    sinirlamak icin kullanilir. Istek su sekilde gonderilmeli:
    Authorization: Bearer <ADMIN_PASSWORD>
    """
    if not ADMIN_PASSWORD:
        # Sunucuda sifre tanimlanmamissa kimse paylasim yapamasin.
        raise HTTPException(status_code=500, detail="Sunucuda ADMIN_PASSWORD tanimli degil")
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Yetkisiz erisim: Authorization header eksik")
    token = authorization.removeprefix("Bearer ").strip()
    if not secrets.compare_digest(token, ADMIN_PASSWORD):
        raise HTTPException(status_code=403, detail="Yetkisiz erisim: gecersiz sifre")

# --- VERI YAPISI ---
class Post(BaseModel):
    title: str
    content: str
    content_type: str  # siir, makale, deneme, hikaye
    category: str = "Genel"
    author: str = "Yönetici"
    cover_image: Optional[str] = None

class CommentCreate(BaseModel):
    author: str
    content: str

def parse_object_id(id_str: str, field_name: str = "id") -> ObjectId:
    try:
        return ObjectId(id_str)
    except InvalidId:
        raise HTTPException(status_code=400, detail=f"Gecersiz {field_name}")

def get_client_ip(request: Request) -> str:
    """
    Render/Railway gibi proxy arkasindaki platformlarda gercek istemci IP'si
    X-Forwarded-For header'inda gelir (ilk deger). O yoksa dogrudan baglantiyi kullan.
    """
    forwarded = request.headers.get("x-forwarded-for")
    if forwarded:
        return forwarded.split(",")[0].strip()
    return request.client.host if request.client else "bilinmiyor"

# --- ROTALAR (ENDPOINTS) ---

@app.on_event("startup")
async def create_indexes():
    # Ayni IP'nin ayni postu iki kez begenmesini veritabani seviyesinde de engelle
    # (esiz istek yarisi/race condition durumlarina karsi ek guvence).
    try:
        await db.likes.create_index([("post_id", 1), ("ip", 1)], unique=True)
    except Exception as e:
        print(f"Index olusturma hatasi: {e}")

@app.get("/")
async def root():
    return {"status": "Online", "database": "Bagli", "mesaj": "Kalemsel v2 Basariyla Calisiyor"}

# Yeni icerik ekleme (sadece ana kullanici / admin)
@app.post("/api/posts")
async def create_post(post: Post, _: None = Depends(verify_admin)):
    try:
        post_dict = post.dict()
        post_dict["slug"] = slugify(post.title)
        post_dict["created_at"] = datetime.now()
        post_dict["likes"] = 0
        result = await db.posts.insert_one(post_dict)
        return {"status": "success", "id": str(result.inserted_id), "slug": post_dict["slug"]}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# Tum icerikleri listeleme
@app.get("/api/posts")
async def get_posts():
    try:
        cursor = db.posts.find().sort("created_at", -1)
        posts = []
        async for doc in cursor:
            doc["_id"] = str(doc["_id"])
            posts.append(doc)
        return posts
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# --- BEGENI ---

# Bir posta begeni ekleme (herkes yapabilir, onay gerekmez, ayni IP ayni postu tekrar begenemez)
@app.post("/api/posts/{post_id}/like")
async def like_post(post_id: str, request: Request):
    try:
        oid = parse_object_id(post_id, "post id")
        post = await db.posts.find_one({"_id": oid})
        if not post:
            raise HTTPException(status_code=404, detail="Post bulunamadi")

        ip = get_client_ip(request)
        existing_like = await db.likes.find_one({"post_id": post_id, "ip": ip})
        if existing_like:
            raise HTTPException(status_code=409, detail="Bu gonderiyi zaten begendiniz")

        await db.likes.insert_one({"post_id": post_id, "ip": ip, "created_at": datetime.now()})
        await db.posts.update_one({"_id": oid}, {"$inc": {"likes": 1}})
        updated_post = await db.posts.find_one({"_id": oid})
        return {"status": "success", "likes": updated_post.get("likes", 0)}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# --- YORUMLAR ---

# Yorum ekleme (herkes yapabilir, ama yayinlanmadan once ana kullanici onayi gerekir)
@app.post("/api/posts/{post_id}/comments")
async def create_comment(post_id: str, comment: CommentCreate):
    try:
        oid = parse_object_id(post_id, "post id")
        post = await db.posts.find_one({"_id": oid})
        if not post:
            raise HTTPException(status_code=404, detail="Post bulunamadi")
        comment_dict = comment.dict()
        comment_dict["post_id"] = post_id
        comment_dict["approved"] = False
        comment_dict["created_at"] = datetime.now()
        result = await db.comments.insert_one(comment_dict)
        return {"status": "success", "id": str(result.inserted_id), "mesaj": "Yorumunuz onay bekliyor"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# Bir postun onaylanmis yorumlarini listeleme (herkese acik)
@app.get("/api/posts/{post_id}/comments")
async def get_comments(post_id: str):
    try:
        cursor = db.comments.find({"post_id": post_id, "approved": True}).sort("created_at", 1)
        comments = []
        async for doc in cursor:
            doc["_id"] = str(doc["_id"])
            comments.append(doc)
        return comments
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# Onay bekleyen yorumlari listeleme (sadece ana kullanici)
@app.get("/api/comments/pending")
async def get_pending_comments(_: None = Depends(verify_admin)):
    try:
        cursor = db.comments.find({"approved": False}).sort("created_at", 1)
        comments = []
        async for doc in cursor:
            doc["_id"] = str(doc["_id"])
            comments.append(doc)
        return comments
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# Yorumu onaylama (sadece ana kullanici)
@app.post("/api/comments/{comment_id}/approve")
async def approve_comment(comment_id: str, _: None = Depends(verify_admin)):
    try:
        oid = parse_object_id(comment_id, "yorum id")
        result = await db.comments.update_one({"_id": oid}, {"$set": {"approved": True}})
        if result.matched_count == 0:
            raise HTTPException(status_code=404, detail="Yorum bulunamadi")
        return {"status": "success", "mesaj": "Yorum onaylandi"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# Yorumu reddetme / silme (sadece ana kullanici)
@app.delete("/api/comments/{comment_id}")
async def delete_comment(comment_id: str, _: None = Depends(verify_admin)):
    try:
        oid = parse_object_id(comment_id, "yorum id")
        result = await db.comments.delete_one({"_id": oid})
        if result.deleted_count == 0:
            raise HTTPException(status_code=404, detail="Yorum bulunamadi")
        return {"status": "success", "mesaj": "Yorum silindi"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)