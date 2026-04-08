import os
import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from motor.motor_asyncio import AsyncIOMotorClient
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime
from slugify import slugify

app = FastAPI(title="Kalemsel v2 API - Kesin Cozum")

# SENIN BILGILERINLE MONGO BAGLANTISI
# <db_username> -> kalemsel2020
# <db_password> -> rMJM1sboQMlubA23
MONGO_URL = "mongodb+srv://kalemsel2020:rMJM1sboQMlubA23@kalemsel-db.9junytc.mongodb.net/?retryWrites=true&w=majority&appName=kalemsel-db"

try:
    client = AsyncIOMotorClient(MONGO_URL)
    db = client.kalemsel_v2_database
    print("MongoDB'ye basariyla baglanildi!")
except Exception as e:
    print(f"Baglanti hatasi: {e}")

# CORS - Sitenin her yerden erisilebilir olmasi icin
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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

# Yeni icerik ekleme
@app.post("/api/posts")
async def create_post(post: Post):
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