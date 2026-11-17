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
    val bead: Int = 0,             // BEADS sırası: 0 Kehribar, 1 Oltu, 2 Sedef, 3 Kristal, 4.. doğal taşlar
    val lang: Int = 0,             // 0 sistem dili, 1.. LANGS sırası (tr, en, ar, ur, id, fr, de, ru)
    val haptic: Int = 2,           // 0 kapalı, 1 hafif, 2 normal, 3 güçlü
    val sound: Boolean = true,
    val soundType: Int = 0,        // 0 kehribar, 1 ahşap, 2 kristal
    val soundVol: Int = 1,         // 0 düşük, 1 orta, 2 yüksek
    val slideSound: Boolean = false, // tanenin ipte kayma (sürtünme) sesi
    val beadShape: Int = 0,        // 0 yuvarlak, 1 oval (arpa)
    val imame: Int = 0,            // 0 telkari başlıklı, 1 klasik (taşın kendisinden, kamçılı)
    val cord: Int = 0,             // ip rengi: 0 kahve, 1 kırmızı, 2 siyah
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
    val baslik: String = "",       // dualar için: ne zaman okunduğu
)

// ───────────── Hazır içerik ─────────────
val VARSAYILAN = listOf(
    Zikir(1, "سُبْحَانَ اللّٰهِ", "Sübhânallâh", "Allah her türlü noksanlıktan münezzehtir", "Namaz sonrası tesbihatta 33 kez okunur."),
    Zikir(2, "اَلْحَمْدُ لِلّٰهِ", "Elhamdülillâh", "Hamd yalnızca Allah'adır", "Namaz sonrası tesbihatta 33 kez okunur."),
    Zikir(3, "اَللّٰهُ اَكْبَرُ", "Allâhu Ekber", "Allah en büyüktür", "Namaz sonrası tesbihatta 33 kez okunur."),
    Zikir(4, "لَا اِلٰهَ اِلَّا اللّٰهُ", "Lâ ilâhe illallâh", "Allah'tan başka ilah yoktur", "Zikrin en faziletlisi \"Lâ ilâhe illallâh\"tır. (Tirmizî, Daavât 9; İbn Mâce, Edeb 55)", target = 100),
    Zikir(5, "اَسْتَغْفِرُ اللّٰهَ", "Estağfirullâh", "Allah'tan bağışlanma dilerim", "Allah'a yemin olsun ki ben günde yetmiş defadan fazla Allah'tan bağışlanma diler, O'na tövbe ederim. (Buhârî, Daavât 3) · İstiğfara devam edene Allah her darlıktan bir çıkış, her kederden bir ferahlık verir ve onu ummadığı yerden rızıklandırır. (Ebû Dâvûd, Vitir 26)", target = 100),
    Zikir(6, "اَللّٰهُمَّ صَلِّ عَلٰى سَيِّدِنَا مُحَمَّدٍ", "Salavât", "Allah'ım, Efendimiz Muhammed'e salât eyle", "Kim bana bir salavat getirirse Allah ona on rahmet eder. (Müslim, Salât 70)", target = 100),
)

private const val TEVHID_AR = "لَا اِلٰهَ اِلَّا اللّٰهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلٰى كُلِّ شَيْءٍ قَدِيرٌ"
private const val TEVHID_TR = "Lâ ilâhe illallâhu vahdehû lâ şerîke leh, lehü'l-mülkü ve lehü'l-hamdü ve hüve alâ külli şey'in kadîr"

val VARSAYILAN_VIRDLER = listOf(
    Vird(
        101, "Namaz Sonrası Tesbihat",
        listOf(
            Step("سُبْحَانَ اللّٰهِ", "Sübhânallâh", "Allah her türlü noksanlıktan münezzehtir", 33),
            Step("اَلْحَمْدُ لِلّٰهِ", "Elhamdülillâh", "Hamd yalnızca Allah'adır", 33),
            Step("اَللّٰهُ اَكْبَرُ", "Allâhu Ekber", "Allah en büyüktür", 33),
            Step(TEVHID_AR, "Kelime-i Tevhîd", "Allah'tan başka ilah yoktur; tektir, ortağı yoktur. Mülk ve hamd O'nundur, O her şeye kadirdir.", 1),
        ),
    ),
    Vird(
        102, "Sabah - Akşam Virdi",
        listOf(
            Step("سُبْحَانَ اللّٰهِ وَبِحَمْدِهِ", "Sübhânallâhi ve bihamdihî", "Allah'ı hamdiyle tesbih ederim", 100),
            Step("اَسْتَغْفِرُ اللّٰهَ", "Estağfirullâh", "Allah'tan bağışlanma dilerim", 100),
            Step("اَللّٰهُمَّ صَلِّ عَلٰى سَيِّدِنَا مُحَمَّدٍ", "Salavât", "Allah'ım, Efendimiz Muhammed'e salât eyle", 100),
        ),
    ),
)

/** Sünnet sabah-akşam zikirleri (sonradan eklenen hazır set). */
val SUNNET_VIRD = Vird(
    103, "Sünnet Sabah-Akşam Zikirleri",
    listOf(
        Step(SEYYID_AR, "Seyyidü'l-İstiğfâr", "İstiğfarın efendisi", 1),
        Step(BISMILLAH_AR, "Bismillâhillezî", "İsmi anıldığında hiçbir şeyin zarar veremediği Allah'ın adıyla", 3),
        Step("رَضِيتُ بِاللّٰهِ رَبًّا وَبِالْاِسْلَامِ دِينًا وَبِمُحَمَّدٍ نَبِيًّا", "Radîtü Billâhi", "Rab olarak Allah'tan, din olarak İslâm'dan, peygamber olarak Muhammed'den razıyım", 3),
        Step("سُبْحَانَ اللّٰهِ وَبِحَمْدِهِ", "Sübhânallâhi ve bihamdihî", "Allah'ı hamdiyle tesbih ederim", 100),
        Step(TEVHID_AR, "Kelime-i Tevhîd", "Allah'tan başka ilah yoktur; tektir, ortağı yoktur.", 10),
        Step("اَللّٰهُمَّ صَلِّ عَلٰى سَيِّدِنَا مُحَمَّدٍ", "Salavât", "Allah'ım, Efendimiz Muhammed'e salât eyle", 10),
    ),
)

// ───────────── Peygamber Efendimiz'in (s.a.v.) tavsiye ettiği zikirler ─────────────
// Yalnızca muteber hadis kaynaklarında yer alan rivayetler; kaynak her maddede belirtilir.
private const val SEYYID_AR = "اَللّٰهُمَّ اَنْتَ رَبِّي لَا اِلٰهَ اِلَّا اَنْتَ خَلَقْتَنِي وَاَنَا عَبْدُكَ وَاَنَا عَلٰى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ اَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ اَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ وَاَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَاِنَّهُ لَا يَغْفِرُ الذُّنُوبَ اِلَّا اَنْتَ"
private const val IBRAHIMIYYE_AR = "اَللّٰهُمَّ صَلِّ عَلٰى مُحَمَّدٍ وَعَلٰى اٰلِ مُحَمَّدٍ كَمَا صَلَّيْتَ عَلٰى اِبْرَاهِيمَ وَعَلٰى اٰلِ اِبْرَاهِيمَ اِنَّكَ حَمِيدٌ مَجِيدٌ اَللّٰهُمَّ بَارِكْ عَلٰى مُحَمَّدٍ وَعَلٰى اٰلِ مُحَمَّدٍ كَمَا بَارَكْتَ عَلٰى اِبْرَاهِيمَ وَعَلٰى اٰلِ اِبْرَاهِيمَ اِنَّكَ حَمِيدٌ مَجِيدٌ"
private const val BISMILLAH_AR = "بِسْمِ اللّٰهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْاَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ"

