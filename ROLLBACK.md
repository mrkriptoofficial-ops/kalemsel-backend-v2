# Bir Hata Olursa Eski Hale Dönme Rehberi

Bu dosya, bir deploy sonrası sitede bir sorun fark ederseniz **hızlıca eski
(çalışır) haline dönmenizi** sağlamak için yazıldı.

## 1. Önce kontrol edin: sorun gerçekten yeni koddan mı kaynaklanıyor?

Siteye şu adresten bakın: `https://<siteniz>/`

```json
{"status": "Online", "database": "Bagli", "mesaj": "..."}
```

- `"database": "Kopuk"` görüyorsanız → sorun genelde `MONGO_URL` environment
  variable'ı (yanlış/eksik/şifre değişmiş) veya MongoDB Atlas tarafında bir
  ayar (IP whitelist vb.). Kodu geri almadan önce önce bunu kontrol edin.
- `"database": "Bagli"` ama başka bir hata alıyorsanız → koddaki yeni bir
  değişiklik sorunlu olabilir, aşağıdaki adımlarla geri dönün.

## 2. Kod tarafında eski hale dönme (Git)

Her önemli değişiklikten sonra bir **git tag** (kontrol noktası) bırakıyoruz.
Mevcut kontrol noktalarını görmek için:

```bash
git tag -l
```

Belirli bir kontrol noktasına dönmek için (örnek):

```bash
git checkout <tag-adi>
```

Ya da GitHub üzerinden hiç terminal kullanmadan:
1. Repo sayfasında "main" yazan yerden tag'i seçin (örn. `kontrol-noktasi-2`).
2. O anki kod halini görürsünüz, isterseniz oradan yeni bir branch açıp
   deploy edebilirsiniz.

**Önemli:** `main` branch'i şu ana kadar hiç değiştirilmedi, hâlâ en
başındaki orijinal kodu taşıyor. Yani `main` kendi başına zaten güvenilir
bir "eski hal" - eğer barındırma platformunuz `main`'den deploy ediyorsa ve
oraya henüz hiçbir değişiklik alınmadıysa, şu an sitede zaten orijinal kod
çalışıyor demektir.

## 3. Barındırma platformunda (Render/Railway vb.) eski deploy'a dönme

Çoğu platform, geçmiş deploy'ları saklar ve tek tıkla eskisine dönebilirsiniz:

- **Render:** Servis sayfası → "Events" veya "Deploys" sekmesi → eski bir
  deploy'un yanındaki "Rollback" / "Redeploy" butonu.
- **Railway:** Proje → "Deployments" sekmesi → eski bir deployment'a tıklayıp
  "Redeploy".

Bu, kodu git'te geri almadan da anlık olarak eski çalışan sürüme dönmenizi
sağlar - bir sorun anında en hızlı çözüm genelde budur.

## 4. Veritabanı (içerik) tarafında eski hale dönme

Kod geri alınsa bile, eğer bir hata **veri kaybına veya bozulmasına** yol
açtıysa (örneğin yanlışlıkla post silindiyse), kod geri almak veriyi geri
getirmez. Bunun için:

- **MongoDB Atlas'ta otomatik yedekleme (backup) özelliğini açık tutun.**
  Atlas panelinde: Cluster → "Backup" sekmesi → "Continuous Backup" veya
  "Snapshot" ayarlarını kontrol edin. Bu, Claude'un ya da benim
  yapabileceğim bir ayar değil - MongoDB Atlas hesabınızdan siz açmalısınız.
- Yedekleme açıksa, Atlas panelinden belirli bir zamana ("point-in-time")
  geri dönebilir veya bir snapshot'ı geri yükleyebilirsiniz.

## 5. Ortam değişkenlerini (environment variables) unutmayın

Eski bir koda dönseniz bile, barındırma platformunda şu iki değişkenin doğru
tanımlı olduğundan emin olun (yoksa uygulama hiç başlamaz):

- `MONGO_URL`
- `ADMIN_PASSWORD`

## Özet - Hızlı Karar Ağacı

```
Sorun var mı?
├─ database: "Kopuk" -> MONGO_URL / Atlas ayarlarini kontrol et
├─ Kod hatasi (500, beklenmeyen davranis) -> Platformda "Rollback/Redeploy"
│                                            ile bir onceki deploy'a don
├─ Veri kayboldu/bozuldu -> Atlas backup'tan geri yukle (kod rollback yetmez)
└─ Emin degilsen -> Claude'a "eski hale dondur" de, ilgili git tag'ine
                     donup tekrar test ederiz
```
