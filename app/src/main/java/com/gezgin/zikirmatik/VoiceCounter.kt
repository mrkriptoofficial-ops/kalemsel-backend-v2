package com.gezgin.zikirmatik

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Sesli sayma: sürekli dinler, her sonuç metnini [onSpoken] ile verir.
 * Yalnızca uygulama ön plandayken çalışır (MainActivity ON_PAUSE/ON_RESUME ile durdurulur).
 */
class VoiceCounter(private val ctx: Context, private val onSpoken: (String) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private var rec: SpeechRecognizer? = null
    private var running = false

    fun start() {
        if (running || !SpeechRecognizer.isRecognitionAvailable(ctx)) return
        running = true
        rec = SpeechRecognizer.createSpeechRecognizer(ctx).also { it.setRecognitionListener(listener) }
        listen()
    }

    fun stop() {
        running = false
        main.removeCallbacksAndMessages(null)
        rec?.cancel()
        rec?.destroy()
        rec = null
    }

    private fun listen() {
        if (!running) return
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
        rec?.startListening(i)
    }

    private fun again(delay: Long) {
        main.postDelayed({ listen() }, delay)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (text.isNotEmpty()) onSpoken(text)
            again(150)
        }

        override fun onError(error: Int) {
            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) { stop(); return }
            // Sessizlik ve meşgul hataları normal; dinlemeye devam et
            again(400)
        }
    }
}
