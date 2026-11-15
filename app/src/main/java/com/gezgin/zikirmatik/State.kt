package com.gezgin.zikirmatik

import android.content.Context
import android.os.SystemClock
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/** Ekranda gösterilen aktif zikir (serbest zikir ya da tesbihat adımı). */
data class Disp(
    val key: String,
    val arapca: String,
    val okunus: String,
    val anlam: String,
    val fazilet: String,
    val count: Int,
    val target: Int,
    val vird: Vird?,
    val step: Int,
    val stopAtTarget: Boolean,
) {
    val shown: Int get() = when {
        vird != null || stopAtTarget -> count.coerceAtMost(target)
        count > 0 && count % target == 0 -> target
        else -> count % target
    }
    val laps: Int get() = if (vird != null || stopAtTarget) 0 else count / target
    val progress: Float get() = shown / target.toFloat()
}

class AppState(private val ctx: Context, val repo: Repo, val fb: Feedback, private val scope: CoroutineScope) {
    val zikirs = mutableStateListOf<Zikir>()
    var selId by mutableLongStateOf(1L)
    var settings by mutableStateOf(Settings())
        private set
    val history = mutableStateMapOf<String, Int>()
    var total by mutableIntStateOf(0)
    var bestStreak by mutableIntStateOf(0)
    val virds = mutableStateListOf<Vird>()
    var session by mutableStateOf<Session?>(null)

    // görsel olay tetikleyicileri
    var pulseKey by mutableIntStateOf(0)
    var celebrateKey by mutableIntStateOf(0)
    var shakeKey by mutableIntStateOf(0)
    var cardDir by mutableIntStateOf(1)

    val snack = SnackbarHostState()
    private var lastTap = 0L

    fun load() {
        zikirs.clear(); zikirs.addAll(repo.loadZikirs())
        selId = repo.selId.takeIf { id -> zikirs.any { it.id == id } } ?: zikirs.first().id
        settings = repo.loadSettings()
        history.clear(); history.putAll(repo.loadHistory())
        total = repo.total
        bestStreak = maxOf(repo.bestStreak, streakOf(history))
        virds.clear(); virds.addAll(repo.loadVirds())
        session = repo.loadSession()?.takeIf { s -> virds.any { it.id == s.virdId } }
    }

    /** Widget gibi dış kaynakların değişikliklerini al. */
    fun reloadCounts() {
        val disk = repo.loadZikirs()
        if (disk.map { it.id } == zikirs.map { it.id }) {
            disk.forEachIndexed { i, z -> if (zikirs[i] != z) zikirs[i] = z }
        } else {
            zikirs.clear(); zikirs.addAll(disk)
        }
        if (zikirs.none { it.id == selId }) selId = zikirs.first().id
        history.clear(); history.putAll(repo.loadHistory())
        total = repo.total
        bestStreak = maxOf(repo.bestStreak, streakOf(history))
    }

    val current: Zikir get() = zikirs.firstOrNull { it.id == selId } ?: zikirs.first()
    val currentIndex: Int get() = zikirs.indexOfFirst { it.id == selId }.coerceAtLeast(0)

    fun display(): Disp {
        val s = session
        val v = s?.let { ss -> virds.firstOrNull { it.id == ss.virdId } }
        val step = if (s != null && v != null) v.steps.getOrNull(s.step) else null
        return if (s != null && v != null && step != null) {
            Disp("v${v.id}_${s.step}", step.arapca, step.okunus, step.anlam, "", s.count, step.target, v, s.step, true)
        } else {
            val z = current
            Disp("z${z.id}", z.arapca, z.okunus, z.anlam, z.fazilet, z.count, z.target, null, 0, settings.stopAtTarget)
        }
    }

