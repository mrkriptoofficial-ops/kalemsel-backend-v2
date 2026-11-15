package com.gezgin.zikirmatik

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

// ───────────── Veri modelleri ─────────────
data class Zikir(
    val id: Long,
    val arapca: String,
    val okunus: String,
    val anlam: String,
    val fazilet: String = "",
    val count: Int = 0,
    val target: Int = 33,
)

data class Step(val arapca: String, val okunus: String, val anlam: String, val target: Int)

data class Vird(val id: Long, val name: String, val steps: List<Step>) {
    val total: Int get() = steps.sumOf { it.target }
}

data class Session(val virdId: Long, val step: Int, val count: Int)

data class Settings(
    val theme: Int = 0,            // 0 Safir, 1 Gece (OLED siyah), 2 Fildişi
    val haptic: Int = 2,           // 0 kapalı, 1 hafif, 2 normal, 3 güçlü
    val sound: Boolean = false,
    val volumeKeys: Boolean = true,
    val stopAtTarget: Boolean = false,
    val guardMs: Int = 70,         // çift dokunma koruması
    val fontScale: Float = 1f,
    val milestone: Int = 33,       // ara titreşim aralığı (0 = kapalı)
    val dailyGoal: Int = 0,
    val remMorning: Boolean = false,
    val morningMin: Int = 7 * 60,
    val remEvening: Boolean = false,
    val eveningMin: Int = 18 * 60 + 30,
)

data class LibItem(
    val arapca: String,
    val okunus: String,
    val anlam: String,
    val fazilet: String = "",
    val target: Int = 33,
    val zikirOkunus: String = okunus,
)

// ───────────── Hazır içerik ─────────────
val VARSAYILAN = listOf(
    Zikir(1, "سُبْحَانَ اللّٰهِ", "Sübhânallah", "Allah her türlü noksanlıktan münezzehtir", "Namaz sonrası tesbihatta 33 kez okunur."),
    Zikir(2, "اَلْحَمْدُ لِلّٰهِ", "Elhamdülillâh", "Hamd yalnızca Allah'adır", "Namaz sonrası tesbihatta 33 kez okunur."),
    Zikir(3, "اَللّٰهُ اَكْبَرُ", "Allâhu Ekber", "Allah en büyüktür", "Namaz sonrası tesbihatta 33 kez okunur."),
    Zikir(4, "لَا اِلٰهَ اِلَّا اللّٰهُ", "Lâ ilâhe illallah", "Allah'tan başka ilah yoktur", "", target = 100),
    Zikir(5, "اَسْتَغْفِرُ اللّٰهَ", "Estağfirullah", "Allah'tan bağışlanma dilerim", "", target = 100),
    Zikir(6, "اَللّٰهُمَّ صَلِّ عَلٰى سَيِّدِنَا مُحَمَّدٍ", "Salavât", "Allah'ım, Efendimiz Muhammed'e salât eyle", "", target = 100),
)

private const val TEVHID_AR = "لَا اِلٰهَ اِلَّا اللّٰهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلٰى كُلِّ شَيْءٍ قَدِيرٌ"
private const val TEVHID_TR = "Lâ ilâhe illallâhu vahdehû lâ şerîke leh, lehü'l-mülkü ve lehü'l-hamdü ve hüve alâ külli şey'in kadîr"

val VARSAYILAN_VIRDLER = listOf(
    Vird(
        101, "Namaz Sonrası Tesbihat",
        listOf(
            Step("سُبْحَانَ اللّٰهِ", "Sübhânallah", "Allah her türlü noksanlıktan münezzehtir", 33),
            Step("اَلْحَمْدُ لِلّٰهِ", "Elhamdülillâh", "Hamd yalnızca Allah'adır", 33),
            Step("اَللّٰهُ اَكْبَرُ", "Allâhu Ekber", "Allah en büyüktür", 33),
            Step(TEVHID_AR, "Kelime-i Tevhîd", "Allah'tan başka ilah yoktur; tektir, ortağı yoktur. Mülk ve hamd O'nundur, O her şeye kadirdir.", 1),
        ),
    ),
    Vird(
        102, "Sabah - Akşam Virdi",
        listOf(
            Step("سُبْحَانَ اللّٰهِ وَبِحَمْدِهِ", "Sübhânallâhi ve bihamdihî", "Allah'ı hamdiyle tesbih ederim", 100),
            Step("اَسْتَغْفِرُ اللّٰهَ", "Estağfirullah", "Allah'tan bağışlanma dilerim", 100),
            Step("اَللّٰهُمَّ صَلِّ عَلٰى سَيِّدِنَا مُحَمَّدٍ", "Salavât", "Allah'ım, Efendimiz Muhammed'e salât eyle", 100),
        ),
    ),
)

