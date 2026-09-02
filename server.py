import os
import secrets
import hashlib
import logging
import uvicorn
from enum import Enum
from fastapi import FastAPI, HTTPException, Header, Depends, Request, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorGridFSBucket
from pydantic import BaseModel, EmailStr
from typing import Optional
from datetime import datetime
from slugify import slugify
from bson import ObjectId
from bson.errors import InvalidId
from gridfs.errors import NoFile
from pymongo.errors import DuplicateKeyError
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded

# --- LOGLAMA ---
# Hata detaylari istemciye degil, sunucu loglarina yazilir (bilgi sizintisini onlemek icin).
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("kalemsel")

app = FastAPI(title="Kalemsel v2 API - Kesin Cozum")

# MONGO BAGLANTISI
# Baglanti adresi artik kod icinde degil, MONGO_URL environment variable'indan okunuyor.
# Ornek: export MONGO_URL="mongodb+srv://<kullanici>:<sifre>@<cluster-adresi>/?retryWrites=true&w=majority"
MONGO_URL = os.environ.get("MONGO_URL")
if not MONGO_URL:
    raise RuntimeError("MONGO_URL environment variable tanimli degil")

client = AsyncIOMotorClient(MONGO_URL)
db = client.kalemsel_v2_database

# --- MEDYA (GORSEL/VIDEO) DEPOLAMA ---
# Dosyalar MongoDB icinde GridFS ile saklanir, boylece ek bir bulut depolama
# servisi/hesabi gerekmez. Buyuk video dosyalarinin MongoDB Atlas depolama
# kotanizi hizla tuketebilecegini unutmayin (uzerinde COK sayida/uzun video
# birikirse ileride S3/Cloudinary gibi bir servise gecmek gerekebilir).
fs_bucket = AsyncIOMotorGridFSBucket(db, bucket_name="media")
ALLOWED_MEDIA_TYPES = {
    "image/jpeg", "image/png", "image/gif", "image/webp",
    "video/mp4", "video/webm", "video/quicktime",
}
MAX_UPLOAD_SIZE = 50 * 1024 * 1024  # 50 MB

# CORS - Sitenin her yerden erisilebilir olmasi icin
# Not: Kimlik dogrulama cookie ile degil Authorization header'i ile yapiliyor,
# bu yuzden allow_credentials'a gerek yok - "*" ile birlikte kullanimi zaten
# tarayicilar tarafindan spesifikasyona aykiri sayilip reddedilebiliyordu.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- YETKILENDIRME (SADECE ANA KULLANICI PAYLASIM YAPABILIR) ---
# Sunucu ortaminda ADMIN_PASSWORD environment variable'i tanimlanmali.
# Ornek: export ADMIN_PASSWORD="cok-guclu-bir-sifre"
ADMIN_PASSWORD = os.environ.get("ADMIN_PASSWORD")