    // ───────────── Sayma ─────────────
    fun tap() {
        val now = SystemClock.uptimeMillis()
        if (now - lastTap < settings.guardMs) return
        lastTap = now

        val s = session
        if (s != null) {
            val v = virds.firstOrNull { it.id == s.virdId }
            val step = v?.steps?.getOrNull(s.step)
            if (v == null || step == null) { endSession(); return }
            if (s.count >= step.target) return
            val n = s.count + 1
            session = s.copy(count = n)
            repo.saveSession(session)
            addHistory(1)
            feedback(n, step.target)
            if (n >= step.target) {
                celebrateKey++
                val vid = v.id
                val si = s.step
                scope.launch { delay(1200); advance(vid, si) }
            } else pulseKey++
            return
        }

        val z = current
        if (settings.stopAtTarget && z.count >= z.target) {
            shakeKey++
            fb.error(settings.haptic)
            return
        }
        val n = z.count + 1
        replace(z.copy(count = n))
        addHistory(1)
        feedback(n, z.target)
        if (n % z.target == 0) celebrateKey++ else pulseKey++
    }

    private fun feedback(n: Int, target: Int) {
        val h = settings.haptic
        when {
            n % target == 0 -> fb.complete(h)
            settings.milestone > 0 && n % settings.milestone == 0 -> fb.milestone(h)
            else -> fb.tap(h)
        }
        fb.click(settings.sound)
    }

    fun undo() {
        val s = session
        if (s != null) {
            if (s.count > 0) {
                session = s.copy(count = s.count - 1); repo.saveSession(session); addHistory(-1); fb.tap(1)
            }
            return
        }
        val z = current
        if (z.count > 0) {
            replace(z.copy(count = z.count - 1)); addHistory(-1); fb.tap(1)
        } else fb.error(settings.haptic)
    }

    private fun addHistory(d: Int) {
        val k = dayKey()
        history[k] = ((history[k] ?: 0) + d).coerceAtLeast(0)
        total = (total + d).coerceAtLeast(0)
        repo.saveHistory(history)
        repo.total = total
        val st = streakOf(history)
        if (st > bestStreak) { bestStreak = st; repo.bestStreak = st }
    }

    // ───────────── Zikir yönetimi ─────────────
    fun replace(z: Zikir) {
        val i = zikirs.indexOfFirst { it.id == z.id }
        if (i >= 0) { zikirs[i] = z; repo.saveZikirs(zikirs) }
    }

    fun select(id: Long) {
        val old = currentIndex
        selId = id
        repo.selId = id
        cardDir = if (currentIndex >= old) 1 else -1
    }

    fun step(delta: Int) {
        if (zikirs.size < 2 || session != null) return
        val n = zikirs.size
        cardDir = if (delta > 0) 1 else -1
        val id = zikirs[(currentIndex + delta + n) % n].id
        selId = id
        repo.selId = id
        fb.tap(1)
    }

    fun setTarget(t: Int) = replace(current.copy(target = t.coerceIn(1, 1_000_000)))

    fun resetCurrent() {
        val s = session
        if (s != null) { session = s.copy(count = 0); repo.saveSession(session); return }
        val prev = current
        if (prev.count == 0) return
        replace(prev.copy(count = 0))
        scope.launch {
            val r = snack.showSnackbar("${prev.okunus} sıfırlandı", "Geri al", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) zikirs.firstOrNull { it.id == prev.id }?.let { replace(it.copy(count = prev.count)) }
        }
    }

    fun saveZikir(existing: Zikir?, okunus: String, arapca: String, anlam: String, fazilet: String, target: Int) {
        if (existing == null) {
            val z = Zikir(System.currentTimeMillis(), arapca, okunus, anlam, fazilet, 0, target)
            zikirs.add(z)
            repo.saveZikirs(zikirs)
            endSession()
            select(z.id)
        } else {
            replace(existing.copy(okunus = okunus, arapca = arapca, anlam = anlam, fazilet = fazilet, target = target))
        }
    }

    fun delete(z: Zikir) {
        if (zikirs.size <= 1) return
        val idx = zikirs.indexOfFirst { it.id == z.id }
        if (idx < 0) return
        zikirs.removeAt(idx)
        repo.saveZikirs(zikirs)
        if (selId == z.id) { selId = zikirs[(idx - 1).coerceAtLeast(0)].id; repo.selId = selId }
        scope.launch {
            val r = snack.showSnackbar("${z.okunus} silindi", "Geri al", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed && zikirs.none { it.id == z.id }) {
                zikirs.add(idx.coerceAtMost(zikirs.size), z)
                repo.saveZikirs(zikirs)
            }
        }
    }

