package com.gezgin.zikirmatik

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Handler
import android.os.Looper
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.math.abs

fun Context.systemVibrator(): Vibrator =
    if (Build.VERSION.SDK_INT >= 31) {
        getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

// ───────────── Eski Android sürümleri için titreşim yardımcıları (VibrationEffect API 26+) ─────────────
fun Vibrator.oneShotCompat(ms: Long, amp: Int) {
    if (Build.VERSION.SDK_INT >= 26) vibrate(VibrationEffect.createOneShot(ms, amp))
    else @Suppress("DEPRECATION") vibrate(ms)
}

fun Vibrator.waveformCompat(timings: LongArray, amps: IntArray? = null) {
    when {
        Build.VERSION.SDK_INT >= 26 && amps != null && hasAmplitudeControl() -> vibrate(VibrationEffect.createWaveform(timings, amps, -1))
        Build.VERSION.SDK_INT >= 26 -> vibrate(VibrationEffect.createWaveform(timings, -1))
        else -> @Suppress("DEPRECATION") vibrate(timings, -1)
    }
}

/** Katmanlı titreşim (destekleyen cihazlarda ince ayarlı haptik primitifler) + sentezlenmiş tespih tanesi sesi. */
class Feedback(ctx: Context) {
    private val vib = ctx.systemVibrator()
    private val hasVib = vib.hasVibrator()
    private val prim = Build.VERSION.SDK_INT >= 30 && hasVib && vib.areAllPrimitivesSupported(
        VibrationEffect.Composition.PRIMITIVE_CLICK, VibrationEffect.Composition.PRIMITIVE_TICK,
    )
    private val rnd = java.util.Random()
    private val main = Handler(Looper.getMainLooper())
    private val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    /** Telefon sessiz ya da titreşim modundaysa (ör. camide) ses çıkarma. */
    private fun silent(): Boolean = audio != null && audio.ringerMode != AudioManager.RINGER_MODE_NORMAL
    private var pool: SoundPool? = null
    private val ids = IntArray(SOUNDS.size)
    private val ready = BooleanArray(SOUNDS.size)

    private val tapFx: Array<VibrationEffect?> = arrayOf(
        null,
        fx(VibrationEffect.Composition.PRIMITIVE_TICK, 0.75f, VibrationEffect.EFFECT_TICK),
        fx(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f, VibrationEffect.EFFECT_CLICK),
        fx(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f, VibrationEffect.EFFECT_HEAVY_CLICK),
    )

    init {
        runCatching {
            // Medya ses kanalı: kullanıcı sistem medya sesiyle de, uygulama içi seviyeyle de ayarlayabilir
            val sp = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                ).build()
            sp.setOnLoadCompleteListener { _, id, status ->
                val i = ids.indexOf(id)
                if (i >= 0) ready[i] = status == 0
            }
            File(ctx.cacheDir, "tane_v3.wav").delete()   // eski sürümün tek sesi
            SOUNDS.forEachIndexed { i, spec -> ids[i] = sp.load(ensureWav(ctx, spec).path, 1) }
            pool = sp
        }
    }

    private fun fx(p: Int, scale: Float, fallback: Int): VibrationEffect? = when {
        prim && Build.VERSION.SDK_INT >= 30 -> VibrationEffect.startComposition().addPrimitive(p, scale).compose()
        Build.VERSION.SDK_INT >= 29 -> VibrationEffect.createPredefined(fallback)
        else -> null
    }

    fun tap(level: Int) {
        if (level <= 0 || !hasVib) return
        val l = level.coerceIn(1, 3)
        val e = tapFx[l]
        if (e != null && Build.VERSION.SDK_INT >= 26) vib.vibrate(e)
        else vib.oneShotCompat(longArrayOf(0, 8, 14, 22)[l], 255)
    }

    fun milestone(level: Int) {
        if (level <= 0 || !hasVib) return
        when {
            prim && Build.VERSION.SDK_INT >= 30 -> vib.vibrate(
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f, 70)
                    .compose()
            )
            Build.VERSION.SDK_INT >= 29 -> vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            else -> vib.waveformCompat(longArrayOf(0, 25, 60, 25))
        }
    }

    /** Hedef: tespihin imame tanesine varış hissi — üç yükselen vuruş. */
    fun complete(level: Int) {
        if (level <= 0 || !hasVib) return
        if (prim && Build.VERSION.SDK_INT >= 30) {
            vib.vibrate(
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 110)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f, 130)
                    .compose()
            )
            return
        }
        vib.waveformCompat(longArrayOf(0, 40, 80, 40, 90, 110), intArrayOf(0, 150, 0, 190, 0, 230))
    }

    /** "Olmaz" hissi: üç kısa, yumuşak titreşim. */
    fun error(level: Int) {
        if (level <= 0 || !hasVib) return
        if (Build.VERSION.SDK_INT >= 26 && vib.hasAmplitudeControl()) {
            vib.waveformCompat(longArrayOf(0, 18, 40, 18, 40, 18), intArrayOf(0, 90, 0, 90, 0, 90))
        } else {
            vib.waveformCompat(longArrayOf(0, 15, 45, 15))
        }
    }

    /** Tane sesi (0 kehribar, 1 ahşap, 2 kristal); her vuruşta ton ve seviye hafifçe değişir, makine gibi tekrar etmez. */
    fun click(type: Int, volume: Float) {
        val i = type.coerceIn(0, 2)
        if (!ready[i] || volume <= 0f || silent()) return
        val v = (volume * (0.93f + rnd.nextFloat() * 0.07f)).coerceAtMost(1f)
        pool?.play(ids[i], v, v, 1, 0, 0.97f + rnd.nextFloat() * 0.06f)
    }

    /** Tespih çekme: önce tanenin ipte kayma sesi, hemen ardından diğer taneye çarpma tıkı. */
    fun pull(type: Int, volume: Float) {
        val i = SOUNDS.indexOfFirst { it.name == "kayma" }
        if (i >= 0 && ready[i] && volume > 0f && !silent()) {
            val v = (volume * 0.55f).coerceAtMost(1f)
            pool?.play(ids[i], v, v, 1, 0, 0.96f + rnd.nextFloat() * 0.08f)
            main.postDelayed({ click(type, volume) }, 55)
        } else click(type, volume)
    }

    /** Ara sayıda iki tanenin art arda çarpması. */
    fun doubleClick(type: Int, volume: Float) {
        click(type, volume)
        main.postDelayed({ click(type, volume * 0.9f) }, 85)
    }

    /** Hedefe ulaşınca yumuşak, iki tonlu zil. */
    fun chime(volume: Float) {
        val i = SOUNDS.lastIndex
        if (!ready[i] || volume <= 0f || silent()) return
        pool?.play(ids[i], volume, volume, 2, 0, 1f)
    }

    fun release() {
        pool?.release()
        pool = null
    }
}