async def verify_admin(authorization: Optional[str] = Header(None)):
    """
    Paylasim (post olusturma/duzenleme/silme) islemlerini sadece ana kullaniciyla
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

# --- UYE (KAYITLI KULLANICI) SISTEMI ---
# Yorum yapabilmek icin ya kayitli bir uye girisi ya da bir e-posta adresi
# gerekiyor. Uye sifreleri PBKDF2 ile (ek bir kutuphane gerektirmeden,
# stdlib'deki hashlib ile) hashlenir; giris sonrasi verilen token da DB'de
# ham haliyle degil, hash'i tutularak saklanir.
PBKDF2_ITERATIONS = 260_000

def hash_password(password: str) -> str:
    salt = secrets.token_hex(16)
    dk = hashlib.pbkdf2_hmac("sha256", password.encode(), bytes.fromhex(salt), PBKDF2_ITERATIONS)
    return f"{salt}${dk.hex()}"

def verify_password(password: str, stored: str) -> bool:
    try:
        salt, hash_hex = stored.split("$")
    except ValueError:
        return False
    dk = hashlib.pbkdf2_hmac("sha256", password.encode(), bytes.fromhex(salt), PBKDF2_ITERATIONS)
    return secrets.compare_digest(dk.hex(), hash_hex)

def hash_token(token: str) -> str:
    return hashlib.sha256(token.encode()).hexdigest()

async def get_current_member(authorization: Optional[str] = Header(None)) -> Optional[dict]:
    """
    Uye girisi ISTEGE BAGLI olan uc noktalarda kullanilir (ornegin yorum
    ekleme): gecerli bir uye token'i varsa uyeyi dondurur, yoksa None -
    hata firlatmaz (cunku anonim + e-posta ile de yorum yapilabiliyor).
    """
    if not authorization or not authorization.startswith("Bearer "):
        return None
    token = authorization.removeprefix("Bearer ").strip()
    return await db.members.find_one({"token_hash": hash_token(token)})

def raise_server_error(e: Exception):
    """
    Ham hata mesajini (str(e)) dogrudan istemciye gostermek yerine sunucu
    loglarina yazip istemciye genel bir mesaj donuyor (bilgi sizintisini onlemek icin).
    """
    logger.exception("Sunucu hatasi")
    raise HTTPException(status_code=500, detail="Sunucuda beklenmeyen bir hata olustu")

# --- VERI YAPISI ---
class ContentType(str, Enum):
    siir = "siir"
    makale = "makale"
    deneme = "deneme"
    hikaye = "hikaye"

class Post(BaseModel):
    title: str
    content: str
    content_type: ContentType
    category: str = "Genel"
    author: str = "Yönetici"
    cover_image: Optional[str] = None  # /api/media/<id> (yuklenen dosya) veya harici bir URL
    video_url: Optional[str] = None  # /api/media/<id> (yuklenen dosya) veya harici bir URL (ornegin YouTube)

class PostUpdate(BaseModel):
    title: Optional[str] = None
    content: Optional[str] = None
    content_type: Optional[ContentType] = None
    category: Optional[str] = None
    author: Optional[str] = None
    cover_image: Optional[str] = None
    video_url: Optional[str] = None

class CommentCreate(BaseModel):
    # author/email: uye girisi yoksa ikisi de zorunlu; uye girisi varsa ikisi
    # de otomatik uye profilinden alinir (author verilirse gorunen ismi gecer).
    author: Optional[str] = None
    content: str
    email: Optional[EmailStr] = None

class MemberRegister(BaseModel):
    name: str
    email: EmailStr
    password: str

class MemberLogin(BaseModel):
    email: EmailStr
    password: str

class AuthorApplicationCreate(BaseModel):
    full_name: str  # Ad Soyad
    phone: str  # Telefon
    sample_text: str  # Ornek metin (siir/oyku vb.)

class AuthorApplicationStatusUpdate(BaseModel):
    status: str  # "beklemede" | "kabul edildi" | "reddedildi"

class HomepageSectionCreate(BaseModel):
    title: str  # Serbest metin: "Günün Şiiri", "Günün Öyküsü" veya admin ne isterse
    content: Optional[str] = None  # Serbest metin icerik
    post_id: Optional[str] = None  # Istege bagli: mevcut bir yaziya referans
    image_url: Optional[str] = None  # /api/media/<id> veya harici bir URL
    video_url: Optional[str] = None  # /api/media/<id> veya harici bir URL
    order: int = 0  # Ana sayfada gorunme sirasi (kucuk sayi once gelir)

class HomepageSectionUpdate(BaseModel):
    title: Optional[str] = None
    content: Optional[str] = None
    post_id: Optional[str] = None
    image_url: Optional[str] = None
    video_url: Optional[str] = None
    order: Optional[int] = None

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

def stringify_id(doc: dict) -> dict:
    doc["_id"] = str(doc["_id"])
    return doc

async def enrich_homepage_section(doc: dict) -> dict:
    """
    Bir ana sayfa bolumune post_id verilmisse, o yazinin tam halini de
    yanit icine ekler; boylece frontend ayrica ikinci bir istek atmak
    zorunda kalmaz.
    """
    doc = stringify_id(doc)
    doc["post"] = None
    if doc.get("post_id"):
        try:
            post = await db.posts.find_one({"_id": ObjectId(doc["post_id"])})
        except InvalidId:
            post = None
        if post:
            doc["post"] = stringify_id(post)
    return doc

# --- SPAM/RATE-LIMIT KORUMASI ---
# Yorum ve begeni gibi kimlik dogrulama gerektirmeyen uc noktalarin
# ayni IP tarafindan asiri sikilikta cagrilmasini engeller.
# key_style="endpoint": limit sayaci endpoint fonksiyonuna gore tutulur, URL
# icindeki post_id gibi degisken degerlere gore degil (yoksa her farkli post
# icin ayri bir sayac olusur ve limit pratikte hicbir zaman dolmaz).
limiter = Limiter(key_func=get_client_ip, key_style="endpoint")
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# --- ROTALAR (ENDPOINTS) ---

@app.on_event("startup")
async def create_indexes():
    # Ayni IP'nin ayni postu iki kez begenmesini veritabani seviyesinde de engelle
    # (esiz istek yarisi/race condition durumlarina karsi ek guvence).
    try:
        await db.likes.create_index([("post_id", 1), ("ip", 1)], unique=True)
    except Exception as e:
        logger.warning(f"Index olusturma hatasi: {e}")
    # Ayni e-posta ile iki kez uye kaydi olusturulmasin
    try:
        await db.members.create_index("email", unique=True)
    except Exception as e:
        logger.warning(f"Index olusturma hatasi: {e}")

@app.get("/")
async def root():
    """
    Saglik kontrolu. Onceden 'database': 'Bagli' her zaman sabit donuyordu -
    gercekte baglanti kopuk olsa da bu deger degismiyordu. Artik gercek bir
    ping atiliyor: bir deploy'dan sonra buraya bakip veritabaninin gercekten
    calisir durumda olup olmadigini anlik olarak gorebilirsiniz. 'Kopuk'
    donuyorsa hemen eski (calisir) versiyona geri donmeniz gerekir.
    """
    db_status = "Bagli"
    try:
        await db.command("ping")
    except Exception as e:
        db_status = "Kopuk"
        logger.error(f"Saglik kontrolu - veritabani baglantisi basarisiz: {e}")
    return {"status": "Online", "database": db_status, "mesaj": "Kalemsel v2 Basariyla Calisiyor"}

# Yeni icerik ekleme (sadece ana kullanici / admin)
@app.post("/api/posts")
async def create_post(post: Post, _: None = Depends(verify_admin)):
    try:
        post_dict = post.dict()
        post_dict["content_type"] = post.content_type.value
        post_dict["slug"] = slugify(post.title)
        post_dict["created_at"] = datetime.now()
        post_dict["likes"] = 0
        post_dict["views"] = 0
        result = await db.posts.insert_one(post_dict)
        return {"status": "success", "id": str(result.inserted_id), "slug": post_dict["slug"]}
    except Exception as e:
        raise_server_error(e)

# Tum icerikleri listeleme (sayfalama + kategori/tur filtreleme destekli)
@app.get("/api/posts")
async def get_posts(
    skip: int = 0,
    limit: int = 20,
    category: Optional[str] = None,
    content_type: Optional[ContentType] = None,
):
    try:
        skip = max(0, skip)
        limit = max(1, min(limit, 100))
        query = {}
        if category:
            query["category"] = category
        if content_type:
            query["content_type"] = content_type.value
        cursor = db.posts.find(query).sort("created_at", -1).skip(skip).limit(limit)
        posts = [stringify_id(doc) async for doc in cursor]
        return posts
    except Exception as e:
        raise_server_error(e)

# Tek bir yaziyi id ile getirme (goruntulenme sayacini artirir)
@app.get("/api/posts/{post_id}")
async def get_post(post_id: str):
    try:
        oid = parse_object_id(post_id, "post id")
        post = await db.posts.find_one({"_id": oid})
        if not post:
            raise HTTPException(status_code=404, detail="Post bulunamadi")
        await db.posts.update_one({"_id": oid}, {"$inc": {"views": 1}})
        post["views"] = post.get("views", 0) + 1
        return stringify_id(post)
    except HTTPException:
        raise
    except Exception as e:
        raise_server_error(e)

# Tek bir yaziyi slug ile getirme (goruntulenme sayacini artirir)
@app.get("/api/posts/slug/{slug}")
async def get_post_by_slug(slug: str):
    try:
        post = await db.posts.find_one({"slug": slug})
        if not post:
            raise HTTPException(status_code=404, detail="Post bulunamadi")
        await db.posts.update_one({"_id": post["_id"]}, {"$inc": {"views": 1}})
        post["views"] = post.get("views", 0) + 1
        return stringify_id(post)
    except HTTPException:
        raise
    except Exception as e:
        raise_server_error(e)

# Yaziyi guncelleme (sadece ana kullanici / admin)
@app.put("/api/posts/{post_id}")
async def update_post(post_id: str, post_update: PostUpdate, _: None = Depends(verify_admin)):
    try:
        oid = parse_object_id(post_id, "post id")
        existing = await db.posts.find_one({"_id": oid})
        if not existing:
            raise HTTPException(status_code=404, detail="Post bulunamadi")

        update_data = {k: v for k, v in post_update.dict(exclude_unset=True).items() if v is not None}
        if not update_data:
            raise HTTPException(status_code=400, detail="Guncellenecek herhangi bir alan gonderilmedi")
        if isinstance(update_data.get("content_type"), ContentType):
            update_data["content_type"] = update_data["content_type"].value
        if "title" in update_data:
            update_data["slug"] = slugify(update_data["title"])

        await db.posts.update_one({"_id": oid}, {"$set": update_data})
        updated = await db.posts.find_one({"_id": oid})
        return stringify_id(updated)
    except HTTPException:
        raise
    except Exception as e:
        raise_server_error(e)

# Yaziyi silme (sadece ana kullanici / admin)
@app.delete("/api/posts/{post_id}")
async def delete_post(post_id: str, _: None = Depends(verify_admin)):
    try:
        oid = parse_object_id(post_id, "post id")
        result = await db.posts.delete_one({"_id": oid})
        if result.deleted_count == 0:
            raise HTTPException(status_code=404, detail="Post bulunamadi")
        # Yaziya bagli yorum ve begenileri de temizle
        await db.comments.delete_many({"post_id": post_id})
        await db.likes.delete_many({"post_id": post_id})
        return {"status": "success", "mesaj": "Post silindi"}
    except HTTPException:
        raise
    except Exception as e:
        raise_server_error(e)

# --- MEDYA (GORSEL/VIDEO) ---

# Dosya yukleme (sadece ana kullanici / admin). Basarili olursa donen "url"
# degeri dogrudan Post/HomepageSection'daki cover_image/video_url/image_url
# alanlarina yazilabilir.
@app.post("/api/media/upload")
async def upload_media(file: UploadFile = File(...), _: None = Depends(verify_admin)):
    try:
        if file.content_type not in ALLOWED_MEDIA_TYPES:
            raise HTTPException(status_code=400, detail=f"Desteklenmeyen dosya turu: {file.content_type}")

        upload_stream = fs_bucket.open_upload_stream(
            file.filename or "dosya",
            metadata={"content_type": file.content_type, "uploaded_at": datetime.now()},
        )
        total_size = 0
        try:
            while True:
                chunk = await file.read(1024 * 1024)  # 1 MB'lik parcalar halinde oku
                if not chunk:
                    break
                total_size += len(chunk)
                if total_size > MAX_UPLOAD_SIZE:
                    raise HTTPException(
                        status_code=413,
                        detail=f"Dosya cok buyuk (maksimum {MAX_UPLOAD_SIZE // (1024 * 1024)} MB)",
                    )
                await upload_stream.write(chunk)
        except HTTPException:
            await upload_stream.abort()
            raise
        await upload_stream.close()

        file_id = str(upload_stream._id)
        return {
            "status": "success",
            "id": file_id,
            "url": f"/api/media/{file_id}",
            "content_type": file.content_type,
            "filename": file.filename,
        }
    except HTTPException:
        raise
    except Exception as e:
        raise_server_error(e)

# Yuklenmis bir dosyayi getirme/oynatma (herkese acik - <img>/<video> src'si olarak kullanilir)
@app.get("/api/media/{file_id}")
async def get_media(file_id: str):
    oid = parse_object_id(file_id, "media id")
    try:
        grid_out = await fs_bucket.open_download_stream(oid)
    except NoFile:
        raise HTTPException(status_code=404, detail="Dosya bulunamadi")
    except Exception as e:
        raise_server_error(e)

    async def stream():
        while True:
            chunk = await grid_out.readchunk()
            if not chunk:
                break
            yield chunk

    content_type = (grid_out.metadata or {}).get("content_type", "application/octet-stream")
    return StreamingResponse(stream(), media_type=content_type)

# Yuklenmis bir dosyayi silme (sadece ana kullanici / admin)
@app.delete("/api/media/{file_id}")
async def delete_media(file_id: str, _: None = Depends(verify_admin)):
    oid = parse_object_id(file_id, "media id")
    try:
        await fs_bucket.delete(oid)
    except NoFile:
        raise HTTPException(status_code=404, detail="Dosya bulunamadi")
    except Exception as e:
        raise_server_error(e)
    return {"status": "success", "mesaj": "Dosya silindi"}

# --- UYELIK (KAYIT / GIRIS) ---
# Yorum yapabilmek icin "kayitli uye" olma yolu bu uc noktalardan gecer.
# Alternatifi (uye olmadan sadece e-posta ile yorum yapma) yorum uc
# noktasinda ele alinir.

@app.post("/api/members/register")
@limiter.limit("5/minute")
async def register_member(request: Request, member: MemberRegister):
    try:
        if len(member.password) < 6:
            raise HTTPException(status_code=400, detail="Sifre en az 6 karakter olmali")
        member_dict = {
            "name": member.name,
            "email": member.email,
            "password_hash": hash_password(member.password),
            "created_at": datetime.now(),
        }
        try:
            result = await db.members.insert_one(member_dict)
        except DuplicateKeyError:
            raise HTTPException(status_code=409, detail="Bu e-posta adresiyle zaten bir uye var")
        return {"status": "success", "id": str(result.inserted_id), "mesaj": "Kayit basarili, simdi giris yapabilirsiniz"}
    except HTTPException:
        raise
    except Exception as e:
        raise_server_error(e)

@app.post("/api/members/login")
@limiter.limit("10/minute")
async def login_member(request: Request, credentials: MemberLogin):
    try:
        member = await db.members.find_one({"email": credentials.email})
        if not member or not verify_password(credentials.password, member["password_hash"]):
            raise HTTPException(status_code=401, detail="E-posta veya sifre hatali")
        token = secrets.token_urlsafe(32)
        await db.members.update_one({"_id": member["_id"]}, {"$set": {"token_hash": hash_token(token)}})
        return {
            "status": "success",
            "token": token,
            "mesaj": "Giris basarili. Bu token'i istekte 'Authorization: Bearer <token>' olarak gonderin",
        }
    except HTTPException:
        raise
    except Exception as e:
        raise_server_error(e)

# --- YAZAR/SAIR BASVURUSU ("Yazar Ol") ---
# Herkes basvurabilir; bilgiler sadece admin'in gorebilecegi bir listeye
# duser, admin degerlendirip durumunu gunceller.

@app.post("/api/author-applications")
@limiter.limit("3/minute")
async def create_author_application(request: Request, application: AuthorApplicationCreate):
    try:
        app_dict = application.dict()
        app_dict["status"] = "beklemede"
        app_dict["created_at"] = datetime.now()
        result = await db.author_applications.insert_one(app_dict)
        return {"status": "success", "id": str(result.inserted_id), "mesaj": "Basvurunuz alindi, degerlendirilecek"}
    except Exception as e:
        raise_server_error(e)

# Basvurulari listeleme (sadece ana kullanici / admin)
@app.get("/api/author-applications")
async def get_author_applications(_: None = Depends(verify_admin)):
    try:
        cursor = db.author_applications.find().sort("created_at", -1)
        applications = [stringify_id(doc) async for doc in cursor]
        return applications
    except Exception as e:
        raise_server_error(e)

# Basvuru durumunu guncelleme (sadece ana kullanici / admin)
@app.put("/api/author-applications/{application_id}")
async def update_author_application(application_id: str, update: AuthorApplicationStatusUpdate, _: None = Depends(verify_admin)):
    try:
        if update.status not in {"beklemede", "kabul edildi", "reddedildi"}:
            raise HTTPException(status_code=400, detail="Gecersiz durum degeri")
        oid = parse_object_id(application_id, "basvuru id")
        result = await db.author_applications.update_one({"_id": oid}, {"$set": {"status": update.status}})
        if result.matched_count == 0:
            raise HTTPException(status_code=404, detail="Basvuru bulunamadi")
        updated = await db.author_applications.find_one({"_id": oid})
        return stringify_id(updated)
    except HTTPException:
        raise
    except Exception as e:
        raise_server_error(e)

# Basvuruyu silme (sadece ana kullanici / admin)
@app.delete("/api/author-applications/{application_id}")
async def delete_author_application(application_id: str, _: None = Depends(verify_admin)):
    try:
        oid = parse_object_id(application_id, "basvuru id")
        result = await db.author_applications.delete_one({"_id": oid})
        if result.deleted_count == 0:
            raise HTTPException(status_code=404, detail="Basvuru bulunamadi")
        return {"status": "success", "mesaj": "Basvuru silindi"}
    except HTTPException:
        raise
    except Exception as e:
        raise_server_error(e)

# --- ANA SAYFA (dinamik bolumler: Gunun Siiri, Gunun Oykusu vb.) ---

# Ana sayfaya yeni bir bolum ekleme (sadece ana kullanici / admin)
@app.post("/api/homepage")
async def create_homepage_section(section: HomepageSectionCreate, _: None = Depends(verify_admin)):
    try:
        if not any([section.content, section.post_id, section.image_url, section.video_url]):
            raise HTTPException(
                status_code=400,
                detail="Icerik metni, bir yazi referansi (post_id), gorsel veya video URL'sinden en az biri girilmeli",
            )
        if section.post_id:
            oid = parse_object_id(section.post_id, "post_id")
            if not await db.posts.find_one({"_id": oid}):
                raise HTTPException(status_code=404, detail="Referans verilen yazi bulunamadi")

        section_dict = section.dict()
        section_dict["created_at"] = datetime.now()
        result = await db.homepage_sections.insert_one(section_dict)
        return {"status": "success", "id": str(result.inserted_id)}
    except HTTPException:
        raise
    except Exception as e:
        raise_server_error(e)

# Ana sayfa icerigini listeleme (herkese acik)
@app.get("/api/homepage")
async def get_homepage_sections():
    try:
        cursor = db.homepage_sections.find().sort([("order", 1), ("created_at", 1)])
        sections = [await enrich_homepage_section(doc) async for doc in cursor]
        return sections
    except Exception as e:
        raise_server_error(e)

# Ana sayfa bolumunu guncelleme (sadece ana kullanici / admin)
@app.put("/api/homepage/{section_id}")
async def update_homepage_section(section_id: str, section_update: HomepageSectionUpdate, _: None = Depends(verify_admin)):
    try:
        oid = parse_object_id(section_id, "section id")
        existing = await db.homepage_sections.find_one({"_id": oid})
        if not existing:
            raise HTTPException(status_code=404, detail="Bolum bulunamadi")

        update_data = {k: v for k, v in section_update.dict(exclude_unset=True).items() if v is not None}
        if not update_data:
            raise HTTPException(status_code=400, detail="Guncellenecek herhangi bir alan gonderilmedi")
        if "post_id" in update_data:
            post_oid = parse_object_id(update_data["post_id"], "post_id")
            if not await db.posts.find_one({"_id": post_oid}):
                raise HTTPException(status_code=404, detail="Referans verilen yazi bulunamadi")

        await db.homepage_sections.update_one({"_id": oid}, {"$set": update_data})
        updated = await db.homepage_sections.find_one({"_id": oid})
        return await enrich_homepage_section(updated)
    except HTTPException:
        raise
    except Exception as e:
        raise_server_error(e)

# Ana sayfa bolumunu silme (sadece ana kullanici / admin)
@app.delete("/api/homepage/{section_id}")
async def delete_homepage_section(section_id: str, _: None = Depends(verify_admin)):
    try:
        oid = parse_object_id(section_id, "section id")
        result = await db.homepage_sections.delete_one({"_id": oid})
        if result.deleted_count == 0:
            raise HTTPException(status_code=404, detail="Bolum bulunamadi")
        return {"status": "success", "mesaj": "Bolum silindi"}
    except HTTPException:
        raise
    except Exception as e:
        raise_server_error(e)

# --- BEGENI ---

# Bir posta begeni ekleme (herkes yapabilir, onay gerekmez, ayni IP ayni postu tekrar begenemez)
@app.post("/api/posts/{post_id}/like")
@limiter.limit("10/minute")
async def like_post(request: Request, post_id: str):
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
        raise_server_error(e)

# --- YORUMLAR ---

# Yorum ekleme: sadece bir isim (author) yeterli, herkes yapabilir.
# Uye girisi yapilmissa (Authorization: Bearer <uye-token>) isim/e-posta
# otomatik profilden alinir, ama bu zorunlu degil. Yayinlanmadan once
# admin onayi gerekir - asil koruma budur.
@app.post("/api/posts/{post_id}/comments")
@limiter.limit("5/minute")
async def create_comment(
    request: Request,
    post_id: str,
    comment: CommentCreate,
    member: Optional[dict] = Depends(get_current_member),
):
    try:
        oid = parse_object_id(post_id, "post id")
        post = await db.posts.find_one({"_id": oid})
        if not post:
            raise HTTPException(status_code=404, detail="Post bulunamadi")

        if member:
            author_name = comment.author or member["name"]
            email = member["email"]
            member_id = str(member["_id"])
        else:
            # Uye girisi sart degil - sadece bir isim yeterli. E-posta
            # verilirse (istege bagli) admin gorebilsin diye saklanir.
            if not comment.author:
                raise HTTPException(status_code=400, detail="Isim (author) girilmeli")
            author_name = comment.author
            email = comment.email
            member_id = None

        comment_dict = {
            "author": author_name,
            "content": comment.content,
            "email": email,
            "member_id": member_id,
            "post_id": post_id,
            "approved": False,
            "created_at": datetime.now(),
        }
        result = await db.comments.insert_one(comment_dict)
        return {"status": "success", "id": str(result.inserted_id), "mesaj": "Yorumunuz onay bekliyor"}
    except HTTPException:
        raise
    except Exception as e:
        raise_server_error(e)

# Bir postun onaylanmis yorumlarini listeleme (herkese acik)
# Not: e-posta ve member_id gizlilik icin bu genel listede gosterilmez,
# sadece admin'in gordugu /api/comments/pending icinde yer alir.
@app.get("/api/posts/{post_id}/comments")
async def get_comments(post_id: str):
    try:
        cursor = db.comments.find({"post_id": post_id, "approved": True}).sort("created_at", 1)
        comments = []
        async for doc in cursor:
            doc.pop("email", None)
            doc.pop("member_id", None)
            comments.append(stringify_id(doc))
        return comments
    except Exception as e:
        raise_server_error(e)

# Onay bekleyen yorumlari listeleme (sadece ana kullanici)
@app.get("/api/comments/pending")
async def get_pending_comments(_: None = Depends(verify_admin)):
    try:
        cursor = db.comments.find({"approved": False}).sort("created_at", 1)
        comments = [stringify_id(doc) async for doc in cursor]
        return comments
    except Exception as e:
        raise_server_error(e)

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
        raise_server_error(e)

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
        raise_server_error(e)

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)