private val SUNNET_ALL = listOf(
    LibItem(
        TEVHID_AR, TEVHID_TR, "Allah'tan başka ilah yoktur; tektir, ortağı yoktur. Mülk ve hamd O'nundur, O her şeye kadirdir.",
        "Günde yüz defa söyleyene on köle azat etmiş gibi sevap verilir, yüz iyilik yazılır, yüz hatası silinir ve o gün akşama kadar şeytandan korunur. (Buhârî, Müslim)",
        100, "Kelime-i Tevhîd",
    ),
    LibItem(
        "سُبْحَانَ اللّٰهِ وَبِحَمْدِهِ", "Sübhânallâhi ve bihamdihî", "Allah'ı hamdiyle tesbih ederim",
        "Günde yüz defa söyleyenin hataları deniz köpüğü kadar olsa bile silinir. (Buhârî, Müslim)", 100,
    ),
    LibItem(
        "سُبْحَانَ اللّٰهِ وَبِحَمْدِهِ سُبْحَانَ اللّٰهِ الْعَظِيمِ", "Sübhânallâhi ve bihamdihî, sübhânallâhi'l-azîm",
        "Allah'ı hamdiyle tesbih ederim, yüce Allah'ı tesbih ederim",
        "Dile hafif, mîzanda ağır, Rahmân'a sevimli iki sözdür. (Buhârî, Müslim)", 100, "Sübhânallâhi ve bihamdihî, sübhânallâhi'l-azîm",
    ),
    LibItem(
        "سُبْحَانَ اللّٰهِ وَالْحَمْدُ لِلّٰهِ وَلَا اِلٰهَ اِلَّا اللّٰهُ وَاللّٰهُ اَكْبَرُ",
        "Sübhânallâhi ve'l-hamdü lillâhi ve lâ ilâhe illallâhu vallâhu ekber",
        "Allah noksanlıklardan münezzehtir, hamd Allah'adır, Allah'tan başka ilah yoktur, Allah en büyüktür",
        "Allah'a en sevimli sözler bu dördüdür; hangisiyle başladığının bir zararı yoktur. (Müslim)", 100, "Bâkıyât-ı Sâlihât",
    ),
    LibItem(
        "لَا اِلٰهَ اِلَّا اللّٰهُ", "Lâ ilâhe illallâh", "Allah'tan başka ilah yoktur",
        "Zikrin en faziletlisi Lâ ilâhe illallâh'tır. (Tirmizî, İbn Mâce)", 100,
    ),
    LibItem(
        "سُبْحَانَ اللّٰهِ", "Sübhânallâh", "Allah her türlü noksanlıktan münezzehtir",
        "Günde yüz defa tesbih edene bin iyilik yazılır ya da bin hatası silinir. (Müslim)", 100,
    ),
    LibItem(
        "اَلْحَمْدُ لِلّٰهِ", "Elhamdülillâh", "Hamd yalnızca Allah'adır",
        "Elhamdülillâh mîzanı doldurur. (Müslim) Duanın en faziletlisi Elhamdülillâh'tır. (Tirmizî)", 33,
    ),
    LibItem(
        "اَللّٰهُ اَكْبَرُ", "Allâhu Ekber", "Allah en büyüktür",
        "Namazdan sonra 33 tesbih, 33 hamd, 33 tekbir söyleyip yüzü kelime-i tevhid ile tamamlayanın hataları deniz köpüğü kadar olsa da bağışlanır. (Müslim)", 33,
    ),
    LibItem(
        "لَا حَوْلَ وَلَا قُوَّةَ اِلَّا بِاللّٰهِ", "Lâ havle ve lâ kuvvete illâ billâh", "Güç ve kuvvet ancak Allah'ın yardımıyladır",
        "Cennet hazinelerinden bir hazinedir. (Buhârî, Müslim)", 100,
    ),
    LibItem(
        "اَسْتَغْفِرُ اللّٰهَ وَاَتُوبُ اِلَيْهِ", "Estağfirullâhe ve etûbü ileyh", "Allah'tan bağışlanma diler, O'na tövbe ederim",
        "Peygamberimiz günde yetmiş defadan fazla istiğfar ve tövbe ettiğini bildirmiştir. (Buhârî) Müslim'in rivayetinde yüz defa geçer.", 100,
    ),
    LibItem(
        SEYYID_AR,
        "Allâhümme ente rabbî lâ ilâhe illâ ente halaktenî ve ene abdük, ve ene alâ ahdike ve va'dike mestata'tü, eûzü bike min şerri mâ sana'tü, ebûü leke bi ni'metike aleyye ve ebûü bi zenbî, fağfir lî fe innehû lâ yağfiru'z-zünûbe illâ ente",
        "Allah'ım! Sen benim Rabbimsin, Senden başka ilah yoktur. Beni Sen yarattın, ben Senin kulunum; gücüm yettiğince Sana verdiğim söz üzereyim. Yaptıklarımın şerrinden Sana sığınırım. Bana verdiğin nimetleri ve günahımı itiraf ederim; beni bağışla, çünkü günahları Senden başkası bağışlayamaz.",
        "İstiğfarın efendisidir. Gündüz inanarak söyleyip akşam olmadan vefat eden ya da gece söyleyip sabah olmadan vefat eden cennetliktir. (Buhârî)", 1, "Seyyidü'l-İstiğfâr",
    ),
    LibItem(
        "اَللّٰهُمَّ صَلِّ عَلٰى سَيِّدِنَا مُحَمَّدٍ", "Allâhümme salli alâ seyyidinâ Muhammed", "Allah'ım, Efendimiz Muhammed'e salât eyle",
        "Bana bir salavât getirene Allah on rahmet eder. (Müslim)", 100, "Salavât",
    ),
    LibItem(
        IBRAHIMIYYE_AR,
        "Allâhümme salli alâ Muhammedin ve alâ âli Muhammed, kemâ salleyte alâ İbrâhîme ve alâ âli İbrâhîm, inneke hamîdün mecîd. Allâhümme bârik alâ Muhammedin ve alâ âli Muhammed, kemâ bârekte alâ İbrâhîme ve alâ âli İbrâhîm, inneke hamîdün mecîd",
        "Allah'ım, İbrâhim'e ve âline salât ve bereket verdiğin gibi Muhammed'e ve âline de salât ve bereket ver. Şüphesiz Sen övülmeye layık ve şanı yücesin.",
        "Peygamberimizin, kendisine nasıl salavât getirileceğini soran ashabına bizzat öğrettiği salavâttır. (Buhârî, Müslim)", 10, "Salavât-ı İbrâhîmiyye",
    ),
    LibItem(
        "سُبْحَانَ اللّٰهِ وَبِحَمْدِهِ عَدَدَ خَلْقِهِ وَرِضَا نَفْسِهِ وَزِنَةَ عَرْشِهِ وَمِدَادَ كَلِمَاتِهِ",
        "Sübhânallâhi ve bihamdihî adede halkıhî ve rıdâ nefsihî ve zinete arşihî ve midâde kelimâtih",
        "Allah'ı, yarattıkları sayısınca, kendi rızası kadar, arşının ağırlığınca ve kelimelerinin mürekkebi miktarınca hamdiyle tesbih ederim",
        "Üçer defa söylenen bu dört söz, sabahtan beri yapılan zikirlerle tartılsa onlardan ağır gelir. (Müslim)", 3, "Cüveyriye Tesbihi",
    ),
    LibItem(
        BISMILLAH_AR,
        "Bismillâhillezî lâ yedurru mea'smihî şey'ün fi'l-ardı ve lâ fi's-semâi ve hüve's-semîu'l-alîm",
        "İsmi anıldığında yerde ve gökte hiçbir şeyin zarar veremediği Allah'ın adıyla. O her şeyi işiten ve bilendir.",
        "Akşam üç defa söyleyene sabaha kadar, sabah üç defa söyleyene akşama kadar ansızın bir musibet isabet etmez. (Ebû Dâvûd, Tirmizî)", 3, "Bismillâhillezî",
    ),
    LibItem(
        "رَضِيتُ بِاللّٰهِ رَبًّا وَبِالْاِسْلَامِ دِينًا وَبِمُحَمَّدٍ نَبِيًّا",
        "Radîtü billâhi rabben ve bi'l-islâmi dînen ve bi Muhammedin nebiyyen",
        "Rab olarak Allah'tan, din olarak İslâm'dan, peygamber olarak Muhammed'den razıyım",
        "Akşam ve sabah üçer defa söyleyeni kıyamet günü razı etmek Allah'ın üzerine bir haktır. (Tirmizî) Bunu söyleyene cennet vâcip olur. (Ebû Dâvûd)", 3, "Radîtü Billâhi",
    ),
    LibItem(
        "لَا اِلٰهَ اِلَّا اَنْتَ سُبْحَانَكَ اِنِّي كُنْتُ مِنَ الظَّالِمِينَ",
        "Lâ ilâhe illâ ente sübhâneke innî küntü mine'z-zâlimîn",
        "Senden başka ilah yoktur, Seni tesbih ederim; ben zalimlerden oldum",
        "Hz. Yûnus'un balığın karnındaki duasıdır (Enbiyâ, 21/87). Müslüman bir kişi bununla dua ettiğinde Allah duasını kabul eder. (Tirmizî)", 100, "Yûnus Duası",
    ),
    LibItem(
        "حَسْبُنَا اللّٰهُ وَنِعْمَ الْوَكِيلُ", "Hasbünallâhu ve ni'me'l-vekîl", "Allah bize yeter, O ne güzel vekildir",
        "Hz. İbrâhim ateşe atıldığında, Peygamberimiz de insanların kendilerine karşı toplandığı haber verildiğinde bunu söylemiştir. (Buhârî)", 33,
    ),
    LibItem(
        "رَبَّنَا اٰتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْاٰخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
        "Rabbenâ âtinâ fi'd-dünyâ haseneten ve fi'l-âhireti haseneten ve kınâ azâbe'n-nâr",
        "Rabbimiz! Bize dünyada da iyilik ver, âhirette de iyilik ver ve bizi ateş azabından koru",
        "Peygamberimizin en çok yaptığı duadır. (Buhârî, Müslim)", 7, "Rabbenâ Âtinâ",
    ),
    LibItem(
        "يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلٰى دِينِكَ", "Yâ Mukallibe'l-kulûb, sebbit kalbî alâ dînik",
        "Ey kalpleri halden hale çeviren Allah'ım! Kalbimi dinin üzere sabit kıl",
        "Hz. Ümmü Seleme, Peygamberimizin en çok yaptığı duanın bu olduğunu bildirmiştir. (Tirmizî)", 7, "Yâ Mukallibe'l-Kulûb",
    ),
    LibItem(
        "اَللّٰهُمَّ اِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي", "Allâhümme inneke afüvvün tühibbü'l-afve fa'fu annî",
        "Allah'ım! Sen affedicisin, affetmeyi seversin; beni affet",
        "Hz. Âişe Kadir gecesinde ne diyeceğini sorduğunda Peygamberimiz bu duayı öğretmiştir. (Tirmizî, İbn Mâce)", 33, "Kadir Gecesi Duası",
    ),
    LibItem(
        "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ اَسْتَغِيثُ", "Yâ Hayyu yâ Kayyûmu bi rahmetike esteğîs",
        "Ey diri ve her şeyi ayakta tutan Allah'ım! Rahmetinle yardım dilerim",
        "Peygamberimiz bir iş kendisini sıkıntıya soktuğunda böyle dua ederdi. (Tirmizî)", 33, "Yâ Hayyu Yâ Kayyûm",
    ),
    LibItem(
        "يَا ذَا الْجَلَالِ وَالْاِكْرَامِ", "Yâ Ze'l-Celâli ve'l-İkrâm", "Ey celâl ve ikram sahibi Allah'ım",
        "\"Yâ Ze'l-Celâli ve'l-İkrâm\" duasına devam edin. (Tirmizî)", 33,
    ),
    LibItem(
        "اَللّٰهُمَّ اَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْاِكْرَامِ",
        "Allâhümme ente's-selâmu ve minke's-selâm, tebârekte yâ ze'l-celâli ve'l-ikrâm",
        "Allah'ım! Sen selâmsın, selâmet Sendendir. Ey celâl ve ikram sahibi, Sen yücesin",
        "Peygamberimiz namazda selam verdikten sonra üç defa istiğfar eder, ardından bunu söylerdi. (Müslim)", 1, "Namaz Sonrası Selâm Duası",
    ),
    LibItem(
        "سُبْحَانَ الْمَلِكِ الْقُدُّوسِ", "Sübhâne'l-meliki'l-kuddûs", "Mülkün sahibi, her türlü eksiklikten uzak Allah'ı tesbih ederim",
        "Peygamberimiz vitir namazından selam verince üç defa söyler, üçüncüde sesini yükseltirdi. (Ebû Dâvûd, Nesâî)", 3,
    ),
)

// ───────────── Günlük dualar (zikirlerden ayrı kategori) ─────────────
/** Sünnet listesinden dualar bölümüne taşınan maddeler: okunuşun başı → başlık. */
private val DUA_MOVE = listOf(
    "Allâhümme ente rabbî" to "Seyyidü'l-istiğfâr · sabah ve akşam",
    "Bismillâhillezî lâ yedurru" to "Sabah ve akşam · 3 kez",
    "Radîtü billâhi" to "Sabah ve akşam · 3 kez",
    "Lâ ilâhe illâ ente sübhâneke" to "Darlıkta · Yûnus (a.s.) duası",
    "Rabbenâ âtinâ" to "En çok yaptığı dua",
    "Yâ Mukallibe'l-kulûb" to "Kalbin imanda sebatı için",
    "Allâhümme inneke afüvvün" to "Af dilerken · Kadir gecesi",
    "Yâ Hayyu yâ Kayyûmu" to "Sıkıntı ve keder anında",
    "Allâhümme ente's-selâmu" to "Namazdan sonra · 3 Estağfirullâh ile",
)

