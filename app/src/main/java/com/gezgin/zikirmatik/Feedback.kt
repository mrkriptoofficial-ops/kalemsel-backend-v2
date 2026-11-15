package com.gezgin.zikirmatik

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

fun Context.systemVibrator(): Vibrator =
    if (Build.VERSION.SDK_INT >= 31) {
        getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

/** Katmanlı titreşim (destekleyen cihazlarda ince ayarlı haptik primitifler) + sentezlenmiş tespih tanesi sesi. */
class Feedback(ctx: Context) {
    private val vib = ctx.systemVibrator()
    private val hasVib = vib.hasVibrator()
    private val prim = Build.VERSION.SDK_INT >= 30 && hasVib && vib.areAllPrimitivesSupported(
        VibrationEffect.Composition.PRIMITIVE_CLICK, VibrationEffect.Composition.PRIMITIVE_TICK,
    )
    private val rnd = java.util.Random()
    private var pool: SoundPool? = null
    private var soundId = 0
    @Volatile private var loaded = false

    private val tapFx: Array<VibrationEffect?> = arrayOf(
        null,
        fx(VibrationEffect.Composition.PRIMITIVE_TICK, 0.75f, VibrationEffect.EFFECT_TICK),
        fx(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f, VibrationEffect.EFFECT_CLICK),
        fx(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f, VibrationEffect.EFFECT_HEAVY_CLICK),
    )

    init {
        runCatching {
            val f = File(ctx.cacheDir, "tane_v3.wav")
            // Yarım yazılmış dosya kalmasın: önce geçici dosyaya yaz, sonra taşı
            if (f.length() != 44L + 2880L * 2L) {
                val tmp = File(ctx.cacheDir, "tane.tmp")
                tmp.writeBytes(beadWav())
                if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
            }
            val sp = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                ).build()
            sp.setOnLoadCompleteListener { _, _, status -> loaded = status == 0 }
            soundId = sp.load(f.path, 1)
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
        if (e != null) vib.vibrate(e)
        else vib.vibrate(VibrationEffect.createOneShot(longArrayOf(0, 8, 14, 22)[l], 255))
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
            else -> vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 25, 60, 25), -1))
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
        val t = longArrayOf(0, 40, 80, 40, 90, 110)
        if (vib.hasAmplitudeControl()) vib.vibrate(VibrationEffect.createWaveform(t, intArrayOf(0, 150, 0, 190, 0, 230), -1))
        else vib.vibrate(VibrationEffect.createWaveform(t, -1))
    }

    /** "Olmaz" hissi: üç kısa, yumuşak titreşim. */
    fun error(level: Int) {
        if (level <= 0 || !hasVib) return
        if (vib.hasAmplitudeControl()) {
            vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 18, 40, 18, 40, 18), intArrayOf(0, 90, 0, 90, 0, 90), -1))
        } else {
            vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 15, 45, 15), -1))
        }
    }

    /** Her vuruşta tonu ve sesi hafifçe değişen tane sesi (makine gibi tekrar etmesin). */
    fun click(enabled: Boolean) {
        if (!enabled || !loaded) return
        val v = 0.45f + rnd.nextFloat() * 0.1f
        pool?.play(soundId, v, v, 1, 0, 0.97f + rnd.nextFloat() * 0.06f)
    }

    fun release() {
        pool?.release()
        pool = null
    }
}

/** Sert bir tespih tanesinin diğerine çarpması: uyumsuz rezonanslar + ikinci temas + kısa sürtünme gürültüsü. */
private fun beadWav(): ByteArray {
    val sr = 48000
    val n = (sr * 0.060).toInt()
    val rnd = java.util.Random(11)
    val pcm = ShortArray(n)
    val modes = arrayOf(
        doubleArrayOf(2870.0, 260.0, 0.40),
        doubleArrayOf(4410.0, 340.0, 0.22),
        doubleArrayOf(6930.0, 520.0, 0.12),
        doubleArrayOf(1630.0, 180.0, 0.16),
    )
    var lp = 0.0
    for (i in 0 until n) {
        val t = i / sr.toDouble()
        var v = 0.0
        for (m in modes) v += sin(2 * PI * m[0] * t) * exp(-t * m[1]) * m[2]
        val t2 = t - 0.007
        if (t2 > 0) v += sin(2 * PI * 3150 * t2) * exp(-t2 * 300) * 0.14
        lp += 0.35 * ((rnd.nextDouble() * 2 - 1) - lp)
        v += lp * exp(-t * 900) * 0.35
        val env = (i / 24.0).coerceAtMost(1.0) * ((n - i) / 240.0).coerceAtMost(1.0)
        pcm[i] = (tanh(v * 1.2) * env * 32767 * 0.8).toInt().toShort()
    }
    val b = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
    b.put("RIFF".toByteArray()); b.putInt(36 + n * 2); b.put("WAVE".toByteArray())
    b.put("fmt ".toByteArray()); b.putInt(16); b.putShort(1.toShort()); b.putShort(1.toShort())
    b.putInt(sr); b.putInt(sr * 2); b.putShort(2.toShort()); b.putShort(16.toShort())
    b.put("data".toByteArray()); b.putInt(n * 2)
    pcm.forEach { b.putShort(it) }
    return b.array()
}