    fun move(z: Zikir, delta: Int) {
        val i = zikirs.indexOfFirst { it.id == z.id }
        val j = i + delta
        if (i < 0 || j !in zikirs.indices) return
        val tmp = zikirs[j]
        zikirs[j] = zikirs[i]
        zikirs[i] = tmp
        repo.saveZikirs(zikirs)
        fb.tap(1)
    }

    fun hasLib(item: LibItem) = zikirs.any { it.okunus == item.zikirOkunus || (item.arapca.isNotBlank() && it.arapca == item.arapca) }

    fun addFromLib(item: LibItem) {
        if (hasLib(item)) return
        val z = Zikir(System.currentTimeMillis(), item.arapca, item.zikirOkunus, item.anlam, item.fazilet, 0, item.target)
        zikirs.add(z)
        repo.saveZikirs(zikirs)
        fb.tap(2)
        scope.launch { snack.showSnackbar("${item.zikirOkunus} zikirlerine eklendi", duration = SnackbarDuration.Short) }
    }

    // ───────────── Tesbihat / vird ─────────────
    fun startVird(v: Vird) {
        if (v.steps.isEmpty()) return
        session = Session(v.id, 0, 0)
        repo.saveSession(session)
        cardDir = 1
        fb.tap(2)
    }

    fun endSession() {
        if (session != null) { session = null; repo.saveSession(null) }
    }

    fun skipStep() {
        val s = session ?: return
        advance(s.virdId, s.step)
    }

    private fun advance(vid: Long, fromStep: Int) {
        val s = session ?: return
        if (s.virdId != vid || s.step != fromStep) return
        val v = virds.firstOrNull { it.id == vid } ?: run { endSession(); return }
        cardDir = 1
        if (s.step + 1 < v.steps.size) {
            session = Session(vid, s.step + 1, 0)
            repo.saveSession(session)
        } else {
            endSession()
            scope.launch { snack.showSnackbar("${v.name} tamamlandı · Allah kabul etsin", duration = SnackbarDuration.Long) }
        }
    }

    fun saveVird(existing: Vird?, name: String, steps: List<Step>) {
        if (existing == null) virds.add(Vird(System.currentTimeMillis(), name, steps))
        else {
            val i = virds.indexOfFirst { it.id == existing.id }
            if (i >= 0) virds[i] = existing.copy(name = name, steps = steps)
            if (session?.virdId == existing.id) endSession()
        }
        repo.saveVirds(virds)
    }

    fun deleteVird(v: Vird) {
        val idx = virds.indexOfFirst { it.id == v.id }
        if (idx < 0) return
        virds.removeAt(idx)
        repo.saveVirds(virds)
        if (session?.virdId == v.id) endSession()
        scope.launch {
            val r = snack.showSnackbar("${v.name} silindi", "Geri al", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) { virds.add(idx.coerceAtMost(virds.size), v); repo.saveVirds(virds) }
        }
    }

    // ───────────── Ayarlar ─────────────
    fun update(f: (Settings) -> Settings) {
        val old = settings
        settings = f(settings)
        repo.saveSettings(settings)
        if (old.remMorning != settings.remMorning || old.morningMin != settings.morningMin ||
            old.remEvening != settings.remEvening || old.eveningMin != settings.eveningMin
        ) Reminders.schedule(ctx, settings)
    }

    fun toast(msg: String) { scope.launch { snack.showSnackbar(msg, duration = SnackbarDuration.Short) } }

    // ───────────── İstatistik ─────────────
    fun today(): Int = history[dayKey()] ?: 0
    fun streak(): Int = streakOf(history)

    fun sumSince(from: LocalDate): Int {
        var d = from
        val t = LocalDate.now()
        var sum = 0
        while (!d.isAfter(t)) { sum += history[dayKey(d)] ?: 0; d = d.plusDays(1) }
        return sum
    }

    fun weekSum(): Int = sumSince(LocalDate.now().with(DayOfWeek.MONDAY))
    fun monthSum(): Int = sumSince(LocalDate.now().withDayOfMonth(1))
}
