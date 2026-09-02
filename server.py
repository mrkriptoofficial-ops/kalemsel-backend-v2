import os
import secrets
import uvicorn
from fastapi import FastAPI, HTTPException, Header, Depends
from fastapi.middleware.cors import CORSMiddleware
from motor.motor_asyncio import AsyncIOMotorClient
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime
from slugify import slugify

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

# --- ROTALAR (ENDPOINTS) ---

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

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)