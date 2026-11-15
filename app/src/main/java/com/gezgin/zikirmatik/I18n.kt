package com.gezgin.zikirmatik

import java.util.Locale

/**
 * Basit iki dilli arayüz (Türkçe / English).
 * Dil değişince kök composable `key(lang)` ile baştan çizilir; bu yüzden düz bir fonksiyon yeterli.
 * Zikir metinleri (Arapça, okunuş, anlam, fazilet) Türkçe kalır.
 */
object I18n {
    @Volatile var en: Boolean = false

    /** 0 = sistem dili, 1 = Türkçe, 2 = English */
    fun apply(setting: Int) {
        en = when (setting) {
            1 -> false
            2 -> true
            else -> Locale.getDefault().language != "tr"
        }
    }

    val locale: Locale get() = if (en) Locale.ENGLISH else Locale.forLanguageTag("tr")
}

fun L(tr: String, en: String): String = if (I18n.en) en else tr

/** Etiketleri dile uygun büyük harfe çevir (Türkçede i → İ, İngilizcede i → I). */
fun String.upperL(): String = uppercase(I18n.locale)

private val NAMES = mapOf(
    "Safir" to "Sapphire", "Gece" to "Night", "Fildişi" to "Ivory",
    "Kehribar" to "Amber", "Oltu" to "Jet", "Sedef" to "Pearl", "Kristal" to "Crystal",
)

fun nameL(s: String): String = if (I18n.en) NAMES[s] ?: s else s