val LIB_SUNNET: List<LibItem> = SUNNET_ALL.filter { s -> DUA_MOVE.none { s.okunus.startsWith(it.first) } }

private fun moved(key: String): LibItem {
    val bs = DUA_MOVE.first { it.first == key }.second
    return SUNNET_ALL.first { it.okunus.startsWith(key) }.copy(baslik = bs)
}

/** Peygamber Efendimiz'in gün içindeki duaları: sabahtan geceye, ardından özel durumlar. */
val LIB_DUA: List<LibItem> = listOf(
    LibItem("اَلْحَمْدُ لِلّٰهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ", "Elhamdü lillâhillezî ahyânâ ba'de mâ emâtenâ ve ileyhi'n-nüşûr", "Bizi öldürdükten sonra dirilten Allah'a hamdolsun; dönüş O'nadır.", fazilet = "Hz. Peygamber uyandığında böyle derdi. (Buhârî, Daavât 7)", target = 1, zikirOkunus = "Uykudan uyanınca", baslik = "Uykudan uyanınca"),
    LibItem("اَللّٰهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْخُبُثِ وَالْخَبَائِثِ", "Allâhümme innî eûzü bike mine'l-hubsi ve'l-habâis", "Allah'ım, erkek ve dişi şeytanların şerrinden sana sığınırım.", fazilet = "Hz. Peygamber tuvalete girmeden önce böyle derdi. (Buhârî, Vudû 9; Müslim, Hayz 122)", target = 1, zikirOkunus = "Tuvalete girerken", baslik = "Tuvalete girerken"),
    LibItem("غُفْرَانَكَ", "Gufrâneke", "Allah'ım, bağışlamanı dilerim.", fazilet = "Hz. Peygamber tuvaletten çıkınca böyle derdi. (Ebû Dâvûd, Tahâret 17; Tirmizî, Tahâret 5)", target = 1, zikirOkunus = "Tuvaletten çıkınca", baslik = "Tuvaletten çıkınca"),
    LibItem("أَشْهَدُ أَنْ لَا إِلٰهَ إِلَّا اللّٰهُ وَحْدَهُ لَا شَرِيكَ لَهُ وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ", "Eşhedü en lâ ilâhe illallâhu vahdehû lâ şerîke leh, ve eşhedü enne Muhammeden abdühû ve rasûlüh", "Şahitlik ederim ki Allah'tan başka ilah yoktur, tektir, ortağı yoktur; yine şahitlik ederim ki Muhammed O'nun kulu ve elçisidir.", fazilet = "Güzelce abdest alıp bunu söyleyene cennetin sekiz kapısı açılır, dilediğinden girer. (Müslim, Tahâret 17)", target = 1, zikirOkunus = "Abdestten sonra", baslik = "Abdestten sonra"),
    LibItem("اَلْحَمْدُ لِلّٰهِ الَّذِي كَسَانِي هٰذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ", "Elhamdü lillâhillezî kesânî hâzâ ve razekanîhi min ğayri havlin minnî ve lâ kuvveh", "Benim hiçbir gücüm ve kuvvetim olmadan bunu bana giydiren ve rızık olarak veren Allah'a hamdolsun.", fazilet = "Yeni bir elbise giyip bunu söyleyenin geçmiş günahları bağışlanır. (Ebû Dâvûd, Libâs 1)", target = 1, zikirOkunus = "Elbise giyerken", baslik = "Elbise giyerken"),
    LibItem("اَللّٰهُمَّ أَنْتَ حَسَّنْتَ خَلْقِي فَحَسِّنْ خُلُقِي", "Allâhümme ente hassente halkī fe hassin hulukī", "Allah'ım, yaratılışımı güzel yaptın; ahlâkımı da güzelleştir.", fazilet = "Hz. Peygamber'in dualarındandır. (Ahmed b. Hanbel, Müsned I, 403)", target = 1, zikirOkunus = "Aynaya bakınca", baslik = "Aynaya bakınca"),
    LibItem("أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلّٰهِ وَالْحَمْدُ لِلّٰهِ لَا إِلٰهَ إِلَّا اللّٰهُ وَحْدَهُ لَا شَرِيكَ لَهُ لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلٰى كُلِّ شَيْءٍ قَدِيرٌ", "Asbahnâ ve asbaha'l-mülkü lillâh, ve'l-hamdü lillâh, lâ ilâhe illallâhu vahdehû lâ şerîke leh, lehü'l-mülkü ve lehü'l-hamdü ve hüve alâ külli şey'in kadîr", "Sabaha ulaştık, mülk de Allah'ın olarak sabaha ulaştı. Hamd Allah'adır. Allah'tan başka ilah yoktur, tektir, ortağı yoktur; mülk ve hamd O'nundur, O her şeye kadirdir.", fazilet = "Hz. Peygamber sabah ve akşam böyle derdi; akşam \"Emseynâ ve emse'l-mülkü lillâh\" diye başlardı. (Müslim, Zikir 75)", target = 1, zikirOkunus = "Sabah olunca", baslik = "Sabah olunca"),
    moved("Allâhümme ente rabbî"),
    moved("Bismillâhillezî lâ yedurru"),
    moved("Radîtü billâhi"),
    LibItem("حَسْبِيَ اللّٰهُ لَا إِلٰهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ", "Hasbiyallâhu lâ ilâhe illâ hû, aleyhi tevekkeltü ve hüve rabbü'l-arşi'l-azîm", "Allah bana yeter; O'ndan başka ilah yoktur. Yalnız O'na güvendim; O, büyük arşın Rabbidir.", fazilet = "Sabah ve akşam yedi defa söyleyene Allah, dünya ve âhiret sıkıntılarında yeter. (Tevbe 9/129; Ebû Dâvûd, Edeb 101)", target = 7, zikirOkunus = "Sabah ve akşam · 7 kez", baslik = "Sabah ve akşam · 7 kez"),
    LibItem("اَللّٰهُمَّ إِنِّي أَسْأَلُكَ الْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ", "Allâhümme innî es'elüke'l-âfiyete fi'd-dünyâ ve'l-âhireh", "Allah'ım, senden dünyada ve âhirette âfiyet dilerim.", fazilet = "Hz. Peygamber sabah ve akşam bu duayı hiç bırakmazdı. (Ebû Dâvûd, Edeb 101; İbn Mâce, Duâ 14)", target = 1, zikirOkunus = "Sabah ve akşam · âfiyet için", baslik = "Sabah ve akşam · âfiyet için"),
    LibItem("بِسْمِ اللّٰهِ تَوَكَّلْتُ عَلَى اللّٰهِ وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللّٰهِ", "Bismillâhi tevekkeltü ala'llâh, ve lâ havle ve lâ kuvvete illâ billâh", "Allah'ın adıyla; Allah'a güvendim. Güç ve kuvvet yalnızca Allah'tandır.", fazilet = "Bunu söyleyene \"Hidayete erdin, yeterli görüldün, korundun\" denir ve şeytan ondan uzaklaşır. (Ebû Dâvûd, Edeb 103; Tirmizî, Daavât 34)", target = 1, zikirOkunus = "Evden çıkarken", baslik = "Evden çıkarken"),
    LibItem("اَللّٰهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ", "Allâhümme'ftah lî ebvâbe rahmetik", "Allah'ım, bana rahmet kapılarını aç.", fazilet = "Hz. Peygamber mescide girerken bunu söylemeyi öğretti. (Müslim, Müsâfirîn 68)", target = 1, zikirOkunus = "Mescide girerken", baslik = "Mescide girerken"),
    LibItem("اَللّٰهُمَّ رَبَّ هٰذِهِ الدَّعْوَةِ التَّامَّةِ وَالصَّلَاةِ الْقَائِمَةِ آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ", "Allâhümme rabbe hâzihi'd-da'veti't-tâmmeh, ve's-salâti'l-kāimeh, âti Muhammedeni'l-vesîlete ve'l-fadîleh, veb'ashü makāmen mahmûdeni'llezî veadteh", "Ey bu eksiksiz davetin ve kılınacak namazın Rabbi Allah'ım! Muhammed'e vesîleyi ve fazileti ver; onu vaat ettiğin Makām-ı Mahmûd'a ulaştır.", fazilet = "Ezanı duyunca bunu okuyana kıyamet günü şefaatim helal olur. (Buhârî, Ezân 8)", target = 1, zikirOkunus = "Ezandan sonra", baslik = "Ezandan sonra"),
    moved("Allâhümme ente's-selâmu"),
    LibItem("اَللّٰهُمَّ أَعِنِّي عَلٰى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ", "Allâhümme einnî alâ zikrike ve şükrike ve husni ibâdetik", "Allah'ım, seni anmak, sana şükretmek ve sana güzelce ibadet etmek için bana yardım et.", fazilet = "Hz. Peygamber Muâz'a \"Seni seviyorum; her namazın sonunda bunu söylemeyi bırakma\" buyurdu. (Ebû Dâvûd, Vitir 26; Nesâî, Sehv 60)", target = 1, zikirOkunus = "Namazdan sonra", baslik = "Namazdan sonra"),
    LibItem("اَللّٰهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ", "Allâhümme innî es'elüke min fadlik", "Allah'ım, senden lütfunu dilerim.", fazilet = "Hz. Peygamber mescitten çıkarken bunu söylemeyi öğretti. (Müslim, Müsâfirîn 68)", target = 1, zikirOkunus = "Mescitten çıkarken", baslik = "Mescitten çıkarken"),
    LibItem("بِسْمِ اللّٰهِ · بِسْمِ اللّٰهِ أَوَّلَهُ وَآخِرَهُ", "Bismillâh · unutulursa: Bismillâhi evvelehû ve âhireh", "Allah'ın adıyla · Başında da sonunda da Allah'ın adıyla.", fazilet = "Yemeğe başlarken Allah'ın adını anın; başta unutan \"Bismillâhi evvelehû ve âhireh\" desin. (Buhârî, Et'ime 2; Ebû Dâvûd, Et'ime 15; Tirmizî, Et'ime 47)", target = 1, zikirOkunus = "Yemeğe başlarken", baslik = "Yemeğe başlarken"),
    LibItem("اَلْحَمْدُ لِلّٰهِ الَّذِي أَطْعَمَنِي هٰذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ", "Elhamdü lillâhillezî at'amenî hâzâ ve razekanîhi min ğayri havlin minnî ve lâ kuvveh", "Benim hiçbir gücüm ve kuvvetim olmadan bunu bana yediren ve rızık olarak veren Allah'a hamdolsun.", fazilet = "Yemekten sonra bunu söyleyenin geçmiş günahları bağışlanır. (Ebû Dâvûd, Libâs 1; Tirmizî, Daavât 56)", target = 1, zikirOkunus = "Yemekten sonra", baslik = "Yemekten sonra"),
    LibItem("بِسْمِ اللّٰهِ وَلَجْنَا وَبِسْمِ اللّٰهِ خَرَجْنَا وَعَلَى اللّٰهِ رَبِّنَا تَوَكَّلْنَا", "Bismillâhi velecnâ, ve bismillâhi haracnâ, ve ala'llâhi rabbinâ tevekkelnâ", "Allah'ın adıyla girdik, Allah'ın adıyla çıktık; Rabbimiz Allah'a güvendik.", fazilet = "Ardından ev halkına selam verilir. Eve girerken Allah'ı anan kişi için şeytan \"Burada size kalacak yer yok\" der. (Ebû Dâvûd, Edeb 103; Müslim, Eşribe 103)", target = 1, zikirOkunus = "Eve girerken", baslik = "Eve girerken"),
    LibItem("أَعُوذُ بِكَلِمَاتِ اللّٰهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ", "Eûzü bi kelimâti'llâhi't-tâmmâti min şerri mâ halak", "Allah'ın eksiksiz kelimelerine, yarattıklarının şerrinden sığınırım.", fazilet = "Bir yere konaklayıp bunu söyleyene, oradan ayrılıncaya kadar hiçbir şey zarar vermez. (Müslim, Zikir 54; Tirmizî, Daavât 13)", target = 3, zikirOkunus = "Akşam olunca · 3 kez", baslik = "Akşam olunca · 3 kez"),
    LibItem("اَللّٰهُمَّ أَسْلَمْتُ وَجْهِي إِلَيْكَ وَفَوَّضْتُ أَمْرِي إِلَيْكَ وَأَلْجَأْتُ ظَهْرِي إِلَيْكَ رَغْبَةً وَرَهْبَةً إِلَيْكَ لَا مَلْجَأَ وَلَا مَنْجَا مِنْكَ إِلَّا إِلَيْكَ آمَنْتُ بِكِتَابِكَ الَّذِي أَنْزَلْتَ وَبِنَبِيِّكَ الَّذِي أَرْسَلْتَ", "Allâhümme eslemtü vechî ileyk, ve fevvadtü emrî ileyk, ve elce'tü zahrî ileyk, rağbeten ve rahbeten ileyk, lâ melce'e ve lâ mencâ minke illâ ileyk, âmentü bi kitâbike'llezî enzelt, ve bi nebiyyike'llezî erselt", "Allah'ım, yüzümü sana çevirdim, işimi sana bıraktım, sırtımı sana dayadım; sana duyduğum arzu ve korkuyla. Senden kaçıp sığınılacak yer yine sensin. İndirdiğin kitaba ve gönderdiğin peygambere iman ettim.", fazilet = "Sağ yanına uzanıp bunu son söz olarak okuyan, o gece ölürse fıtrat üzere ölür. (Buhârî, Vudû 75; Müslim, Zikir 56)", target = 1, zikirOkunus = "Yatağa uzanınca", baslik = "Yatağa uzanınca"),
    LibItem("سُبْحَانَ اللّٰهِ ٣٣ · اَلْحَمْدُ لِلّٰهِ ٣٣ · اَللّٰهُ أَكْبَرُ ٣٤", "Sübhânallâh 33 · Elhamdülillâh 33 · Allâhu ekber 34", "Allah noksanlıklardan münezzehtir · Hamd Allah'adır · Allah en büyüktür.", fazilet = "Hz. Peygamber bunu Hz. Ali ve Hz. Fâtıma'ya \"size bir hizmetçiden daha hayırlıdır\" diyerek öğretti. (Buhârî, Nafakāt 6; Müslim, Zikir 80)", target = 100, zikirOkunus = "Yatmadan önce tesbihat", baslik = "Yatmadan önce tesbihat"),
    LibItem("بِاسْمِكَ اللّٰهُمَّ أَمُوتُ وَأَحْيَا", "Bismike'llâhümme emûtü ve ahyâ", "Allah'ım, senin adınla ölür ve dirilirim.", fazilet = "Hz. Peygamber yatağına girince böyle derdi. (Buhârî, Daavât 7)", target = 1, zikirOkunus = "Uyurken", baslik = "Uyurken"),
    moved("Rabbenâ âtinâ"),
    moved("Yâ Mukallibe'l-kulûb"),
    LibItem("اَللّٰهُمَّ إِنِّي أَسْأَلُكَ الْهُدٰى وَالتُّقٰى وَالْعَفَافَ وَالْغِنٰى", "Allâhümme innî es'elüke'l-hüdâ ve't-tükā ve'l-afâfe ve'l-ğınâ", "Allah'ım, senden hidayet, takva, iffet ve gönül zenginliği dilerim.", fazilet = "Hz. Peygamber'in sık yaptığı dualardandır. (Müslim, Zikir 72)", target = 1, zikirOkunus = "Hidayet ve iffet için", baslik = "Hidayet ve iffet için"),
    LibItem("لَا إِلٰهَ إِلَّا اللّٰهُ الْعَظِيمُ الْحَلِيمُ لَا إِلٰهَ إِلَّا اللّٰهُ رَبُّ الْعَرْشِ الْعَظِيمِ لَا إِلٰهَ إِلَّا اللّٰهُ رَبُّ السَّمَاوَاتِ وَرَبُّ الْأَرْضِ وَرَبُّ الْعَرْشِ الْكَرِيمِ", "Lâ ilâhe illallâhu'l-azîmu'l-halîm, lâ ilâhe illallâhu rabbü'l-arşi'l-azîm, lâ ilâhe illallâhu rabbü's-semâvâti ve rabbü'l-ardı ve rabbü'l-arşi'l-kerîm", "Azîm ve Halîm olan Allah'tan başka ilah yoktur. Büyük arşın Rabbi Allah'tan başka ilah yoktur. Göklerin, yerin ve değerli arşın Rabbi Allah'tan başka ilah yoktur.", fazilet = "Hz. Peygamber sıkıntı anında böyle dua ederdi. (Buhârî, Daavât 27; Müslim, Zikir 83)", target = 1, zikirOkunus = "Sıkıntı anında", baslik = "Sıkıntı anında"),
    moved("Yâ Hayyu yâ Kayyûmu"),
    moved("Lâ ilâhe illâ ente sübhâneke"),
    LibItem("اَللّٰهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ وَالْعَجْزِ وَالْكَسَلِ وَالْبُخْلِ وَالْجُبْنِ وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ", "Allâhümme innî eûzü bike mine'l-hemmi ve'l-hazen, ve'l-aczi ve'l-kesel, ve'l-buhli ve'l-cübn, ve dala'i'd-deyni ve ğalebeti'r-ricâl", "Allah'ım, kaygıdan ve hüzünden, acizlikten ve tembellikten, cimrilikten ve korkaklıktan, borç yükünden ve insanların baskısından sana sığınırım.", fazilet = "Hz. Peygamber bu duayı çok yapardı. (Buhârî, Daavât 36)", target = 1, zikirOkunus = "Keder, tembellik ve borçtan korunmak için", baslik = "Keder, tembellik ve borçtan korunmak için"),
    moved("Allâhümme inneke afüvvün"),
    LibItem("سُبْحَانَ الَّذِي سَخَّرَ لَنَا هٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلٰى رَبِّنَا لَمُنْقَلِبُونَ", "Sübhâne'llezî sahhara lenâ hâzâ ve mâ künnâ lehû mukrinîn, ve innâ ilâ rabbinâ le munkalibûn", "Bunu bizim hizmetimize veren Allah'ı tenzih ederiz; yoksa biz buna güç yetiremezdik. Şüphesiz biz Rabbimize döneceğiz.", fazilet = "Hz. Peygamber bineğine binip yola çıkarken böyle derdi. (Zuhruf 43/13-14; Müslim, Hac 425)", target = 1, zikirOkunus = "Yolculuğa çıkarken", baslik = "Yolculuğa çıkarken"),
    LibItem("أَعُوذُ بِاللّٰهِ مِنَ الشَّيْطَانِ الرَّجِيمِ", "Eûzü billâhi mine'ş-şeytâni'r-racîm", "Kovulmuş şeytanın şerrinden Allah'a sığınırım.", fazilet = "Öfkelenen kişi bunu söylerse öfkesi geçer. (Buhârî, Edeb 76; Müslim, Birr 109)", target = 1, zikirOkunus = "Öfkelenince", baslik = "Öfkelenince"),
    LibItem("اَلْحَمْدُ لِلّٰهِ · يَرْحَمُكَ اللّٰهُ · يَهْدِيكُمُ اللّٰهُ وَيُصْلِحُ بَالَكُمْ", "Elhamdülillâh · Yerhamükellâh · Yehdîkümullâhu ve yuslihu bâleküm", "Hapşıran: Hamd Allah'adır · Duyan: Allah sana merhamet etsin · Hapşıran: Allah sizi hidayete erdirsin ve hâlinizi düzeltsin.", fazilet = "(Buhârî, Edeb 126)", target = 1, zikirOkunus = "Hapşırınca", baslik = "Hapşırınca"),
    LibItem("اَللّٰهُمَّ صَيِّبًا نَافِعًا", "Allâhümme sayyiben nâfiâ", "Allah'ım, bunu faydalı bir yağmur kıl.", fazilet = "Hz. Peygamber yağmur yağınca böyle derdi. (Buhârî, İstiskā 23)", target = 1, zikirOkunus = "Yağmur yağınca", baslik = "Yağmur yağınca"),
    LibItem("لَا بَأْسَ طَهُورٌ إِنْ شَاءَ اللّٰهُ", "Lâ be'se tahûrun inşâallâh", "Geçmiş olsun; inşallah bu hastalık günahlarına temizlik olur.", fazilet = "Hz. Peygamber hasta ziyaret edince böyle derdi. (Buhârî, Menâkıb 25; Merdâ 10)", target = 1, zikirOkunus = "Hasta ziyaretinde", baslik = "Hasta ziyaretinde"),
    LibItem("إِنَّا لِلّٰهِ وَإِنَّا إِلَيْهِ رَاجِعُونَ اَللّٰهُمَّ أْجُرْنِي فِي مُصِيبَتِي وَأَخْلِفْ لِي خَيْرًا مِنْهَا", "İnnâ lillâhi ve innâ ileyhi râciûn, Allâhümme'cürnî fî musîbetî ve ahlif lî hayran minhâ", "Biz Allah'a aidiz ve O'na döneceğiz. Allah'ım, bu musibetimde bana ecir ver ve bana ondan daha hayırlısını ver.", fazilet = "Bunu söyleyene Allah, kaybettiğinden daha hayırlısını verir. (Müslim, Cenâiz 4)", target = 1, zikirOkunus = "Musibet anında", baslik = "Musibet anında"),
    LibItem("سُبْحَانَكَ اللّٰهُمَّ وَبِحَمْدِكَ أَشْهَدُ أَنْ لَا إِلٰهَ إِلَّا أَنْتَ أَسْتَغْفِرُكَ وَأَتُوبُ إِلَيْكَ", "Sübhânekellâhümme ve bihamdik, eşhedü en lâ ilâhe illâ ent, estağfiruke ve etûbü ileyk", "Allah'ım, seni hamdinle tenzih ederim. Senden başka ilah olmadığına şahitlik ederim. Senden bağışlanma diler, sana tövbe ederim.", fazilet = "Bir meclisten kalkarken bunu söyleyenin o meclisteki hataları bağışlanır. (Tirmizî, Daavât 39; Ebû Dâvûd, Edeb 27)", target = 1, zikirOkunus = "Meclisten kalkarken", baslik = "Meclisten kalkarken"),
    LibItem("اَلْحَمْدُ لِلّٰهِ الَّذِي بِنِعْمَتِهِ تَتِمُّ الصَّالِحَاتُ", "Elhamdü lillâhillezî bi ni'metihî tetimmü's-sâlihât", "İyilikler, nimetiyle tamamlanan Allah'a hamdolsun.", fazilet = "Hoşa gitmeyen bir şey görünce \"Elhamdü lillâhi alâ külli hâl\" derdi. (İbn Mâce, Edeb 55)", target = 1, zikirOkunus = "Sevindirici bir şey görünce", baslik = "Sevindirici bir şey görünce"),
    LibItem("اَللّٰهُمَّ أَهِلَّهُ عَلَيْنَا بِالْيُمْنِ وَالْإِيمَانِ وَالسَّلَامَةِ وَالْإِسْلَامِ رَبِّي وَرَبُّكَ اللّٰهُ", "Allâhümme ehillehû aleynâ bi'l-yümni ve'l-îmân, ve's-selâmeti ve'l-islâm, rabbî ve rabbüke'llâh", "Allah'ım, bu ayı bize bereket, iman, esenlik ve İslâm ile doğur. (Ey hilâl) benim de senin de Rabbin Allah'tır.", fazilet = "(Tirmizî, Daavât 50)", target = 1, zikirOkunus = "Yeni ayı (hilâli) görünce", baslik = "Yeni ayı (hilâli) görünce"),
    LibItem("اَلسَّلَامُ عَلَيْكُمْ أَهْلَ الدِّيَارِ مِنَ الْمُؤْمِنِينَ وَالْمُسْلِمِينَ وَإِنَّا إِنْ شَاءَ اللّٰهُ بِكُمْ لَلَاحِقُونَ أَسْأَلُ اللّٰهَ لَنَا وَلَكُمُ الْعَافِيَةَ", "Esselâmü aleyküm ehle'd-diyâri mine'l-mü'minîne ve'l-müslimîn, ve innâ inşâallâhu biküm le lâhikūn, es'elu'llâhe lenâ ve leküme'l-âfiyeh", "Selam size ey bu yurtların mümin ve müslüman sakinleri! İnşallah biz de size katılacağız. Allah'tan bizim ve sizin için âfiyet dilerim.", fazilet = "Hz. Peygamber kabristana gidenlere bunu söylemeyi öğretti. (Müslim, Cenâiz 104)", target = 1, zikirOkunus = "Kabir ziyaretinde", baslik = "Kabir ziyaretinde"),
)