// ───────────── Ses sentezi (kayıt dosyası yok; ilk açılışta üretilir, önbelleğe yazılır) ─────────────
private class SndSpec(val name: String, val seconds: Double, val gen: (t: Double, rnd: java.util.Random, state: DoubleArray) -> Double)

private const val SR = 48000

private val SOUNDS = listOf(
    // Kehribar: hafif ve sıcak; uyumsuz rezonanslar + ikinci temas + kısa sürtünme
    SndSpec("kehribar", 0.060) { t, rnd, st ->
        var v = sin(2 * PI * 2870 * t) * exp(-t * 260) * 0.40 + sin(2 * PI * 4410 * t) * exp(-t * 340) * 0.22 +
            sin(2 * PI * 6930 * t) * exp(-t * 520) * 0.12 + sin(2 * PI * 1630 * t) * exp(-t * 180) * 0.16
        val t2 = t - 0.007
        if (t2 > 0) v += sin(2 * PI * 3150 * t2) * exp(-t2 * 300) * 0.14
        st[0] += 0.35 * ((rnd.nextDouble() * 2 - 1) - st[0])
        v + st[0] * exp(-t * 900) * 0.35
    },
    // Ahşap: daha tok ve alçak
    SndSpec("ahsap", 0.075) { t, rnd, st ->
        var v = sin(2 * PI * 1150 * t) * exp(-t * 95) * 0.45 + sin(2 * PI * 2380 * t) * exp(-t * 160) * 0.20 +
            sin(2 * PI * 520 * t) * exp(-t * 60) * 0.22
        val t2 = t - 0.006
        if (t2 > 0) v += sin(2 * PI * 1320 * t2) * exp(-t2 * 120) * 0.12
        st[0] += 0.25 * ((rnd.nextDouble() * 2 - 1) - st[0])
        v + st[0] * exp(-t * 700) * 0.30
    },
    // Kristal: ince ve çınlayan
    SndSpec("kristal", 0.30) { t, _, _ ->
        sin(2 * PI * 3520 * t) * exp(-t * 18) * 0.30 + sin(2 * PI * 5280 * t) * exp(-t * 26) * 0.16 +
            sin(2 * PI * 7040 * t) * exp(-t * 40) * 0.08 + sin(2 * PI * 4980 * t) * exp(-t * 300) * 0.20
    },
    // Kayma: tane ipte kayarken çıkan kısa, yumuşak sürtünme (filtrelenmiş gürültü, frekansı yükselip alçalır)
    SndSpec("kayma", 0.090) { t, rnd, st ->
        val x = t / 0.090
        val env = sin(PI * x).let { it * it }
        val a = 0.06 + 0.30 * sin(PI * x)
        st[0] += a * ((rnd.nextDouble() * 2 - 1) - st[0])
        st[1] += a * (st[0] - st[1])
        (st[0] - st[1]) * env * 2.2
    },
    // Hedef zili: yumuşak çan (inharmonik kısmi sesler), ardından beşli aralıkta ikinci nota
    SndSpec("zil", 1.6) { t, _, _ ->
        fun bell(f: Double, tt: Double): Double = if (tt < 0) 0.0 else
            (sin(2 * PI * f * tt) * exp(-tt * 3.2) * 0.50 + sin(2 * PI * f * 2.0 * tt) * exp(-tt * 4.5) * 0.22 +
                sin(2 * PI * f * 2.76 * tt) * exp(-tt * 6.0) * 0.14 + sin(2 * PI * f * 5.4 * tt) * exp(-tt * 9.0) * 0.06) *
                (tt / 0.004).coerceAtMost(1.0)
        bell(659.25, t) * 0.75 + bell(987.77, t - 0.13) * 0.55
    },
)