val LIB_GUNLUK = listOf(
    LibItem("سُبْحَانَ اللّٰهِ", "Sübhânallah", "Allah her türlü noksanlıktan münezzehtir"),
    LibItem("اَلْحَمْدُ لِلّٰهِ", "Elhamdülillâh", "Hamd yalnızca Allah'adır"),
    LibItem("اَللّٰهُ اَكْبَرُ", "Allâhu Ekber", "Allah en büyüktür"),
    LibItem("لَا اِلٰهَ اِلَّا اللّٰهُ", "Lâ ilâhe illallah", "Allah'tan başka ilah yoktur", target = 100),
    LibItem(TEVHID_AR, TEVHID_TR, "Allah'tan başka ilah yoktur; tektir, ortağı yoktur. Mülk ve hamd O'nundur, O her şeye kadirdir.", target = 10, zikirOkunus = "Kelime-i Tevhîd"),
    LibItem(
        "سُبْحَانَ اللّٰهِ وَبِحَمْدِهِ", "Sübhânallâhi ve bihamdihî", "Allah'ı hamdiyle tesbih ederim",
        "Buhârî ve Müslim'deki hadise göre günde yüz kez söyleyenin günahları deniz köpüğü kadar olsa da bağışlanır.", 100,
    ),
    LibItem("سُبْحَانَ اللّٰهِ الْعَظِيمِ", "Sübhânallâhi'l-azîm", "Yüce Allah'ı tesbih ederim"),
    LibItem(
        "لَا حَوْلَ وَلَا قُوَّةَ اِلَّا بِاللّٰهِ", "Lâ havle ve lâ kuvvete illâ billâh", "Güç ve kuvvet ancak Allah'ın yardımıyladır",
        "Hadiste cennet hazinelerinden bir hazine olarak bildirilmiştir (Buhârî, Müslim).",
    ),
    LibItem("حَسْبُنَا اللّٰهُ وَنِعْمَ الْوَكِيلُ", "Hasbünallâhu ve ni'me'l-vekîl", "Allah bize yeter, O ne güzel vekildir"),
    LibItem("اَسْتَغْفِرُ اللّٰهَ", "Estağfirullah", "Allah'tan bağışlanma dilerim", target = 100),
    LibItem("اَسْتَغْفِرُ اللّٰهَ الْعَظِيمَ وَاَتُوبُ اِلَيْهِ", "Estağfirullâhe'l-azîm ve etûbü ileyh", "Yüce Allah'tan bağışlanma diler, O'na tövbe ederim", target = 100),
    LibItem(
        "لَا اِلٰهَ اِلَّا اَنْتَ سُبْحَانَكَ اِنِّي كُنْتُ مِنَ الظَّالِمِينَ",
        "Lâ ilâhe illâ ente sübhâneke innî küntü mine'z-zâlimîn",
        "Senden başka ilah yoktur, Seni tesbih ederim; ben zalimlerden oldum",
        "Hz. Yûnus'un duası (Enbiyâ 87).", 100, "Yûnus Duası",
    ),
)

val LIB_SALAVAT = listOf(
    LibItem("اَللّٰهُمَّ صَلِّ عَلٰى سَيِّدِنَا مُحَمَّدٍ", "Allâhümme salli alâ seyyidinâ Muhammed", "Allah'ım, Efendimiz Muhammed'e salât eyle", target = 100, zikirOkunus = "Salavât"),
    LibItem(
        "اَللّٰهُمَّ صَلِّ عَلٰى مُحَمَّدٍ وَعَلٰى اٰلِ مُحَمَّدٍ",
        "Allâhümme salli alâ Muhammedin ve alâ âli Muhammed",
        "Allah'ım, Muhammed'e ve onun âline salât eyle", target = 100, zikirOkunus = "Salli Salavâtı",
    ),
    LibItem("صَلَّى اللّٰهُ عَلَيْهِ وَسَلَّمَ", "Sallallâhu aleyhi ve sellem", "Allah'ın salât ve selâmı O'nun üzerine olsun", target = 100),
    LibItem(
        "اَللّٰهُمَّ صَلِّ وَسَلِّمْ عَلٰى سَيِّدِنَا مُحَمَّدٍ",
        "Allâhümme salli ve sellim alâ seyyidinâ Muhammed",
        "Allah'ım, Efendimiz Muhammed'e salât ve selâm eyle", target = 100, zikirOkunus = "Salavât-ı Şerîfe",
    ),
)