/** Kaynağı belirtilmemiş maddelerin faziletleri (okunuş → fazilet). */
private val FZ_EK = mapOf(
    "Sübhânallâh" to "Günde yüz defa tesbih edene bin sevap yazılır ya da bin hatası silinir. (Müslim, Zikir 37) · Her namazdan sonra 33 Sübhânallâh, 33 Elhamdülillâh, 33 Allâhu ekber deyip yüzü tevhidle tamamlayanın günahları deniz köpüğü kadar olsa da bağışlanır. (Müslim, Mesâcid 146)",
    "Elhamdülillâh" to "\"Elhamdülillâh\" mizanı doldurur. (Müslim, Tahâret 1) · Duanın en faziletlisi \"Elhamdülillâh\"tır. (Tirmizî, Daavât 9)",
    "Allâhu Ekber" to "Sübhânallâh, elhamdülillâh, lâ ilâhe illallâh ve Allâhu ekber demek, üzerine güneşin doğduğu her şeyden bana daha sevimlidir. (Müslim, Zikir 32)",
    "Lâ ilâhe illallâh" to "Zikrin en faziletlisi \"Lâ ilâhe illallâh\"tır. (Tirmizî, Daavât 9; İbn Mâce, Edeb 55)",
    "Kelime-i Tevhîd" to "On defa söyleyen, İsmâiloğullarından dört köle azat etmiş gibi olur. (Buhârî, Daavât 64; Müslim, Zikir 30)",
    "Sübhânallâhi'l-azîm" to "İki kelime vardır; dile hafif, mizanda ağır, Rahmân'a sevimlidir: \"Sübhânallâhi ve bihamdihî, sübhânallâhi'l-azîm.\" (Buhârî, Daavât 65; Müslim, Zikir 31)",
    "Hasbünallâhu ve ni'me'l-vekîl" to "Hz. İbrâhim ateşe atılırken bunu söyledi; Hz. Peygamber ve ashabı da \"Düşman size karşı toplandı\" denildiğinde bunu söylediler. (Âl-i İmrân 3/173; Buhârî, Tefsîr, Âl-i İmrân 13)",
    "Estağfirullâh" to "Allah'a yemin olsun ki ben günde yetmiş defadan fazla Allah'tan bağışlanma diler, O'na tövbe ederim. (Buhârî, Daavât 3) · İstiğfara devam edene Allah her darlıktan bir çıkış, her kederden bir ferahlık verir ve onu ummadığı yerden rızıklandırır. (Ebû Dâvûd, Vitir 26)",
    "Estağfirullâhe'l-azîm ve etûbü ileyh" to "Ey insanlar, Allah'a tövbe edin; ben günde yüz defa tövbe ederim. (Müslim, Zikir 42)",
    "Salavât" to "Kim bana bir salavat getirirse Allah ona on rahmet eder. (Müslim, Salât 70)",
    "Allâhümme salli alâ seyyidinâ Muhammed" to "Kim bana bir salavat getirirse Allah ona on rahmet eder. (Müslim, Salât 70) · Allah ve melekleri Peygamber'e salât eder; ey iman edenler, siz de ona salât ve selam getirin. (Ahzâb 33/56)",
    "Allâhümme salli alâ Muhammedin ve alâ âli Muhammed" to "Ashab \"Sana nasıl salavat getirelim?\" diye sorunca Hz. Peygamber bu salavatı öğretti; namazda okunan salli-bârik dualarıdır. (Buhârî, Enbiyâ 10; Müslim, Salât 66)",
    "Sallallâhu aleyhi ve sellem" to "Yanında anıldığım hâlde bana salavat getirmeyen kişi cimridir. (Tirmizî, Daavât 100)",
    "Allâhümme salli ve sellim alâ seyyidinâ Muhammed" to "Kıyamet günü insanların bana en yakın olanı, bana en çok salavat getirenidir. (Tirmizî, Vitir 21)",
)

