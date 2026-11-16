package com.gezgin.zikirmatik

import java.util.Locale

/** Desteklenen arayüz dilleri. Ayarlardaki sıra = Settings.lang değeri (0 = sistem dili). */
data class Lang(val code: String, val native: String)

val LANGS = listOf(
    Lang("tr", "Türkçe"),
    Lang("en", "English"),
    Lang("ar", "العربية"),
    Lang("ur", "اردو"),
    Lang("id", "Bahasa Indonesia"),
    Lang("fr", "Français"),
    Lang("de", "Deutsch"),
    Lang("ru", "Русский"),
)

/**
 * Çok dilli arayüz.
 * Kaynak metinler kodda Türkçe + İngilizce yazılır: L("Türkçe", "English").
 * Diğer diller, İngilizce metni anahtar olarak kullanan tablolardan gelir (Translations.kt);
 * tabloda olmayan bir metin İngilizce gösterilir.
 * Dil değişince kök composable key(lang) ile baştan çizilir.
 * Zikir içerikleri (Arapça, okunuş, anlam, fazilet) Türkçe kalır.
 */
object I18n {
    @Volatile var code: String = "tr"
        private set

    /** 0 = sistem dili; 1.. = LANGS sırası */
    fun apply(setting: Int) {
        code = if (setting in 1..LANGS.size) LANGS[setting - 1].code
        else LANGS.firstOrNull { it.code == Locale.getDefault().language }?.code ?: "en"
    }

    val tr: Boolean get() = code == "tr"
    val rtl: Boolean get() = code == "ar" || code == "ur"

    /** Büyük/küçük harf ve tarih için dil yereli. */
    val locale: Locale get() = Locale.forLanguageTag(code)

    /** Sayılar her dilde Latin rakamlarıyla (yazı tipleri Arapça-Hint rakamlarını içermiyor). */
    val numLocale: Locale get() = if (rtl) Locale.US else locale

    internal fun table(): Map<String, String>? = when (code) {
        "ar" -> T_AR
        "ur" -> T_UR
        "id" -> T_ID
        "fr" -> T_FR
        "de" -> T_DE
        "ru" -> T_RU
        else -> null
    }
}

fun L(tr: String, en: String): String = when (I18n.code) {
    "tr" -> tr
    "en" -> en
    else -> I18n.table()?.get(en) ?: en
}

/** Yer tutuculu metin: {0}, {1} … sırasıyla args ile doldurulur. */
fun Lf(tr: String, en: String, vararg args: Any): String {
    var s = L(tr, en)
    args.forEachIndexed { i, a -> s = s.replace("{$i}", a.toString()) }
    return s
}

/** Etiketleri dile uygun büyük harfe çevir (Türkçede i → İ). */
fun String.upperL(): String = uppercase(I18n.locale)

private val NAMES_EN = mapOf(
    "Safir" to "Sapphire", "Gece" to "Night", "Fildişi" to "Ivory",
    "Kehribar" to "Amber", "Oltu" to "Jet", "Sedef" to "Pearl", "Kristal" to "Crystal",
)

/** Tema ve tespih adları. */
fun nameL(s: String): String = NAMES_EN[s]?.let { L(s, it) } ?: s

private val DAYS3 = mapOf(
    "tr" to listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"),
    "en" to listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    "ar" to listOf("إثن", "ثلا", "أرب", "خمي", "جمع", "سبت", "أحد"),
    "ur" to listOf("پیر", "منگل", "بدھ", "جمعرات", "جمعہ", "ہفتہ", "اتوار"),
    "id" to listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"),
    "fr" to listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"),
    "de" to listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"),
    "ru" to listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"),
)

/** Haftanın günü kısaltması (1 = Pazartesi … 7 = Pazar). */
fun dayShort(isoDay: Int): String = (DAYS3[I18n.code] ?: DAYS3.getValue("en"))[(isoDay - 1).coerceIn(0, 6)]

/** Takvim başlığı için iki harflik gün kısaltmaları (Pazartesi'den başlar). */
fun dayHeaders(): List<String> = (DAYS3[I18n.code] ?: DAYS3.getValue("en")).map { if (I18n.rtl) it else it.take(2) }
