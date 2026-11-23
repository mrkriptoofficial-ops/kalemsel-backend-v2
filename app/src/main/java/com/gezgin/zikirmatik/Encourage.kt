package com.gezgin.zikirmatik

/** Hedef tamamlanınca gösterilen teşvik metni: (ayet/hadis, kaynak). Web sürümüyle aynı eşleşme. */
private val ENCOURAGE_RULES: List<Pair<Regex, Pair<String, String>>> = listOf(
    Regex("subhan|sübhan") to ("Sübhânallâhi ve bihamdihî, Sübhânallâhi’l-azîm." to "Buhârî, Müslim"),
    Regex("elhamd|hamd") to ("Temizlik imanın yarısıdır. Elhamdülillah mizanı doldurur." to "Müslim"),
    Regex("ekber") to ("Sübhânallah, elhamdülillah, lâ ilâhe illallah ve Allahu ekber; bunlardan hangisiyle başlarsan başla zarar etmezsin." to "Müslim"),
    Regex("ilahe|ilah") to ("Bil ki Allah’tan başka ilah yoktur." to "Muhammed 19"),
    Regex("estag|istig") to ("Rabbinize istiğfar edin; çünkü O çok bağışlayıcıdır." to "Nûh 10"),
    Regex("salav|salli|salat") to ("Bana bir kez salavat getiren kimseye Allah on rahmet eder." to "Müslim"),
    Regex("esma|isim") to ("Allah’ın en güzel isimleri vardır; onlarla O’na dua edin." to "A’râf 180"),
    Regex("dua|rabbena") to ("Dua ibadetin özüdür." to "Tirmizî, Ebû Dâvûd"),
    Regex("kur|hisn") to ("Kim Allah’ın Kitabı’ndan bir harf okursa, ona bir sevap vardır." to "Tirmizî"),
)
private val ENCOURAGE_DEFAULT = "Siz beni anın ki ben de sizi anayım." to "Bakara 152"

/** Zikrin okunuşuna göre teşvik metni seçer. Aksan farkları önemsizdir. */
fun encouragementFor(name: String): Pair<String, String> {
    val n = java.text.Normalizer.normalize(name.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}"), "")
    return ENCOURAGE_RULES.firstOrNull { it.first.containsMatchIn(n) }?.second ?: ENCOURAGE_DEFAULT
}