private fun LibItem.withFz(): LibItem = if (fazilet.isNotBlank()) this else copy(fazilet = FZ_EK[okunus] ?: FZ_EK[zikirOkunus] ?: "")

/** Esmâ-ül Hüsnâ: her isimle hangi hâlde dua edildiği. */
private val ESMA_FZ = listOf(
    "Bütün isimleri kendinde toplayan isimdir; her duanın başında ve sonunda anılır. (Tâhâ 20/8)",
    "Merhamet ve rahmet dilerken \"Yâ Rahmân\" diye dua edilir.",
    "Âhirette bağışlanma ve rahmet umarken \"Yâ Rahîm\" diye dua edilir.",
    "Bütün işlerin O'nun elinde olduğunu bilip, kimseye muhtaç olmamak için \"Yâ Melik\" diye dua edilir.",
    "Kalbin ve amellerin arınması için \"Yâ Kuddûs\" diye dua edilir. Hz. Peygamber vitirden sonra \"Sübhâne'l-meliki'l-kuddûs\" derdi (kaynağıyla Sünnet bölümünde).",
    "Huzur, esenlik ve selamet dilerken \"Yâ Selâm\" diye dua edilir.",
    "Korku ve endişe anında güven dilerken \"Yâ Mü'min\" diye dua edilir.",
    "Kendini, aileni ve emanetleri korumasına bırakırken \"Yâ Müheymin\" diye dua edilir.",
    "İzzet ve onur dilerken, zillete düşmekten sığınırken \"Yâ Azîz\" diye dua edilir.",
    "Kırılmış gönüllerin onarılması ve işlerin düzelmesi için \"Yâ Cebbâr\" diye dua edilir.",
    "Kibirden korunmak ve tevazu istemek için \"Yâ Mütekebbir\" diye anılır.",
    "Yeni bir işe başlarken hayırlısının yaratılması için \"Yâ Hâlık\" diye dua edilir.",
    "Kusur ve ayıplardan korunmak için \"Yâ Bâri'\" diye dua edilir.",
    "Hamilelikte, sağlıklı ve güzel bir yaratılış için \"Yâ Musavvir\" diye dua edilir.",
    "Günahların örtülüp bağışlanması için \"Yâ Gaffâr\" diye dua edilir. (Nûh 71/10)",
    "Nefsin ve zalimlerin şerrinden korunmak için \"Yâ Kahhâr\" diye dua edilir.",
    "Hayırlı evlat, ilim ve rahmet dilerken \"Yâ Vehhâb\" diye dua edilir. (Âl-i İmrân 3/8)",
    "Helal ve bereketli rızık dilerken \"Yâ Rezzâk\" diye dua edilir.",
    "Kapalı kapıların ve hayır yollarının açılması için \"Yâ Fettâh\" diye dua edilir.",
    "Faydalı ilim dilerken \"Yâ Alîm\" diye dua edilir.",
    "Darlık geldiğinde sabır ve hikmetini anlamak için \"Yâ Kâbıd\" diye anılır.",
    "Gönül ferahlığı ve rızık genişliği için \"Yâ Bâsıt\" diye dua edilir.",
    "Kibirden ve zalimin üstünlüğünden korunmak için \"Yâ Hâfıd\" diye anılır.",
    "Derecenin yükselmesi ve hayırlı makam için \"Yâ Râfi'\" diye dua edilir.",
    "Dünyada ve âhirette izzet dilerken \"Yâ Muiz\" diye dua edilir. (Âl-i İmrân 3/26)",
    "Zillete düşmekten sığınırken \"Yâ Müzil\" diye anılır.",
    "Duasının işitilmesini dileyen \"Yâ Semî'\" diye niyaz eder.",
    "Gizli ve açık hâlini Allah'ın gördüğünü bilerek \"Yâ Basîr\" diye dua edilir.",
    "Anlaşmazlıklarda doğru hükme ulaşmak için \"Yâ Hakem\" diye dua edilir.",
    "Haksızlığa uğrayan, adalet dilerken \"Yâ Adl\" diye dua edilir.",
    "İşlerin kolaylaşması ve lütuf için \"Yâ Latîf\" diye dua edilir.",
    "Gizli kalmış hayrı bilmek ve doğru kararı vermek için \"Yâ Habîr\" diye dua edilir.",
    "Öfkeye karşı yumuşaklık ve sabır dilerken \"Yâ Halîm\" diye dua edilir.",
    "Büyük sıkıntılar karşısında \"Yâ Azîm\" diye dua edilir.",
    "Günahların bağışlanması için \"Yâ Gafûr\" diye dua edilir.",
    "Şükreden bir kalp ve amellerin kabulü için \"Yâ Şekûr\" diye dua edilir.",
    "Himmet ve gayretin yükselmesi için \"Yâ Aliyy\" diye dua edilir.",
    "Dertlerin küçülüp Allah'ın büyüklüğünün kalbe yerleşmesi için \"Yâ Kebîr\" diye dua edilir.",
    "Her türlü tehlikeden korunmak için \"Yâ Hafîz\" diye dua edilir. (Yûsuf 12/64)",
    "Bedenin ve kalbin gıdası için \"Yâ Mukît\" diye dua edilir.",
    "Hesap gününe hazırlık ve Allah'ın yeterliliği için \"Yâ Hasîb\" diye anılır.",
    "Heybet ve vakar dilerken \"Yâ Celîl\" diye dua edilir.",
    "İkram ve cömertlik dilerken \"Yâ Kerîm\" diye dua edilir.",
    "Allah'ın her an gözettiğini bilerek nefsi korumak için \"Yâ Rakîb\" diye anılır.",
    "Duaların kabulü için \"Yâ Mucîb\" diye dua edilir. (Hûd 11/61)",
    "Rızkın ve gönlün genişlemesi için \"Yâ Vâsi'\" diye dua edilir.",
    "Hikmet ve doğru söz dilerken \"Yâ Hakîm\" diye dua edilir.",
    "Allah'ın sevgisini ve insanlar arasında muhabbet dilerken \"Yâ Vedûd\" diye dua edilir.",
    "Şeref ve hayırlı ün dilerken \"Yâ Mecîd\" diye dua edilir.",
    "Ölü kalplerin dirilmesi ve âhirete hazırlık için \"Yâ Bâis\" diye anılır.",
    "Doğruluğa şahit olunması ve ihlas için \"Yâ Şehîd\" diye dua edilir.",
    "Hakkı hak olarak görüp ona uymak için \"Yâ Hakk\" diye dua edilir.",
    "İşlerini Allah'a bırakan \"Yâ Vekîl\" diye dua eder. (Âl-i İmrân 3/173)",
    "Zayıflık ve güçsüzlük anında \"Yâ Kaviyy\" diye dua edilir.",
    "Kararlılık ve sebat dilerken \"Yâ Metîn\" diye dua edilir.",
    "Allah'ın dostluğunu ve yardımını dilerken \"Yâ Veliyy\" diye dua edilir.",
    "Övülecek ahlâk ve hamd edilecek işler için \"Yâ Hamîd\" diye dua edilir.",
    "Hiçbir amelin boşa gitmemesi için \"Yâ Muhsî\" diye anılır.",
    "Yeni ve hayırlı başlangıçlar için \"Yâ Mübdi'\" diye dua edilir.",
    "Kaybedilen hayrın geri dönmesi için \"Yâ Muîd\" diye dua edilir.",
    "Kalbin ve bedenin dirilmesi, şifa için \"Yâ Muhyî\" diye dua edilir.",
    "Güzel bir son (hüsn-i hâtime) dilerken \"Yâ Mümît\" diye anılır.",
    "Ölü kalbin dirilmesi için \"Yâ Hayy\" diye dua edilir. Hz. Peygamber sıkıntıda \"Yâ Hayyu yâ Kayyûm\" derdi. (Tirmizî, Daavât 91)",
    "İşlerin ayakta kalması ve düzene girmesi için \"Yâ Kayyûm\" diye dua edilir.",
    "Aranılan hayrın bulunması için \"Yâ Vâcid\" diye dua edilir.",
    "Şerefli bir hayat dilerken \"Yâ Mâcid\" diye dua edilir.",
    "Kalbin dağınıklıktan kurtulup birlenmesi için \"Yâ Vâhid\" diye anılır.",
    "Hiç kimseye muhtaç olmamak için \"Yâ Samed\" diye dua edilir. (İhlâs 112/2)",
    "İmkânsız görünen işler için \"Yâ Kâdir\" diye dua edilir.",
    "Güç yetiremediği işlerde \"Yâ Muktedir\" diye dua edilir.",
    "Hayırlı işlerde öne geçmek için \"Yâ Mukaddim\" diye dua edilir.",
    "Şerrin ertelenip uzaklaşması için \"Yâ Muahhir\" diye dua edilir.",
    "Her işin başında Allah'ı anmak için \"Yâ Evvel\" diye anılır. (Hadîd 57/3)",
    "Güzel bir son dilerken \"Yâ Âhir\" diye dua edilir.",
    "Hakkın açığa çıkması için \"Yâ Zâhir\" diye dua edilir.",
    "Gizli sırların bilinmesi ve iç dünyanın düzelmesi için \"Yâ Bâtın\" diye dua edilir.",
    "İşlerin güzelce yönetilmesi için \"Yâ Vâlî\" diye dua edilir.",
    "Kalbin yücelmesi ve aşağılık hâllerden korunmak için \"Yâ Müteâlî\" diye anılır.",
    "İyilik ve ihsan dilerken \"Yâ Berr\" diye dua edilir.",
    "Tövbenin kabulü için \"Yâ Tevvâb\" diye dua edilir. (Bakara 2/128)",
    "Zalimin zulmünden korunmak için \"Yâ Müntakim\" diye anılır.",
    "Af dilerken \"Yâ Afüvv\" diye dua edilir. (Tirmizî, Daavât 84)",
    "Şefkat ve merhamet dilerken \"Yâ Raûf\" diye dua edilir.",
    "Her şeyin asıl sahibine sığınırken \"Yâ Mâlike'l-mülk\" diye dua edilir. (Âl-i İmrân 3/26)",
    "Dualarda çokça tekrar edin; Hz. Peygamber \"Yâ Ze'l-celâli ve'l-ikrâm\" sözüne devam edin buyurdu. (Tirmizî, Daavât 91)",
    "Adalet ve hakkaniyet dilerken \"Yâ Muksit\" diye dua edilir.",
    "Dağılan ailenin, kaybolan şeyin bir araya gelmesi için \"Yâ Câmi'\" diye dua edilir.",
    "Gönül zenginliği dilerken \"Yâ Ganiyy\" diye dua edilir.",
    "Fakirlikten kurtulmak için \"Yâ Muğnî\" diye dua edilir.",
    "Kötülüklerin engellenmesi için \"Yâ Mâni'\" diye dua edilir.",
    "Zarar ve fayda O'ndandır; zarardan korunmak için \"Yâ Dârr\" diye anılır.",
    "Fayda ve şifa dilerken \"Yâ Nâfi'\" diye dua edilir.",
    "Kalbin ve yolun aydınlanması için \"Yâ Nûr\" diye dua edilir. (Nûr 24/35)",
    "Kendisi ve sevdikleri için hidayet dilerken \"Yâ Hâdî\" diye dua edilir.",
    "Güzel ve özgün işler için \"Yâ Bedî'\" diye dua edilir.",
    "Kalıcı iyilikler ve sevaplar için \"Yâ Bâkî\" diye dua edilir.",
    "Ardında hayırlı bir miras bırakmak için \"Yâ Vâris\" diye dua edilir.",
    "Doğru yolu bulmak ve isabetli karar için \"Yâ Reşîd\" diye dua edilir.",
    "Sabır dilerken \"Yâ Sabûr\" diye dua edilir.",
)

