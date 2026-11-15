package com.gezgin.zikirmatik

/**
 * Sayma kurallarının saf (Android'siz) motoru.
 *
 * Mimari karar: Tüm sayım/tesbihat kuralları burada, yan etkisiz fonksiyonlar olarak durur.
 * AppState yalnızca durumu tutar, kaydeder ve geri bildirim (titreşim/ses) verir.
 * Böylece kurallar JVM birim testleriyle doğrulanabilir.
 */
object Engine {

    sealed interface Tap {
        /** Sayılmadı ve kullanıcıya hata hissi verilmeli (ör. "hedefte dur" açıkken hedef doldu). */
        data object Blocked : Tap
        /** Sessizce yok sayıldı (ör. tesbihat adımı doldu, sonraki adıma geçiş bekleniyor). */
        data object Ignored : Tap
        data class Counted(val count: Int, val completed: Boolean, val milestone: Boolean) : Tap
    }

    /** Serbest zikirde bir dokunuş. */
    fun tapFree(count: Int, target: Int, stopAtTarget: Boolean, milestone: Int): Tap {
        if (stopAtTarget && count >= target) return Tap.Blocked
        val n = count + 1
        return Tap.Counted(n, completed = n % target == 0, milestone = milestone > 0 && n % milestone == 0 && n % target != 0)
    }

    /** Tesbihat adımında bir dokunuş. */
    fun tapStep(count: Int, target: Int, milestone: Int): Tap {
        if (count >= target) return Tap.Ignored
        val n = count + 1
        return Tap.Counted(n, completed = n >= target, milestone = milestone > 0 && n % milestone == 0 && n < target)
    }

    /** Serbest zikirde geri alma; null ise geri alınacak bir şey yok. "Hedefte dur" açıkken görünen sayıdan düşer. */
    fun undoFree(count: Int, target: Int, stopAtTarget: Boolean): Int? {
        val visible = if (stopAtTarget) count.coerceAtMost(target) else count
        return if (visible > 0) visible - 1 else null
    }

    /** Hedef değişince sayacın alacağı değer. */
    fun countOnTargetChange(count: Int, newTarget: Int, stopAtTarget: Boolean): Int =
        if (stopAtTarget) count.coerceAtMost(newTarget) else count

    /** Sıfırlama geri alındığında: sıfırlamadan sonra çekilenler kaybolmasın. */
    fun undoReset(countNow: Int, countBeforeReset: Int): Int = countNow + countBeforeReset

    /**
     * Zamanlanmış geçişte adımın gerçekten tamamlanıp tamamlanmadığını kontrol eder.
     * Kullanıcı bekleme süresinde geri aldıysa geçiş iptal olur.
     */
    fun shouldAdvance(s: Session, v: Vird, expectedVird: Long, expectedStep: Int, skipping: Boolean): Boolean {
        if (s.virdId != expectedVird || s.step != expectedStep) return false
        if (skipping) return true
        val t = v.steps.getOrNull(expectedStep)?.target ?: return true
        return s.count >= t
    }

    /** Sonraki adım; null ise tesbihat bitti. */
    fun nextStep(s: Session, v: Vird): Session? =
        if (s.step + 1 < v.steps.size) Session(v.id, s.step + 1, 0) else null

    /**
     * Uygulama yeniden açıldığında oturumu tutarlı hale getirir:
     * - tesbihat silinmiş/adım yoksa oturum biter,
     * - adım dolu kaldıysa (geçiş anında kapandıysa) sonraki adıma geçilir.
     * Dönüş: (yeni oturum ya da null, tesbihat bu düzeltmeyle tamamlandı mı)
     */
    fun normalize(s: Session?, virds: List<Vird>): Pair<Session?, Boolean> {
        if (s == null) return null to false
        val v = virds.firstOrNull { it.id == s.virdId } ?: return null to false
        val step = v.steps.getOrNull(s.step) ?: return null to false
        if (s.count < step.target) return s to false
        val next = nextStep(s, v)
        return next to (next == null)
    }

    /** Tesbihat düzenlenince süren oturum. */
    fun sessionAfterEdit(s: Session, newSteps: List<Step>): Session? {
        val step = newSteps.getOrNull(s.step) ?: return null
        return if (s.count > step.target) s.copy(count = step.target) else s
    }
}
