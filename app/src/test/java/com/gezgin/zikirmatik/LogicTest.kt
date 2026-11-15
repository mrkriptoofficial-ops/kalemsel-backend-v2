package com.gezgin.zikirmatik

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** İş mantığı birim testleri: seri hesabı, tur/hedef gösterimi, içerik bütünlüğü. */
class LogicTest {
    private val today = LocalDate.of(2026, 11, 15)
    private fun h(vararg daysAgo: Int) = daysAgo.associate { dayKey(today.minusDays(it.toLong())) to 5 }

    // ── Seri (streak) ──
    @Test fun streak_bos_gecmis_sifir() = assertEquals(0, streakOf(emptyMap(), today))
    @Test fun streak_bugun_dahil() = assertEquals(3, streakOf(h(0, 1, 2), today))
    @Test fun streak_bugun_henuz_cekilmedi_dunden_sayar() = assertEquals(2, streakOf(h(1, 2), today))
    @Test fun streak_bosluk_seriyi_keser() = assertEquals(1, streakOf(h(0, 2, 3), today))
    @Test fun streak_dun_yoksa_sifir() = assertEquals(0, streakOf(h(2, 3), today))

    // ── Gösterim: tur modu ──
    private fun d(count: Int, target: Int, stop: Boolean = false, vird: Vird? = null) =
        Disp("k", "", "x", "", "", count, target, vird, 0, stop || vird != null)

    @Test fun tur_hedefte_hedefi_gosterir() { val x = d(33, 33); assertEquals(33, x.shown); assertEquals(1, x.laps) }
    @Test fun tur_sonrasi_birden_baslar() { val x = d(34, 33); assertEquals(1, x.shown); assertEquals(1, x.laps) }
    @Test fun sifirda_sifir() { val x = d(0, 33); assertEquals(0, x.shown); assertEquals(0, x.laps); assertEquals(0f, x.progress) }
    @Test fun ikinci_tur_tamam() { val x = d(66, 33); assertEquals(33, x.shown); assertEquals(2, x.laps) }

    // ── Gösterim: hedefte dur / tesbihat ──
    @Test fun hedefte_dur_tasmaz() { val x = d(50, 33, stop = true); assertEquals(33, x.shown); assertEquals(0, x.laps); assertEquals(1f, x.progress) }
    @Test fun tesbihat_tur_saymaz() {
        val v = Vird(1, "t", listOf(Step("", "a", "", 33)))
        val x = d(33, 33, vird = v)
        assertEquals(33, x.shown); assertEquals(0, x.laps)
    }

    // ── İçerik bütünlüğü ──
    @Test fun esma_99_isim() = assertEquals(99, LIB_ESMA.size)
    @Test fun esma_arapca_bos_degil() = assertTrue(LIB_ESMA.all { it.arapca.isNotBlank() && it.anlam.isNotBlank() })
    @Test fun esma_anlamlari_tekil() = assertEquals(LIB_ESMA.size, LIB_ESMA.map { it.anlam }.toSet().size)
    @Test fun kutuphane_anahtarlari_tekil() {
        listOf(LIB_SUNNET, LIB_GUNLUK, LIB_SALAVAT, LIB_ESMA).forEach { l -> assertEquals(l.size, l.map { it.okunus }.toSet().size) }
    }
    @Test fun varsayilan_kimlikler_tekil() = assertEquals(VARSAYILAN.size, VARSAYILAN.map { it.id }.toSet().size)
    @Test fun sunnet_her_maddede_kaynak_var() = assertTrue(LIB_SUNNET.all { it.fazilet.contains("(") && it.fazilet.contains(")") })
    @Test fun sunnet_zikir_adlari_tekil() = assertEquals(LIB_SUNNET.size, LIB_SUNNET.map { it.zikirOkunus }.toSet().size)
    @Test fun namaz_tesbihati_100_eder() = assertEquals(100, VARSAYILAN_VIRDLER.first().total)
}

class AmberTest {
    @Test fun sayi_arttikca_koyulasir() {
        val l0 = androidx.compose.ui.graphics.Color(amberAt(0f).base.value).luminance()
        val l5 = amberAt(0.5f).base.luminance()
        val l1 = amberAt(1f).base.luminance()
        assertTrue(l0 > l5 && l5 > l1)
    }
    @Test fun yazi_rengi_koyu_tonda_doner() { assertTrue(!amberIsDark(0.1f)); assertTrue(amberIsDark(0.9f)) }
}
