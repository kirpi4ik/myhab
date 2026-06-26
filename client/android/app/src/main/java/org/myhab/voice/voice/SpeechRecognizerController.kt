package org.myhab.voice.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Thin wrapper over the framework [SpeechRecognizer]. One-shot: [start] kicks off
 * a single utterance and reports the best transcript (or an error) exactly once.
 * Must be driven from the main thread (the platform requirement).
 *
 * Recognition is attempted **on-device first** (works with no internet) and, if the
 * device has no offline model for the language, automatically **falls back to
 * online** recognition once. So it succeeds whether the phone has the offline pack
 * or just internet, with no configuration.
 */
class SpeechRecognizerController(context: Context) {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null

    interface Callbacks {
        fun onResult(text: String)
        fun onError(message: String)
    }

    fun start(languageTag: String, cb: Callbacks) {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            cb.onError("Speech recognition is not available on this device")
            return
        }
        startInternal(languageTag, cb, preferOffline = true)
    }

    private fun startInternal(languageTag: String, cb: Callbacks, preferOffline: Boolean) {
        destroy()
        // The installed on-device language packs belong to the *on-device*
        // recognition service, reachable only via createOnDeviceSpeechRecognizer
        // (API 31+). The default recognizer is online-oriented and reports
        // installed offline languages as "not supported".
        val onDevice = preferOffline && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val r = if (onDevice) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
        } else {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        }
        Log.d(TAG, "Listening lang=$languageTag onDevice=$onDevice")
        recognizer = r

        var delivered = false
        // Force the recognizer to finalize if its end-of-speech detection never
        // fires (online recognition can otherwise hang for 20–30s).
        val watchdog = Runnable {
            if (!delivered) runCatching { recognizer?.stopListening() }
        }
        main.postDelayed(watchdog, MAX_LISTEN_MS)

        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                if (delivered) return
                delivered = true
                main.removeCallbacks(watchdog)
                // Free the mic now: a lingering recognizer session forces the reply
                // audio to the earpiece on some devices. Posted so we don't destroy
                // from inside the recognizer's own callback.
                main.post { destroy() }
                val text = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                if (text.isNullOrEmpty()) cb.onError("I didn't catch that") else cb.onResult(text)
            }

            override fun onError(error: Int) {
                if (delivered) return
                delivered = true
                main.removeCallbacks(watchdog)
                Log.w(TAG, "SpeechRecognizer error code=$error (preferOffline=$preferOffline)")
                // If the on-device pass fails for any reason other than "heard you
                // but no match", fall back to online once. (NO_MATCH/TIMEOUT mean it
                // listened fine — retrying online would just relisten.)
                val heardButEmpty = error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                if (preferOffline && !heardButEmpty) {
                    Log.d(TAG, "On-device STT failed (code=$error); retrying online")
                    main.post { startInternal(languageTag, cb, preferOffline = false) }
                } else {
                    cb.onError(mapError(error))
                }
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Some recognizers reject requests without the calling package.
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            // Finalize sooner after the user stops (commands are short); cuts the
            // recognizer's trailing-silence wait. Best-effort — not all engines honor it.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
        }
        r.startListening(intent)
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun mapError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't catch that"
        SpeechRecognizer.ERROR_AUDIO -> "Microphone error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "No internet for online recognition. Install an on-device language: " +
                "Settings → System → Languages & input → On-device speech recognition."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy, try again"
        ERROR_LANGUAGE_NOT_SUPPORTED,
        ERROR_LANGUAGE_UNAVAILABLE ->
            "Language unavailable. Install it in Settings → System → Languages & " +
                "input → On-device speech recognition, or connect to the internet."
        ERROR_CLIENT -> "Recognizer client error ($error)"
        ERROR_SERVER -> "Recognizer server error ($error)"
        else -> "Speech recognition failed (code $error)"
    }

    private companion object {
        const val TAG = "VoiceStt"
        /** Cap on a single listen; commands are short, so force finalize past this. */
        const val MAX_LISTEN_MS = 10_000L
        // Numeric SpeechRecognizer error codes (avoids API-level constant gating).
        const val ERROR_CLIENT = 5
        const val ERROR_SERVER = 4
        const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
        const val ERROR_LANGUAGE_UNAVAILABLE = 13
    }
}
