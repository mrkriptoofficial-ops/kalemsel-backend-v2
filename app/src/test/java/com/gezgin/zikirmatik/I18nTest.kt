package com.gezgin.zikirmatik

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class I18nTest {
    private val tables = listOf(T_AR, T_UR, T_ID, T_FR, T_DE, T_RU)

    @Test fun tum_dillerde_ayni_anahtarlar() = tables.forEach { assertEquals(T_AR.keys, it.keys) }

    @Test fun yer_tutucular_korunur() = tables.forEach { t ->
        t.forEach { (en, tr) ->
            Regex("\\{\\d\\}|%02d").findAll(en).forEach { m -> assertTrue("$en → $tr", tr.contains(m.value)) }
        }
    }

    @Test fun ceviriler_bos_degil() = tables.forEach { t -> assertTrue(t.values.all { it.isNotBlank() }) }

    @Test fun her_dil_icin_7_gun() {
        for (i in 1..LANGS.size) {
            I18n.apply(i)
            assertEquals(7, (1..7).map { dayShort(it) }.toSet().size)
        }
        I18n.apply(1)
    }

    @Test fun yer_tutucu_doldurulur() {
        I18n.apply(2)
        assertEquals("ROUND 3", Lf("{0}. TUR", "ROUND {0}", 3))
        I18n.apply(1)
        assertEquals("3. TUR", Lf("{0}. TUR", "ROUND {0}", 3))
    }

    @Test fun arapca_sagdan_sola() { I18n.apply(3); assertTrue(I18n.rtl); I18n.apply(1) }
}