private val GUNLUK_RAW = listOf(
    LibItem("سُبْحَانَ اللّٰهِ", "Sübhânallâh", "Allah her türlü noksanlıktan münezzehtir"),
    LibItem("اَلْحَمْدُ لِلّٰهِ", "Elhamdülillâh", "Hamd yalnızca Allah'adır"),
    LibItem("اَللّٰهُ اَكْبَرُ", "Allâhu Ekber", "Allah en büyüktür"),
    LibItem("لَا اِلٰهَ اِلَّا اللّٰهُ", "Lâ ilâhe illallâh", "Allah'tan başka ilah yoktur", target = 100),
    LibItem(TEVHID_AR, TEVHID_TR, "Allah'tan başka ilah yoktur; tektir, ortağı yoktur. Mülk ve hamd O'nundur, O her şeye kadirdir.", target = 10, zikirOkunus = "Kelime-i Tevhîd"),
    LibItem(
        "سُبْحَانَ اللّٰهِ وَبِحَمْدِهِ", "Sübhânallâhi ve bihamdihî", "Allah'ı hamdiyle tesbih ederim",
        "Buhârî ve Müslim'in rivayet ettiği hadise göre günde yüz defa söyleyenin hataları, deniz köpüğü kadar olsa bile silinir.", 100,
    ),
    LibItem("سُبْحَانَ اللّٰهِ الْعَظِيمِ", "Sübhânallâhi'l-azîm", "Yüce Allah'ı tesbih ederim"),
    LibItem(
        "لَا حَوْلَ وَلَا قُوَّةَ اِلَّا بِاللّٰهِ", "Lâ havle ve lâ kuvvete illâ billâh", "Güç ve kuvvet ancak Allah'ın yardımıyladır",
        "Hadiste cennet hazinelerinden bir hazine olarak bildirilmiştir (Buhârî, Müslim).",
    ),
    LibItem("حَسْبُنَا اللّٰهُ وَنِعْمَ الْوَكِيلُ", "Hasbünallâhu ve ni'me'l-vekîl", "Allah bize yeter, O ne güzel vekildir"),
    LibItem("اَسْتَغْفِرُ اللّٰهَ", "Estağfirullâh", "Allah'tan bağışlanma dilerim", target = 100),
    LibItem("اَسْتَغْفِرُ اللّٰهَ الْعَظِيمَ وَاَتُوبُ اِلَيْهِ", "Estağfirullâhe'l-azîm ve etûbü ileyh", "Yüce Allah'tan bağışlanma diler, O'na tövbe ederim", target = 100),
    LibItem(
        "لَا اِلٰهَ اِلَّا اَنْتَ سُبْحَانَكَ اِنِّي كُنْتُ مِنَ الظَّالِمِينَ",
        "Lâ ilâhe illâ ente sübhâneke innî küntü mine'z-zâlimîn",
        "Senden başka ilah yoktur, Seni tesbih ederim; ben zalimlerden oldum",
        "Hz. Yûnus'un duası (Enbiyâ, 21/87).", 100, "Yûnus Duası",
    ),
)