/** Sesi önbelleğe yazar; yarım kalmış dosya kalmasın diye önce geçici dosyaya yazıp taşır. */
private fun ensureWav(ctx: Context, spec: SndSpec): File {
    val n = (SR * spec.seconds).toInt()
    val f = File(ctx.cacheDir, "snd_${spec.name}_v6.wav")
    if (f.length() == 44L + n * 2L) return f
    val rnd = java.util.Random(11)
    val st = DoubleArray(2)
    // Hafif sıkıştırma (tanh) ile algılanan yükseklik artar; sonra tepe seviyesi %97'ye normalize edilir
    val raw = DoubleArray(n)
    var peak = 1e-9
    for (i in 0 until n) {
        val t = i / SR.toDouble()
        val env = (i / 24.0).coerceAtMost(1.0) * ((n - i) / 240.0).coerceAtMost(1.0)
        raw[i] = tanh(spec.gen(t, rnd, st) * 1.8) * env
        if (abs(raw[i]) > peak) peak = abs(raw[i])
    }
    val gain = 0.97 / peak
    val pcm = ShortArray(n) { i -> (raw[i] * gain * 32767).toInt().coerceIn(-32767, 32767).toShort() }
    val b = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
    b.put("RIFF".toByteArray()); b.putInt(36 + n * 2); b.put("WAVE".toByteArray())
    b.put("fmt ".toByteArray()); b.putInt(16); b.putShort(1.toShort()); b.putShort(1.toShort())
    b.putInt(SR); b.putInt(SR * 2); b.putShort(2.toShort()); b.putShort(16.toShort())
    b.put("data".toByteArray()); b.putInt(n * 2)
    pcm.forEach { b.putShort(it) }
    val tmp = File(ctx.cacheDir, "snd.tmp")
    tmp.writeBytes(b.array())
    if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
    return f
}
