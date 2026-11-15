package com.gezgin.zikirmatik

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uzman panelinin bulduğu hataların her biri için bir test (hata önce testle yakalanır). */
class EngineTest {
    private val vird = Vird(7, "T", listOf(Step("", "a", "", 33), Step("", "b", "", 33), Step("", "c", "", 1)))

    // ── Sayma ──
    @Test fun serbest_sayim_artar() = assertEquals(Engine.Tap.Counted(1, false, false), Engine.tapFree(0, 33, false, 33))
    @Test fun hedefte_tamamlandi() = assertTrue((Engine.tapFree(32, 33, false, 33) as Engine.Tap.Counted).completed)
    @Test fun tur_modunda_hedef_asilir() = assertEquals(34, (Engine.tapFree(33, 33, false, 33) as Engine.Tap.Counted).count)
    @Test fun hedefte_dur_engeller() = assertEquals(Engine.Tap.Blocked, Engine.tapFree(33, 33, true, 33))
    @Test fun ara_titresim_hedefle_cakismaz() {
        val t = Engine.tapFree(32, 33, false, 33) as Engine.Tap.Counted
        assertTrue(t.completed); assertFalse(t.milestone)
    }
    @Test fun ara_titresim_her_10() = assertTrue((Engine.tapFree(9, 100, false, 10) as Engine.Tap.Counted).milestone)
    @Test fun adim_dolunca_yok_sayilir() = assertEquals(Engine.Tap.Ignored, Engine.tapStep(33, 33, 33))

    // ── Hata 4: "hedefte dur" açıkken geri al görünen sayıdan düşmeli ──
    @Test fun geri_al_hedefte_durda_gorunenden_duser() = assertEquals(32, Engine.undoFree(50, 33, true))
    @Test fun geri_al_sifirda_null() = assertNull(Engine.undoFree(0, 33, false))
    @Test fun hedef_dusurulunce_kirpilir() = assertEquals(33, Engine.countOnTargetChange(50, 33, true))
    @Test fun tur_modunda_hedef_degisince_korunur() = assertEquals(50, Engine.countOnTargetChange(50, 33, false))

    // ── Hata 3: sıfırlamayı geri almak sonradan çekilenleri silmemeli ──
    @Test fun sifirlama_geri_alma_toplar() = assertEquals(45, Engine.undoReset(5, 40))

    // ── Hata 1: bekleme süresinde geri alınırsa adım atlanmamalı ──
    @Test fun geri_alininca_gecis_iptal() =
        assertFalse(Engine.shouldAdvance(Session(7, 0, 32), vird, 7, 0, skipping = false))
    @Test fun adim_doluysa_gecilir() =
        assertTrue(Engine.shouldAdvance(Session(7, 0, 33), vird, 7, 0, skipping = false))
    @Test fun atla_her_zaman_gecer() =
        assertTrue(Engine.shouldAdvance(Session(7, 0, 5), vird, 7, 0, skipping = true))
    @Test fun baska_adima_gecilmisse_gecilmez() =
        assertFalse(Engine.shouldAdvance(Session(7, 1, 33), vird, 7, 0, skipping = false))
    @Test fun son_adimdan_sonra_biter() = assertNull(Engine.nextStep(Session(7, 2, 1), vird))

    // ── Hata 2: geçiş anında kapanan uygulama takılı kalmamalı ──
    @Test fun dolu_adim_acilista_ilerler() =
        assertEquals(Session(7, 1, 0) to false, Engine.normalize(Session(7, 0, 33), listOf(vird)))
    @Test fun son_adim_doluysa_tamamlanir() =
        assertEquals(null to true, Engine.normalize(Session(7, 2, 1), listOf(vird)))
    @Test fun silinmis_tesbihat_oturumu_biter() =
        assertEquals(null to false, Engine.normalize(Session(99, 0, 3), listOf(vird)))
    @Test fun normal_oturum_degismez() =
        assertEquals(Session(7, 1, 10) to false, Engine.normalize(Session(7, 1, 10), listOf(vird)))

    // ── Hata 7: tesbihat düzenlenince oturum korunmalı ──
    @Test fun duzenlemede_oturum_korunur() =
        assertEquals(Session(7, 1, 10), Engine.sessionAfterEdit(Session(7, 1, 10), vird.steps))
    @Test fun duzenlemede_hedef_kucukse_kirpilir() =
        assertEquals(Session(7, 0, 11), Engine.sessionAfterEdit(Session(7, 0, 20), listOf(Step("", "a", "", 11))))
    @Test fun duzenlemede_adim_silindiyse_biter() =
        assertNull(Engine.sessionAfterEdit(Session(7, 2, 0), listOf(Step("", "a", "", 11))))
}