// Esmâ-ül Hüsnâ: (Arapça, isim, zikir hâli, anlam)
private val ESMA_RAW = listOf(
    arrayOf("الله", "Allah", "Allah", "Eşi benzeri olmayan, bütün noksan sıfatlardan münezzeh tek ilah"),
    arrayOf("الرحمن", "Er-Rahmân", "Yâ Rahmân", "Dünyada bütün yaratılmışlara merhamet eden"),
    arrayOf("الرحيم", "Er-Rahîm", "Yâ Rahîm", "Âhirette müminlere merhamet eden"),
    arrayOf("الملك", "El-Melik", "Yâ Melik", "Mülkün ve kâinatın sahibi"),
    arrayOf("القدوس", "El-Kuddûs", "Yâ Kuddûs", "Her türlü eksiklikten uzak"),
    arrayOf("السلام", "Es-Selâm", "Yâ Selâm", "Esenlik veren"),
    arrayOf("المؤمن", "El-Mü'min", "Yâ Mü'min", "Güven veren"),
    arrayOf("المهيمن", "El-Müheymin", "Yâ Müheymin", "Gözetip koruyan"),
    arrayOf("العزيز", "El-Azîz", "Yâ Azîz", "İzzet sahibi, her şeye galip"),
    arrayOf("الجبار", "El-Cebbâr", "Yâ Cebbâr", "Dilediğini yaptırmaya gücü yeten"),
    arrayOf("المتكبر", "El-Mütekebbir", "Yâ Mütekebbir", "Büyüklükte eşi olmayan"),
    arrayOf("الخالق", "El-Hâlık", "Yâ Hâlık", "Yoktan var eden"),
    arrayOf("البارئ", "El-Bâri'", "Yâ Bâri'", "Her şeyi kusursuz yaratan"),
    arrayOf("المصور", "El-Musavvir", "Yâ Musavvir", "Her şeye şekil veren"),
    arrayOf("الغفار", "El-Gaffâr", "Yâ Gaffâr", "Günahları örtüp bağışlayan"),
    arrayOf("القهار", "El-Kahhâr", "Yâ Kahhâr", "Her şeye galip gelen"),
    arrayOf("الوهاب", "El-Vehhâb", "Yâ Vehhâb", "Karşılıksız bol bol veren"),
    arrayOf("الرزاق", "Er-Rezzâk", "Yâ Rezzâk", "Rızık veren"),
    arrayOf("الفتاح", "El-Fettâh", "Yâ Fettâh", "Hayır kapılarını açan"),
    arrayOf("العليم", "El-Alîm", "Yâ Alîm", "Her şeyi bilen"),
    arrayOf("القابض", "El-Kâbıd", "Yâ Kâbıd", "Dilediğine darlık veren"),
    arrayOf("الباسط", "El-Bâsıt", "Yâ Bâsıt", "Dilediğine bolluk veren"),
    arrayOf("الخافض", "El-Hâfıd", "Yâ Hâfıd", "Dilediğini alçaltan"),
    arrayOf("الرافع", "Er-Râfi'", "Yâ Râfi'", "Dilediğini yükselten"),
    arrayOf("المعز", "El-Muız", "Yâ Muız", "Dilediğine izzet veren"),
    arrayOf("المذل", "El-Müzil", "Yâ Müzil", "Dilediğini zillete düşüren"),
    arrayOf("السميع", "Es-Semî'", "Yâ Semî'", "Her şeyi işiten"),
    arrayOf("البصير", "El-Basîr", "Yâ Basîr", "Her şeyi gören"),
    arrayOf("الحكم", "El-Hakem", "Yâ Hakem", "Mutlak hüküm sahibi"),
    arrayOf("العدل", "El-Adl", "Yâ Adl", "Mutlak adalet sahibi"),
    arrayOf("اللطيف", "El-Latîf", "Yâ Latîf", "Lütuf sahibi, en ince işleri bilen"),
    arrayOf("الخبير", "El-Habîr", "Yâ Habîr", "Her şeyden haberdar olan"),
    arrayOf("الحليم", "El-Halîm", "Yâ Halîm", "Cezada acele etmeyen, yumuşak davranan"),
    arrayOf("العظيم", "El-Azîm", "Yâ Azîm", "Pek yüce"),
    arrayOf("الغفور", "El-Gafûr", "Yâ Gafûr", "Çok bağışlayan"),
    arrayOf("الشكور", "Eş-Şekûr", "Yâ Şekûr", "Az amele çok mükâfat veren"),
    arrayOf("العلي", "El-Aliyy", "Yâ Aliyy", "Yüceler yücesi"),
    arrayOf("الكبير", "El-Kebîr", "Yâ Kebîr", "Pek büyük"),
    arrayOf("الحفيظ", "El-Hafîz", "Yâ Hafîz", "Her şeyi koruyan"),
    arrayOf("المقيت", "El-Mukît", "Yâ Mukît", "Her canlının gıdasını veren"),
    arrayOf("الحسيب", "El-Hasîb", "Yâ Hasîb", "Hesaba çeken"),
    arrayOf("الجليل", "El-Celîl", "Yâ Celîl", "Celâl ve azamet sahibi"),
    arrayOf("الكريم", "El-Kerîm", "Yâ Kerîm", "Keremi, ikramı bol"),
    arrayOf("الرقيب", "Er-Rakîb", "Yâ Rakîb", "Her an gözetleyen"),
    arrayOf("المجيب", "El-Mucîb", "Yâ Mucîb", "Duaları kabul eden"),
    arrayOf("الواسع", "El-Vâsi'", "Yâ Vâsi'", "İlmi ve rahmeti her şeyi kuşatan"),
    arrayOf("الحكيم", "El-Hakîm", "Yâ Hakîm", "Her işi hikmetli olan"),
    arrayOf("الودود", "El-Vedûd", "Yâ Vedûd", "Kullarını seven, sevilmeye layık olan"),
    arrayOf("المجيد", "El-Mecîd", "Yâ Mecîd", "Şanı yüce"),
    arrayOf("الباعث", "El-Bâis", "Yâ Bâis", "Ölüleri dirilten"),
    arrayOf("الشهيد", "Eş-Şehîd", "Yâ Şehîd", "Her yerde hazır ve nâzır"),
    arrayOf("الحق", "El-Hakk", "Yâ Hakk", "Varlığı hiç değişmeden duran, gerçek"),
    arrayOf("الوكيل", "El-Vekîl", "Yâ Vekîl", "Kendisine güvenilen"),
    arrayOf("القوي", "El-Kaviyy", "Yâ Kaviyy", "Kudretli, güçlü"),
    arrayOf("المتين", "El-Metîn", "Yâ Metîn", "Çok sağlam"),
    arrayOf("الولي", "El-Veliyy", "Yâ Veliyy", "Müminlerin dostu"),
    arrayOf("الحميد", "El-Hamîd", "Yâ Hamîd", "Övülmeye layık"),
    arrayOf("المحصي", "El-Muhsî", "Yâ Muhsî", "Her şeyi tek tek bilen"),
    arrayOf("المبدئ", "El-Mübdi'", "Yâ Mübdi'", "Örneksiz yaratan"),
    arrayOf("المعيد", "El-Muîd", "Yâ Muîd", "Yeniden yaratan"),
    arrayOf("المحيي", "El-Muhyî", "Yâ Muhyî", "Can veren"),
    arrayOf("المميت", "El-Mümît", "Yâ Mümît", "Ölümü yaratan"),
    arrayOf("الحي", "El-Hayy", "Yâ Hayy", "Ezelî ve ebedî diri"),
    arrayOf("القيوم", "El-Kayyûm", "Yâ Kayyûm", "Her şeyi ayakta tutan"),
    arrayOf("الواجد", "El-Vâcid", "Yâ Vâcid", "Dilediğini dilediği an bulan"),
    arrayOf("الماجد", "El-Mâcid", "Yâ Mâcid", "Kadri ve şanı büyük"),
    arrayOf("الواحد", "El-Vâhid", "Yâ Vâhid", "Tek, eşsiz"),
    arrayOf("الصمد", "Es-Samed", "Yâ Samed", "Hiçbir şeye muhtaç olmayan"),
    arrayOf("القادر", "El-Kâdir", "Yâ Kâdir", "Her şeye gücü yeten"),
    arrayOf("المقتدر", "El-Muktedir", "Yâ Muktedir", "Kudret sahibi"),
    arrayOf("المقدم", "El-Mukaddim", "Yâ Mukaddim", "Dilediğini öne alan"),
    arrayOf("المؤخر", "El-Muahhir", "Yâ Muahhir", "Dilediğini geri bırakan"),
    arrayOf("الأول", "El-Evvel", "Yâ Evvel", "Başlangıcı olmayan"),
    arrayOf("الآخر", "El-Âhir", "Yâ Âhir", "Sonu olmayan"),
    arrayOf("الظاهر", "Ez-Zâhir", "Yâ Zâhir", "Varlığı apaçık"),
    arrayOf("الباطن", "El-Bâtın", "Yâ Bâtın", "Akılların idrakinden gizli"),
    arrayOf("الوالي", "El-Vâlî", "Yâ Vâlî", "Kâinatı yöneten"),
    arrayOf("المتعالي", "El-Müteâlî", "Yâ Müteâlî", "Pek yüce"),
    arrayOf("البر", "El-Berr", "Yâ Berr", "İyiliği bol"),
    arrayOf("التواب", "Et-Tevvâb", "Yâ Tevvâb", "Tövbeleri kabul eden"),
    arrayOf("المنتقم", "El-Müntekım", "Yâ Müntekım", "Zalimleri cezalandıran"),
    arrayOf("العفو", "El-Afüvv", "Yâ Afüvv", "Affı çok olan"),
    arrayOf("الرؤوف", "Er-Raûf", "Yâ Raûf", "Çok şefkatli"),
    arrayOf("مالك الملك", "Mâlikü'l-Mülk", "Yâ Mâlike'l-Mülk", "Mülkün ebedî sahibi"),
    arrayOf("ذو الجلال والإكرام", "Zü'l-Celâli ve'l-İkrâm", "Yâ Ze'l-Celâli ve'l-İkrâm", "Celâl ve ikram sahibi"),
    arrayOf("المقسط", "El-Muksit", "Yâ Muksit", "Adaletle hükmeden"),
    arrayOf("الجامع", "El-Câmi'", "Yâ Câmi'", "Dilediğini bir araya getiren"),
    arrayOf("الغني", "El-Ganiyy", "Yâ Ganiyy", "Hiçbir şeye muhtaç olmayan"),
    arrayOf("المغني", "El-Muğnî", "Yâ Muğnî", "Dilediğini zengin eden"),
    arrayOf("المانع", "El-Mâni'", "Yâ Mâni'", "Dilemediği şeye engel olan"),
    arrayOf("الضار", "Ed-Dârr", "Yâ Dârr", "Elem ve zarar verenleri yaratan"),
    arrayOf("النافع", "En-Nâfi'", "Yâ Nâfi'", "Fayda veren"),
    arrayOf("النور", "En-Nûr", "Yâ Nûr", "Âlemleri nurlandıran"),
    arrayOf("الهادي", "El-Hâdî", "Yâ Hâdî", "Hidayet veren"),
    arrayOf("البديع", "El-Bedî'", "Yâ Bedî'", "Eşsiz yaratan"),
    arrayOf("الباقي", "El-Bâkî", "Yâ Bâkî", "Varlığı sonsuz"),
    arrayOf("الوارث", "El-Vâris", "Yâ Vâris", "Her şeyin asıl sahibi"),
    arrayOf("الرشيد", "Er-Reşîd", "Yâ Reşîd", "Doğru yolu gösteren"),
    arrayOf("الصبور", "Es-Sabûr", "Yâ Sabûr", "Çok sabırlı"),
)