private val SALAVAT_RAW = listOf(
    LibItem("اَللّٰهُمَّ صَلِّ عَلٰى سَيِّدِنَا مُحَمَّدٍ", "Allâhümme salli alâ seyyidinâ Muhammed", "Allah'ım, Efendimiz Muhammed'e salât eyle", target = 100, zikirOkunus = "Salavât"),
    LibItem(
        "اَللّٰهُمَّ صَلِّ عَلٰى مُحَمَّدٍ وَعَلٰى اٰلِ مُحَمَّدٍ",
        "Allâhümme salli alâ Muhammedin ve alâ âli Muhammed",
        "Allah'ım, Muhammed'e ve onun âline salât eyle", target = 100, zikirOkunus = "Salli Salavâtı",
    ),
    LibItem("صَلَّى اللّٰهُ عَلَيْهِ وَسَلَّمَ", "Sallallâhu aleyhi ve sellem", "Allah'ın salât ve selâmı onun üzerine olsun", target = 100),
    LibItem(
        "اَللّٰهُمَّ صَلِّ وَسَلِّمْ عَلٰى سَيِّدِنَا مُحَمَّدٍ",
        "Allâhümme salli ve sellim alâ seyyidinâ Muhammed",
        "Allah'ım, Efendimiz Muhammed'e salât ve selâm eyle", target = 100, zikirOkunus = "Salavât-ı Şerîfe",
    ),
)

// Esmâ-ül Hüsnâ, Tirmizî sıralaması: (Arapça, isim, zikir hâli, anlam)
val LIB_GUNLUK: List<LibItem> by lazy { GUNLUK_RAW.map { it.withFz() } }
val LIB_SALAVAT: List<LibItem> by lazy { SALAVAT_RAW.map { it.withFz() } }

private val ESMA_RAW = listOf(
    arrayOf("اَللّٰهُ", "Allah", "Allah", "Eşi benzeri olmayan, bütün noksan sıfatlardan münezzeh tek ilah"),
    arrayOf("اَلرَّحْمٰنُ", "Er-Rahmân", "Yâ Rahmân", "Dünyada bütün yaratılmışlara merhamet eden"),
    arrayOf("اَلرَّحِيمُ", "Er-Rahîm", "Yâ Rahîm", "Âhirette müminlere merhamet eden"),
    arrayOf("اَلْمَلِكُ", "El-Melik", "Yâ Melik", "Mülkün ve kâinatın sahibi"),
    arrayOf("اَلْقُدُّوسُ", "El-Kuddûs", "Yâ Kuddûs", "Her türlü eksiklikten uzak"),
    arrayOf("اَلسَّلَامُ", "Es-Selâm", "Yâ Selâm", "Esenlik veren"),
    arrayOf("اَلْمُؤْمِنُ", "El-Mü'min", "Yâ Mü'min", "Güven veren"),
    arrayOf("اَلْمُهَيْمِنُ", "El-Müheymin", "Yâ Müheymin", "Gözetip koruyan"),
    arrayOf("اَلْعَزِيزُ", "El-Azîz", "Yâ Azîz", "İzzet sahibi, her şeye galip"),
    arrayOf("اَلْجَبَّارُ", "El-Cebbâr", "Yâ Cebbâr", "Dilediğini yaptırmaya gücü yeten"),
    arrayOf("اَلْمُتَكَبِّرُ", "El-Mütekebbir", "Yâ Mütekebbir", "Büyüklükte eşi olmayan"),
    arrayOf("اَلْخَالِقُ", "El-Hâlık", "Yâ Hâlık", "Yoktan var eden"),
    arrayOf("اَلْبَارِئُ", "El-Bâri'", "Yâ Bâri'", "Her şeyi kusursuz yaratan"),
    arrayOf("اَلْمُصَوِّرُ", "El-Musavvir", "Yâ Musavvir", "Her şeye şekil veren"),
    arrayOf("اَلْغَفَّارُ", "El-Gaffâr", "Yâ Gaffâr", "Günahları örtüp bağışlayan"),
    arrayOf("اَلْقَهَّارُ", "El-Kahhâr", "Yâ Kahhâr", "Her şeye galip gelen"),
    arrayOf("اَلْوَهَّابُ", "El-Vehhâb", "Yâ Vehhâb", "Karşılıksız bol bol veren"),
    arrayOf("اَلرَّزَّاقُ", "Er-Rezzâk", "Yâ Rezzâk", "Rızık veren"),
    arrayOf("اَلْفَتَّاحُ", "El-Fettâh", "Yâ Fettâh", "Hayır kapılarını açan"),
    arrayOf("اَلْعَلِيمُ", "El-Alîm", "Yâ Alîm", "Her şeyi bilen"),
    arrayOf("اَلْقَابِضُ", "El-Kâbıd", "Yâ Kâbıd", "Dilediğine darlık veren"),
    arrayOf("اَلْبَاسِطُ", "El-Bâsıt", "Yâ Bâsıt", "Dilediğine bolluk veren"),
    arrayOf("اَلْخَافِضُ", "El-Hâfıd", "Yâ Hâfıd", "Dilediğini alçaltan"),
    arrayOf("اَلرَّافِعُ", "Er-Râfi'", "Yâ Râfi'", "Dilediğini yükselten"),
    arrayOf("اَلْمُعِزُّ", "El-Muiz", "Yâ Muiz", "Dilediğine izzet veren"),
    arrayOf("اَلْمُذِلُّ", "El-Müzil", "Yâ Müzil", "Dilediğini zillete düşüren"),
    arrayOf("اَلسَّمِيعُ", "Es-Semî'", "Yâ Semî'", "Her şeyi işiten"),
    arrayOf("اَلْبَصِيرُ", "El-Basîr", "Yâ Basîr", "Her şeyi gören"),
    arrayOf("اَلْحَكَمُ", "El-Hakem", "Yâ Hakem", "Mutlak hüküm sahibi"),
    arrayOf("اَلْعَدْلُ", "El-Adl", "Yâ Adl", "Mutlak adalet sahibi"),
    arrayOf("اَللَّطِيفُ", "El-Latîf", "Yâ Latîf", "Lütuf sahibi, en ince işleri bilen"),
    arrayOf("اَلْخَبِيرُ", "El-Habîr", "Yâ Habîr", "Her şeyden haberdar olan"),
    arrayOf("اَلْحَلِيمُ", "El-Halîm", "Yâ Halîm", "Cezada acele etmeyen, yumuşak davranan"),
    arrayOf("اَلْعَظِيمُ", "El-Azîm", "Yâ Azîm", "Azameti sonsuz olan"),
    arrayOf("اَلْغَفُورُ", "El-Gafûr", "Yâ Gafûr", "Çok bağışlayan"),
    arrayOf("اَلشَّكُورُ", "Eş-Şekûr", "Yâ Şekûr", "Az amele çok mükâfat veren"),
    arrayOf("اَلْعَلِيُّ", "El-Aliyy", "Yâ Aliyy", "Yüceler yücesi"),
    arrayOf("اَلْكَبِيرُ", "El-Kebîr", "Yâ Kebîr", "Pek büyük"),
    arrayOf("اَلْحَفِيظُ", "El-Hafîz", "Yâ Hafîz", "Her şeyi koruyan"),
    arrayOf("اَلْمُقِيتُ", "El-Mukît", "Yâ Mukît", "Her canlının gıdasını veren"),
    arrayOf("اَلْحَسِيبُ", "El-Hasîb", "Yâ Hasîb", "Hesaba çeken"),
    arrayOf("اَلْجَلِيلُ", "El-Celîl", "Yâ Celîl", "Celâl ve azamet sahibi"),
    arrayOf("اَلْكَرِيمُ", "El-Kerîm", "Yâ Kerîm", "Keremi, ikramı bol"),
    arrayOf("اَلرَّقِيبُ", "Er-Rakîb", "Yâ Rakîb", "Her an gözetleyen"),
    arrayOf("اَلْمُجِيبُ", "El-Mucîb", "Yâ Mucîb", "Duaları kabul eden"),
    arrayOf("اَلْوَاسِعُ", "El-Vâsi'", "Yâ Vâsi'", "İlmi ve rahmeti her şeyi kuşatan"),
    arrayOf("اَلْحَكِيمُ", "El-Hakîm", "Yâ Hakîm", "Her işi hikmetli olan"),
    arrayOf("اَلْوَدُودُ", "El-Vedûd", "Yâ Vedûd", "Kullarını seven, sevilmeye layık olan"),
    arrayOf("اَلْمَجِيدُ", "El-Mecîd", "Yâ Mecîd", "Şanı yüce"),
    arrayOf("اَلْبَاعِثُ", "El-Bâis", "Yâ Bâis", "Ölüleri dirilten"),
    arrayOf("اَلشَّهِيدُ", "Eş-Şehîd", "Yâ Şehîd", "Her yerde hazır ve nâzır"),
    arrayOf("اَلْحَقُّ", "El-Hakk", "Yâ Hakk", "Varlığı hiç değişmeden duran, gerçek"),
    arrayOf("اَلْوَكِيلُ", "El-Vekîl", "Yâ Vekîl", "Kendisine güvenilen"),
    arrayOf("اَلْقَوِيُّ", "El-Kaviyy", "Yâ Kaviyy", "Kudretli, güçlü"),
    arrayOf("اَلْمَتِينُ", "El-Metîn", "Yâ Metîn", "Çok sağlam"),
    arrayOf("اَلْوَلِيُّ", "El-Veliyy", "Yâ Veliyy", "Müminlerin dostu"),
    arrayOf("اَلْحَمِيدُ", "El-Hamîd", "Yâ Hamîd", "Övülmeye layık"),
    arrayOf("اَلْمُحْصِي", "El-Muhsî", "Yâ Muhsî", "Her şeyi tek tek bilen"),
    arrayOf("اَلْمُبْدِئُ", "El-Mübdi'", "Yâ Mübdi'", "Örneksiz yaratan"),
    arrayOf("اَلْمُعِيدُ", "El-Muîd", "Yâ Muîd", "Yeniden yaratan"),
    arrayOf("اَلْمُحْيِي", "El-Muhyî", "Yâ Muhyî", "Can veren"),
    arrayOf("اَلْمُمِيتُ", "El-Mümît", "Yâ Mümît", "Ölümü yaratan"),
    arrayOf("اَلْحَيُّ", "El-Hayy", "Yâ Hayy", "Ezelî ve ebedî diri"),
    arrayOf("اَلْقَيُّومُ", "El-Kayyûm", "Yâ Kayyûm", "Her şeyi ayakta tutan"),
    arrayOf("اَلْوَاجِدُ", "El-Vâcid", "Yâ Vâcid", "Dilediğini dilediği an bulan"),
    arrayOf("اَلْمَاجِدُ", "El-Mâcid", "Yâ Mâcid", "Kadri ve şanı büyük"),
    arrayOf("اَلْوَاحِدُ", "El-Vâhid", "Yâ Vâhid", "Tek, eşsiz"),
    arrayOf("اَلصَّمَدُ", "Es-Samed", "Yâ Samed", "Her şeyin kendisine muhtaç olduğu"),
    arrayOf("اَلْقَادِرُ", "El-Kâdir", "Yâ Kâdir", "Her şeye gücü yeten"),
    arrayOf("اَلْمُقْتَدِرُ", "El-Muktedir", "Yâ Muktedir", "Kudret sahibi"),
    arrayOf("اَلْمُقَدِّمُ", "El-Mukaddim", "Yâ Mukaddim", "Dilediğini öne alan"),
    arrayOf("اَلْمُؤَخِّرُ", "El-Muahhir", "Yâ Muahhir", "Dilediğini geri bırakan"),
    arrayOf("اَلْاَوَّلُ", "El-Evvel", "Yâ Evvel", "Başlangıcı olmayan"),
    arrayOf("اَلْاٰخِرُ", "El-Âhir", "Yâ Âhir", "Sonu olmayan"),
    arrayOf("اَلظَّاهِرُ", "Ez-Zâhir", "Yâ Zâhir", "Varlığı apaçık"),
    arrayOf("اَلْبَاطِنُ", "El-Bâtın", "Yâ Bâtın", "Akılların idrakinden gizli"),
    arrayOf("اَلْوَالِي", "El-Vâlî", "Yâ Vâlî", "Kâinatı yöneten"),
    arrayOf("اَلْمُتَعَالِي", "El-Müteâlî", "Yâ Müteâlî", "Yaratılmışların sıfatlarından yüce olan"),
    arrayOf("اَلْبَرُّ", "El-Berr", "Yâ Berr", "İyiliği bol"),
    arrayOf("اَلتَّوَّابُ", "Et-Tevvâb", "Yâ Tevvâb", "Tövbeleri kabul eden"),
    arrayOf("اَلْمُنْتَقِمُ", "El-Müntakim", "Yâ Müntakim", "Zalimleri cezalandıran"),
    arrayOf("اَلْعَفُوُّ", "El-Afüvv", "Yâ Afüvv", "Affı çok olan"),
    arrayOf("اَلرَّؤُفُ", "Er-Raûf", "Yâ Raûf", "Çok şefkatli"),
    arrayOf("مَالِكُ الْمُلْكِ", "Mâlikü'l-Mülk", "Yâ Mâlike'l-Mülk", "Mülkün ebedî sahibi"),
    arrayOf("ذُو الْجَلَالِ وَالْاِكْرَامِ", "Zü'l-Celâli ve'l-İkrâm", "Yâ Ze'l-Celâli ve'l-İkrâm", "Celâl ve ikram sahibi"),
    arrayOf("اَلْمُقْسِطُ", "El-Muksit", "Yâ Muksit", "Adaletle hükmeden"),
    arrayOf("اَلْجَامِعُ", "El-Câmi'", "Yâ Câmi'", "Dilediğini bir araya getiren"),
    arrayOf("اَلْغَنِيُّ", "El-Ganiyy", "Yâ Ganiyy", "Zengin, hiçbir şeye muhtaç olmayan"),
    arrayOf("اَلْمُغْنِي", "El-Muğnî", "Yâ Muğnî", "Dilediğini zengin eden"),
    arrayOf("اَلْمَانِعُ", "El-Mâni'", "Yâ Mâni'", "Dilemediği şeye engel olan"),
    arrayOf("اَلضَّارُّ", "Ed-Dârr", "Yâ Dârr", "Elem ve zarar verenleri yaratan"),
    arrayOf("اَلنَّافِعُ", "En-Nâfi'", "Yâ Nâfi'", "Fayda veren"),
    arrayOf("اَلنُّورُ", "En-Nûr", "Yâ Nûr", "Âlemleri nurlandıran"),
    arrayOf("اَلْهَادِي", "El-Hâdî", "Yâ Hâdî", "Hidayet veren"),
    arrayOf("اَلْبَدِيعُ", "El-Bedî'", "Yâ Bedî'", "Eşsiz yaratan"),
    arrayOf("اَلْبَاقِي", "El-Bâkî", "Yâ Bâkî", "Varlığı sonsuz"),
    arrayOf("اَلْوَارِثُ", "El-Vâris", "Yâ Vâris", "Her şeyin asıl sahibi"),
    arrayOf("اَلرَّشِيدُ", "Er-Reşîd", "Yâ Reşîd", "Doğru yolu gösteren"),
    arrayOf("اَلصَّبُورُ", "Es-Sabûr", "Yâ Sabûr", "Çok sabırlı"),
)

