# Zikirmatik

Gece safiri, kristal ve altın tonlarında, sade ama özenli bir Android zikirmatiği (Kotlin + Jetpack Compose).

## Özellikler
- 6 hazır zikir + kendi zikirlerini ekleme, düzenleme, silme
- Her zikre ayrı hedef (33 / 99 / 100 ya da istediğin sayı)
- Tespih taneleriyle dolan altın halka, dokunuşta dalga ve yaylanma animasyonu
- Hedef seçimi: 33 / 99 / 100 / 1000, tur sayacı
- Her dokunuşta hafif titreşim, hedefe ulaşınca belirgin titreşim (kapatılabilir)
- **Ses tuşlarıyla sayma**: ekrana bakmadan zikir çekebilirsin
- Ekran kapanmaz, sayaçlar her zikir için ayrı kaydedilir, toplam sayaç tutulur

## APK'yı alma

### Yol 1: Bilgisayara hiçbir şey kurmadan (GitHub)
1. GitHub'da yeni bir depo aç ve bu klasörün tüm içeriğini yükle (`.github` klasörü dahil).
2. **Actions** sekmesinde "APK derle" işinin bitmesini bekle (~5 dk).
3. İşe tıkla, en alttaki **Zikirmatik-APK** dosyasını indir, zip'ten çıkar.
4. APK'yı telefona at, aç ve kur ("bilinmeyen kaynaklar" izni isteyebilir).

### Yol 2: Android Studio
1. Android Studio ile bu klasörü aç (File → Open).
2. Gradle senkronizasyonu bitince telefonu USB ile bağla ve ▶ Run'a bas.
