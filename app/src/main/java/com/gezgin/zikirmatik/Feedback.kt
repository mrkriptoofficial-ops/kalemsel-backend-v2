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

fun Context.systemVibrator(): Vibrator =
    if (Build.VERSION.SDK_INT >= 31) {
        getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

/** Katmanlı titreşim + sentezlenmiş tespih tanesi sesi. */
class Feedback(ctx: Context) {
    private val vib = ctx.systemVibrator()
    private var pool: SoundPool? = null
    private var soundId = 0
    @Volatile private var loaded = false

    init {
        runCatching {
            val f = File(ctx.cacheDir, "tane_v2.wav")
            if (!f.exists()) f.writeBytes(beadWav())
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

    fun tap(level: Int) = when (level) {
        0 -> Unit
        1 -> predefined(VibrationEffect.EFFECT_TICK, 8, 70)
        2 -> predefined(VibrationEffect.EFFECT_CLICK, 14, 150)
        else -> predefined(VibrationEffect.EFFECT_HEAVY_CLICK, 22, 255)
    }

    fun milestone(level: Int) {
        if (level == 0 || !vib.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 29) vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        else vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 25, 60, 25), -1))
    }

    fun complete(level: Int) {
        if (level == 0 || !vib.hasVibrator()) return
        val t = longArrayOf(0, 55, 70, 55, 70, 240)
        if (vib.hasAmplitudeControl()) {
            val a = intArrayOf(0, 170, 0, 200, 0, 255)
            vib.vibrate(VibrationEffect.createWaveform(t, a, -1))
        } else {
            vib.vibrate(VibrationEffect.createWaveform(t, -1))
        }
    }

    fun error(level: Int) {
        if (level == 0 || !vib.hasVibrator()) return
        vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 50, 30), -1))
    }

    fun click(enabled: Boolean) {
        if (enabled && loaded) pool?.play(soundId, 0.5f, 0.5f, 1, 0, 1f)
    }

    fun release() {
        pool?.release()
        pool = null
    }

    private fun predefined(effect: Int, ms: Long, amp: Int) {
        if (!vib.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 29) vib.vibrate(VibrationEffect.createPredefined(effect))
        else vib.vibrate(VibrationEffect.createOneShot(ms, amp))
    }
}

/** Kehribar tespih tanesinin birbirine çarpma sesini taklit eden kısa, yumuşak bir tık. */
private fun beadWav(): ByteArray {
    val sr = 44100
    val n = (sr * 0.075).toInt()
    val rnd = java.util.Random(11)
    val pcm = ShortArray(n)
    for (i in 0 until n) {
        val t = i / sr.toDouble()
        val v = sin(2 * PI * 2350 * t) * exp(-t * 150) * 0.42 +
            sin(2 * PI * 3720 * t) * exp(-t * 220) * 0.18 +
            sin(2 * PI * 1180 * t) * exp(-t * 95) * 0.28 +
            sin(2 * PI * 210 * t) * exp(-t * 70) * 0.22 +
            (rnd.nextDouble() * 2 - 1) * exp(-t * 520) * 0.20
        val fadeIn = (i / 40.0).coerceAtMost(1.0)
        pcm[i] = (v.coerceIn(-1.0, 1.0) * fadeIn * 32767 * 0.85).toInt().toShort()
    }
    val b = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
    b.put("RIFF".toByteArray()); b.putInt(36 + n * 2); b.put("WAVE".toByteArray())
    b.put("fmt ".toByteArray()); b.putInt(16); b.putShort(1.toShort()); b.putShort(1.toShort())
    b.putInt(sr); b.putInt(sr * 2); b.putShort(2.toShort()); b.putShort(16.toShort())
    b.put("data".toByteArray()); b.putInt(n * 2)
    pcm.forEach { b.putShort(it) }
    return b.array()
}