val LIB_ESMA: List<LibItem> = ESMA_RAW.mapIndexed { i, a ->
    LibItem(a[0], "${i + 1}. ${a[1]}", a[3], fazilet = ESMA_FZ[i], target = 33, zikirOkunus = a[2])
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
                bead = o.optInt("bead", 0).coerceIn(0, BEADS.lastIndex),
                lang = o.optInt("lang", 0).coerceIn(0, LANGS.size),
                haptic = o.optInt("haptic", 2).coerceIn(0, 3),
                // Ses özelliği yenilendi: eski kayıtlarda (soundType yok) varsayılan olarak aç
                sound = if (o.has("soundType")) o.optBoolean("sound", true) else true,
                soundType = o.optInt("soundType", 0).coerceIn(0, 2),
                soundVol = o.optInt("soundVol", 1).coerceIn(0, 2),
                slideSound = o.optBoolean("slideSound", false),
                beadShape = o.optInt("beadShape", 0).coerceIn(0, 1),
                imame = o.optInt("imame", 0).coerceIn(0, 1),
                cord = o.optInt("cord", 0).coerceIn(0, 2),
                volumeKeys = o.optBoolean("volumeKeys", true),
                stopAtTarget = o.optBoolean("stopAtTarget", false),
                guardMs = o.optInt("guardMs", 70).coerceIn(0, 1000),
                fontScale = o.optDouble("fontScale", 1.0).toFloat().coerceIn(0.85f, 1.4f),
                milestone = o.optInt("milestone", 33).coerceIn(0, 100_000),
                dailyGoal = o.optInt("dailyGoal", 0).coerceIn(0, 100_000),
                remMorning = o.optBoolean("remMorning", false),
                morningMin = o.optInt("morningMin", 7 * 60).coerceIn(0, 1439),
                remEvening = o.optBoolean("remEvening", false),
                eveningMin = o.optInt("eveningMin", 18 * 60 + 30).coerceIn(0, 1439),
            )
        }.getOrDefault(Settings())
    }

    fun saveSettings(s: Settings) {
        val o = JSONObject()
            .put("theme", s.theme).put("bead", s.bead).put("lang", s.lang).put("haptic", s.haptic).put("sound", s.sound).put("soundType", s.soundType).put("soundVol", s.soundVol).put("slideSound", s.slideSound).put("beadShape", s.beadShape).put("imame", s.imame).put("cord", s.cord)
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

    fun loadLaps(): Map<String, Map<String, Int>> {
        val s = p.getString("laps", null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(s)
            o.keys().asSequence().associateWith { d ->
                val m = o.getJSONObject(d)
                m.keys().asSequence().associateWith { m.optInt(it, 0) }.filterValues { it > 0 }
            }
        }.getOrDefault(emptyMap())
    }

    fun saveLaps(l: Map<String, Map<String, Int>>) {
        val o = JSONObject()
        l.forEach { (d, m) -> if (m.isNotEmpty()) o.put(d, JSONObject().apply { m.forEach { (k, v) -> put(k, v) } }) }
        p.edit().putString("laps", o.toString()).apply()
    }

    fun loadVirds(): List<Vird> {
        val list = p.getString("virds", null)?.let { s -> runCatching { virdsFrom(JSONArray(s)) }.getOrNull() } ?: VARSAYILAN_VIRDLER
        // Yeni hazır seti bir kez ekle (kullanıcı silerse tekrar eklenmez)
        if (!p.getBoolean("v103", false)) {
            p.edit().putBoolean("v103", true).apply()
            if (list.none { it.id == SUNNET_VIRD.id }) return (list + SUNNET_VIRD).also { saveVirds(it) }
        }
        return list
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
        if (nz.count % nz.target == 0) {
            val l = loadLaps().toMutableMap()
            val m = (l[k] ?: emptyMap()).toMutableMap()
            m[nz.okunus] = (m[nz.okunus] ?: 0) + 1
            l[k] = m
            saveLaps(l)
        }
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
        .put("laps", p.getString("laps", "{}"))
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
            .putString("laps", o.optString("laps", "{}"))
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
            target = o.optInt("t", 33).coerceIn(1, 1_000_000),
        )
    }.distinctBy { it.id }

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
                Step(s.optString("ar"), s.getString("ok"), s.optString("an"), s.optInt("t", 33).coerceIn(1, 1_000_000))
            },
        )
    }.distinctBy { it.id }

    private fun virdsTo(v: List<Vird>) = JSONArray().apply {
        v.forEach { vd ->
            val sa = JSONArray()
            vd.steps.forEach { s -> sa.put(JSONObject().put("ar", s.arapca).put("ok", s.okunus).put("an", s.anlam).put("t", s.target)) }
            put(JSONObject().put("id", vd.id).put("name", vd.name).put("steps", sa))
        }
    }
}
