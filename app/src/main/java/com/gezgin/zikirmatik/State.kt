package com.gezgin.zikirmatik

import android.content.Context
import android.os.SystemClock
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
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

/** Uygulama içi ses seviyeleri: düşük, orta, yüksek */
val SOUND_VOLUMES = floatArrayOf(0.55f, 0.8f, 1f)

class AppState(private val ctx: Context, val repo: Repo, val fb: Feedback, private val scope: CoroutineScope) {
    val zikirs = mutableStateListOf<Zikir>()
    var selId by mutableLongStateOf(1L)
    var settings by mutableStateOf(Settings())
        private set
    val history = mutableStateMapOf<String, Int>()
    var total by mutableIntStateOf(0)
    var bestStreak by mutableIntStateOf(0)
    val virds = mutableStateListOf<Vird>()
    /** Gün → (zikir adı → tamamlanan tur sayısı) */
    val laps = mutableStateMapOf<String, Map<String, Int>>()
    var session by mutableStateOf<Session?>(null)

    // görsel olay tetikleyicileri
    var pulseKey by mutableIntStateOf(0)
    var celebrateKey by mutableIntStateOf(0)
    var shakeKey by mutableIntStateOf(0)
    var cardDir by mutableIntStateOf(1)

    /** Hedef tamamlanınca gösterilen (metin, kaynak) çifti; kısa süre sonra temizlenir. */
    var encourage by mutableStateOf<Pair<String, String>?>(null)
    /** Sesli sayma çalışıyor mu (çalışma anında; uygulama açılışında kapalı). */
    var voiceOn by mutableStateOf(false)
    /** Mikrofon izni istemek için MainActivity tarafından verilir. */
    var requestMic: (() -> Unit)? = null
    private val voice = VoiceCounter(ctx) { onVoice(it) }
    private val encPicker = EncouragePicker(ctx.applicationContext.getSharedPreferences("zikirmatik", Context.MODE_PRIVATE))

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
        laps.clear(); laps.putAll(repo.loadLaps())
        // Uygulama adım tamamlandığı anda kapandıysa takılı kalmasın
        val (ns, finished) = Engine.normalize(repo.loadSession(), virds)
        session = ns
        repo.saveSession(ns)
        if (finished) toast(L("Tesbihat tamamlandı · Allah kabul etsin", "Sequence complete · May Allah accept it"))
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
        laps.clear(); laps.putAll(repo.loadLaps())
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
            when (val r = Engine.tapStep(s.count, step.target, 0)) {
                is Engine.Tap.Counted -> {
                    session = s.copy(count = r.count)
                    repo.saveSession(session)
                    addHistory(1)
                    feedback(r)
                    if (r.completed) {
                        addLap(step.okunus, 1)
                        celebrateKey++
                        val vid = v.id
                        val si = s.step
                        scope.launch { delay(1200); advance(vid, si) }
                    } else pulseKey++
                }
                else -> Unit
            }
            return
        }

        val z = current
        when (val r = Engine.tapFree(z.count, z.target, settings.stopAtTarget, 0)) {
            Engine.Tap.Blocked -> { shakeKey++; fb.error(settings.haptic) }
            Engine.Tap.Ignored -> Unit
            is Engine.Tap.Counted -> {
                replace(z.copy(count = r.count))
                addHistory(1)
                feedback(r)
                if (r.completed) { addLap(z.okunus, 1); celebrateKey++; encourage = encPicker.next() } else pulseKey++
            }
        }
    }

    /** Kullanıcı titreşimi kapattıysa hiçbir yerde titreşme. */
    private fun soft(level: Int = 1) = fb.tap(if (settings.haptic == 0) 0 else level)

    private fun feedback(r: Engine.Tap.Counted) {
        // Ses, titreşimden daha geç algılanır: önce başlat
        if (settings.sound) {
            val v = SOUND_VOLUMES[settings.soundVol.coerceIn(0, 2)]
            when {
                r.completed -> { fb.pull(settings.soundType, v, settings.slideSound); fb.chime(v, PULL_MS.toLong()) }
                r.milestone -> fb.doubleClick(settings.soundType, v)
                else -> fb.pull(settings.soundType, v, settings.slideSound)
            }
        }
        val h = settings.haptic
        when {
            r.completed -> fb.complete(h)
            r.milestone -> fb.milestone(h)
            else -> fb.tap(h)
        }
    }

    private fun addLap(name: String, d: Int) {
        val k = dayKey()
        val m = (laps[k] ?: emptyMap()).toMutableMap()
        val v = ((m[name] ?: 0) + d).coerceAtLeast(0)
        if (v == 0) m.remove(name) else m[name] = v
        if (m.isEmpty()) laps.remove(k) else laps[k] = m
        repo.saveLaps(laps)
    }

    fun lapsToday(): Map<String, Int> = laps[dayKey()] ?: emptyMap()
    fun lapsTotal(): Int = laps.values.sumOf { it.values.sum() }

    fun micGranted(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** Ayar anahtarı: izin varsa açar, yoksa izin ister. */
    fun requestVoice(on: Boolean) {
        if (!on) { setVoice(false); return }
        if (micGranted()) setVoice(true) else requestMic?.invoke()
    }

    fun setVoice(on: Boolean) {
        voiceOn = on && micGranted()
        if (voiceOn) voice.start() else voice.stop()
    }

    fun pauseVoice() { voice.stop() }
    fun resumeVoice() { if (voiceOn) voice.start() }

    private fun onVoice(text: String) {
        if (!voiceOn) return
        val heard = voiceKey(text)
        // Söylenen başka bir zikrin adıysa önce onu ana sayfaya getir (en uzun eşleşme kazanır)
        val hit = zikirs
            .filter { voiceKey(it.okunus).length >= 2 && heard.contains(voiceKey(it.okunus)) }
            .maxByOrNull { voiceKey(it.okunus).length }
        if (hit != null && hit.id != selId) select(hit.id)
        val target = voiceKey(current.okunus)
        if (target.length < 2) return
        val n = Regex(Regex.escape(target)).findAll(heard).count()
        if (n > 0) voiceAdd(n)
    }

    /** Tanıma metnini karşılaştırmak için: küçük harf, aksansız, yalnızca a-z ve 0-9. */
    private fun voiceKey(s: String): String =
        java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}"), "").replace("ı", "i").replace(Regex("[^a-z0-9]"), "")

    /** Sesle sayılan n tekrar: hedef kuralına uyar, tamamlanınca teşvik kartı çıkar. */
    private fun voiceAdd(n: Int) {
        repeat(n) {
            val z = current
            when (val r = Engine.tapFree(z.count, z.target, settings.stopAtTarget, 0)) {
                is Engine.Tap.Counted -> {
                    replace(z.copy(count = r.count))
                    addHistory(1)
                    if (r.completed) { addLap(z.okunus, 1); celebrateKey++; encourage = encPicker.next() }
                }
                else -> return@repeat
            }
        }
        pulseKey++
        fb.tap(settings.haptic)
    }

    fun undo() {
        val s = session
        if (s != null) {
            if (s.count > 0) {
                val step = virds.firstOrNull { it.id == s.virdId }?.steps?.getOrNull(s.step)
                if (step != null && s.count >= step.target) addLap(step.okunus, -1)
                session = s.copy(count = s.count - 1); repo.saveSession(session); addHistory(-1); soft()
            }
            return
        }
        val z = current
        val n = Engine.undoFree(z.count, z.target, settings.stopAtTarget)
        if (n != null) {
            val visibleBefore = n + 1
            if (visibleBefore % z.target == 0) addLap(z.okunus, -1)
            replace(z.copy(count = n)); addHistory(-1); soft()
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
        soft()
    }

    fun setTarget(t: Int) {
        val nt = t.coerceIn(1, 1_000_000)
        val z = current
        replace(z.copy(target = nt, count = Engine.countOnTargetChange(z.count, nt, settings.stopAtTarget)))
    }

    fun resetCurrent() {
        val s = session
        if (s != null) {
            if (s.count == 0) return
            session = s.copy(count = 0); repo.saveSession(session)
            scope.launch {
                val r = snack.showSnackbar(L("Adım sıfırlandı", "Step reset"), L("Geri al", "Undo"), duration = SnackbarDuration.Short)
                val now = session
                if (r == SnackbarResult.ActionPerformed && now != null && now.virdId == s.virdId && now.step == s.step) {
                    session = now.copy(count = Engine.undoReset(now.count, s.count)); repo.saveSession(session)
                }
            }
            return
        }
        val prev = current
        if (prev.count == 0) return
        replace(prev.copy(count = 0))
        scope.launch {
            val r = snack.showSnackbar(Lf("{0} sıfırlandı", "{0} reset", prev.okunus), L("Geri al", "Undo"), duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) zikirs.firstOrNull { it.id == prev.id }?.let { replace(it.copy(count = Engine.undoReset(it.count, prev.count))) }
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
            // Düzenleme paneli açıkken (ör. widget'tan) çekilenler kaybolmasın: güncel sayıyı kullan
            val cur = zikirs.firstOrNull { it.id == existing.id } ?: return
            replace(cur.copy(okunus = okunus, arapca = arapca, anlam = anlam, fazilet = fazilet, target = target,
                count = Engine.countOnTargetChange(cur.count, target, settings.stopAtTarget)))
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
            val r = snack.showSnackbar(Lf("{0} silindi", "{0} deleted", z.okunus), L("Geri al", "Undo"), duration = SnackbarDuration.Short)
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
        soft()
    }

    fun hasLib(item: LibItem) = zikirs.any { it.okunus == item.zikirOkunus || (item.arapca.isNotBlank() && it.arapca == item.arapca) }

    fun addFromLib(item: LibItem) {
        if (hasLib(item)) return
        val z = Zikir(System.currentTimeMillis(), item.arapca, item.zikirOkunus, item.anlam, item.fazilet, 0, item.target)
        zikirs.add(z)
        repo.saveZikirs(zikirs)
        soft(2)
        scope.launch { snack.showSnackbar(Lf("{0} zikirlerine eklendi", "{0} added to your dhikr", item.zikirOkunus), duration = SnackbarDuration.Short) }
    }

    // ───────────── Tesbihat / vird ─────────────
    fun startVird(v: Vird) {
        if (v.steps.isEmpty()) return
        session = Session(v.id, 0, 0)
        repo.saveSession(session)
        cardDir = 1
        soft(2)
    }

    fun endSession() {
        if (session != null) { session = null; repo.saveSession(null) }
    }

    fun skipStep() {
        val s = session ?: return
        advance(s.virdId, s.step, skipping = true)
    }

    private fun advance(vid: Long, fromStep: Int, skipping: Boolean = false) {
        val s = session ?: return
        if (s.virdId != vid || s.step != fromStep) return
        val v = virds.firstOrNull { it.id == vid } ?: run { endSession(); return }
        if (!Engine.shouldAdvance(s, v, vid, fromStep, skipping)) return
        cardDir = 1
        val next = Engine.nextStep(s, v)
        if (next != null) {
            session = next
            repo.saveSession(session)
        } else {
            endSession()
            scope.launch { snack.showSnackbar(Lf("{0} tamamlandı · Allah kabul etsin", "{0} complete · May Allah accept it", v.name), duration = SnackbarDuration.Long) }
        }
    }

    fun saveVird(existing: Vird?, name: String, steps: List<Step>) {
        if (existing == null) virds.add(Vird(System.currentTimeMillis(), name, steps))
        else {
            val i = virds.indexOfFirst { it.id == existing.id }
            if (i >= 0) virds[i] = existing.copy(name = name, steps = steps)
            val s = session
            if (s != null && s.virdId == existing.id) {
                val ns = Engine.sessionAfterEdit(s, steps)
                if (ns == null) endSession() else if (ns != s) { session = ns; repo.saveSession(ns) }
            }
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
            val r = snack.showSnackbar(Lf("{0} silindi", "{0} deleted", v.name), L("Geri al", "Undo"), duration = SnackbarDuration.Short)
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