val LIB_ESMA: List<LibItem> = ESMA_RAW.mapIndexed { i, a ->
    LibItem(a[0], "${i + 1}. ${a[1]}", a[3], target = 33, zikirOkunus = a[2])
}

// ───────────── Tarih yardımcıları ─────────────
fun dayKey(d: LocalDate = LocalDate.now()): String = d.toString()

fun streakOf(history: Map<String, Int>, today: LocalDate = LocalDate.now()): Int {
    var d = today
    if ((history[dayKey(d)] ?: 0) <= 0) d = d.minusDays(1)
    var n = 0
    while ((history[dayKey(d)] ?: 0) > 0) {
        n++
        d = d.minusDays(1)
    }
    return n
}

// ───────────── Kalıcı depo ─────────────
class Repo(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("zikirmatik", Context.MODE_PRIVATE)

    fun loadZikirs(): List<Zikir> {
        val s = p.getString("list", null) ?: return VARSAYILAN
        return runCatching { zikirsFrom(JSONArray(s)) }.getOrNull()?.takeIf { it.isNotEmpty() } ?: VARSAYILAN
    }

    fun saveZikirs(list: List<Zikir>) = p.edit().putString("list", zikirsTo(list).toString()).apply()

    var selId: Long
        get() = p.getLong("selId", 1L)
        set(v) = p.edit().putLong("selId", v).apply()

    var total: Int
        get() = p.getInt("total", 0)
        set(v) = p.edit().putInt("total", v).apply()

    var bestStreak: Int
        get() = p.getInt("bestStreak", 0)
        set(v) = p.edit().putInt("bestStreak", v).apply()

    fun loadSettings(): Settings {
        val s = p.getString("settings", null)
        if (s == null) {
            // 1.x sürümünden geçiş
            return Settings(haptic = if (p.getBoolean("vib", true)) 2 else 0)
        }
        return runCatching {
            val o = JSONObject(s)
            Settings(
                theme = o.optInt("theme", 0).coerceIn(0, 2),
                haptic = o.optInt("haptic", 2).coerceIn(0, 3),
                sound = o.optBoolean("sound", false),
                volumeKeys = o.optBoolean("volumeKeys", true),
                stopAtTarget = o.optBoolean("stopAtTarget", false),
                guardMs = o.optInt("guardMs", 70),
                fontScale = o.optDouble("fontScale", 1.0).toFloat().coerceIn(0.85f, 1.4f),
                milestone = o.optInt("milestone", 33),
                dailyGoal = o.optInt("dailyGoal", 0),
                remMorning = o.optBoolean("remMorning", false),
                morningMin = o.optInt("morningMin", 7 * 60),
                remEvening = o.optBoolean("remEvening", false),
                eveningMin = o.optInt("eveningMin", 18 * 60 + 30),
            )
        }.getOrDefault(Settings())
    }

    fun saveSettings(s: Settings) {
        val o = JSONObject()
            .put("theme", s.theme).put("haptic", s.haptic).put("sound", s.sound)
            .put("volumeKeys", s.volumeKeys).put("stopAtTarget", s.stopAtTarget)
            .put("guardMs", s.guardMs).put("fontScale", s.fontScale.toDouble())
            .put("milestone", s.milestone).put("dailyGoal", s.dailyGoal)
            .put("remMorning", s.remMorning).put("morningMin", s.morningMin)
            .put("remEvening", s.remEvening).put("eveningMin", s.eveningMin)
        p.edit().putString("settings", o.toString()).apply()
    }

    fun loadHistory(): Map<String, Int> {
        val s = p.getString("history", null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(s)
            o.keys().asSequence().associateWith { o.optInt(it, 0) }
        }.getOrDefault(emptyMap())
    }

    fun saveHistory(h: Map<String, Int>) {
        val o = JSONObject()
        h.forEach { (k, v) -> if (v > 0) o.put(k, v) }
        p.edit().putString("history", o.toString()).apply()
    }

    fun loadVirds(): List<Vird> {
        val s = p.getString("virds", null) ?: return VARSAYILAN_VIRDLER
        return runCatching { virdsFrom(JSONArray(s)) }.getOrDefault(VARSAYILAN_VIRDLER)
    }

    fun saveVirds(v: List<Vird>) = p.edit().putString("virds", virdsTo(v).toString()).apply()

    fun loadSession(): Session? {
        val s = p.getString("session", null) ?: return null
        return runCatching {
            val o = JSONObject(s)
            Session(o.getLong("v"), o.getInt("s"), o.getInt("c"))
        }.getOrNull()
    }

    fun saveSession(s: Session?) {
        if (s == null) p.edit().remove("session").apply()
        else p.edit().putString("session", JSONObject().put("v", s.virdId).put("s", s.step).put("c", s.count).toString()).apply()
    }

    /** Ana ekran widget'ından sayım (uygulama kapalıyken). */
    fun widgetIncrement(): Zikir? {
        val list = loadZikirs().toMutableList()
        val i = list.indexOfFirst { it.id == selId }.let { if (it < 0) 0 else it }
        val z = list.getOrNull(i) ?: return null
        val settings = loadSettings()
        if (settings.stopAtTarget && z.count >= z.target) return z
        val nz = z.copy(count = z.count + 1)
        list[i] = nz
        saveZikirs(list)
        val h = loadHistory().toMutableMap()
        val k = dayKey()
        h[k] = (h[k] ?: 0) + 1
        saveHistory(h)
        total = total + 1
        val st = streakOf(h)
        if (st > bestStreak) bestStreak = st
        return nz
    }

    fun exportJson(): String = JSONObject()
        .put("app", "zikirmatik").put("version", 2)
        .put("list", p.getString("list", null) ?: zikirsTo(VARSAYILAN).toString())
        .put("selId", selId).put("total", total).put("bestStreak", bestStreak)
        .put("settings", p.getString("settings", "{}"))
        .put("history", p.getString("history", "{}"))
        .put("virds", p.getString("virds", null) ?: virdsTo(VARSAYILAN_VIRDLER).toString())
        .toString(2)

    fun importJson(text: String): Boolean = runCatching {
        val o = JSONObject(text)
        require(o.optString("app") == "zikirmatik")
        val list = zikirsFrom(JSONArray(o.getString("list")))
        require(list.isNotEmpty())
        val virds = virdsFrom(JSONArray(o.optString("virds", "[]")))
        JSONObject(o.optString("history", "{}"))
        JSONObject(o.optString("settings", "{}"))
        p.edit()
            .putString("list", zikirsTo(list).toString())
            .putLong("selId", o.optLong("selId", list.first().id))
            .putInt("total", o.optInt("total", 0))
            .putInt("bestStreak", o.optInt("bestStreak", 0))
            .putString("settings", o.optString("settings", "{}"))
            .putString("history", o.optString("history", "{}"))
            .putString("virds", virdsTo(virds.ifEmpty { VARSAYILAN_VIRDLER }).toString())
            .remove("session")
            .apply()
        true
    }.getOrDefault(false)

    private fun zikirsFrom(a: JSONArray): List<Zikir> = (0 until a.length()).map { i ->
        val o = a.getJSONObject(i)
        Zikir(
            id = o.getLong("id"),
            arapca = o.optString("ar"),
            okunus = o.getString("ok"),
            anlam = o.optString("an"),
            fazilet = o.optString("fz"),
            count = o.optInt("c", 0).coerceAtLeast(0),
            target = o.optInt("t", 33).coerceAtLeast(1),
        )
    }

    private fun zikirsTo(list: List<Zikir>) = JSONArray().apply {
        list.forEach {
            put(
                JSONObject().put("id", it.id).put("ar", it.arapca).put("ok", it.okunus)
                    .put("an", it.anlam).put("fz", it.fazilet).put("c", it.count).put("t", it.target)
            )
        }
    }

    private fun virdsFrom(a: JSONArray): List<Vird> = (0 until a.length()).map { i ->
        val o = a.getJSONObject(i)
        val sa = o.getJSONArray("steps")
        Vird(
            o.getLong("id"), o.getString("name"),
            (0 until sa.length()).map { j ->
                val s = sa.getJSONObject(j)
                Step(s.optString("ar"), s.getString("ok"), s.optString("an"), s.optInt("t", 33).coerceAtLeast(1))
            },
        )
    }

    private fun virdsTo(v: List<Vird>) = JSONArray().apply {
        v.forEach { vd ->
            val sa = JSONArray()
            vd.steps.forEach { s -> sa.put(JSONObject().put("ar", s.arapca).put("ok", s.okunus).put("an", s.anlam).put("t", s.target)) }
            put(JSONObject().put("id", vd.id).put("name", vd.name).put("steps", sa))
        }
    }
}
