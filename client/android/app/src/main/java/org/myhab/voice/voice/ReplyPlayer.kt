package org.myhab.voice.voice

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.util.Log
import org.myhab.voice.net.VoiceResult
import java.io.File
import java.util.Locale

/**
 * Speaks the assistant reply. Prefers the server's neural MP3 ([VoiceResult.audioContent],
 * base64) played via [MediaPlayer]; falls back to the on-device [TextToSpeech]
 * engine when no audio was returned or playback fails. [onDone] is always called
 * exactly once on the main thread so the caller can continue the turn.
 *
 * Routing note: on the One UI / Android 16 test device, the modern `AudioAttributes`
 * path (USAGE_MEDIA) mis-routed the reply to the earpiece while the earcon beep —
 * which uses the *legacy* `STREAM_MUSIC` path — played on the loudspeaker. So the
 * MP3 uses the identical legacy path ([MediaPlayer.setAudioStreamType]) for parity.
 * (Media volume is left to the user — adjust it with the device's volume keys.)
 *
 * When `toSpeaker` is set, MP3 playback is also pinned to the built-in loudspeaker
 * even if a Bluetooth/wired device is connected (handy for a docked, hands-free unit).
 */
class ReplyPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var media: MediaPlayer? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }

    fun play(result: VoiceResult, languageTag: String, toSpeaker: Boolean, onDone: () -> Unit) {
        val once = onceOnMain(onDone)
        val audio = result.audioContent
        if (!audio.isNullOrBlank() && playMp3(audio, toSpeaker, once)) return
        speak(result.spokenResponse, languageTag, once)
    }

    fun stop() {
        releaseMedia()
        tts?.stop()
    }

    fun release() {
        releaseMedia()
        tts?.shutdown()
        tts = null
    }

    // ---- internals ----------------------------------------------------------

    private fun playMp3(base64: String, toSpeaker: Boolean, onDone: () -> Unit): Boolean = try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val tmp = File.createTempFile("reply", ".mp3", appContext.cacheDir)
        tmp.writeBytes(bytes)
        releaseMedia()
        val mp = MediaPlayer()
        media = mp
        // Use the SAME legacy audio path as the earcon beep (ToneGenerator on
        // STREAM_MUSIC), which plays loud on the loudspeaker. The modern
        // AudioAttributes path mis-routed to the earpiece on this device.
        @Suppress("DEPRECATION")
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mp.setDataSource(tmp.absolutePath)
        mp.setOnCompletionListener {
            releaseMedia(); tmp.delete(); onDone()
        }
        mp.setOnErrorListener { _, _, _ ->
            releaseMedia(); tmp.delete(); onDone(); true
        }
        mp.prepare() // local file → cheap, synchronous
        // Force speaker over a connected headset/BT only when the user asked for it.
        if (toSpeaker) builtInSpeaker()?.let { mp.setPreferredDevice(it) }
        mp.start()
        true
    } catch (e: Exception) {
        Log.w(TAG, "playMp3 failed", e)
        releaseMedia()
        false
    }

    /** The phone's built-in loudspeaker output device, or null if unavailable. */
    private fun builtInSpeaker(): AudioDeviceInfo? =
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

    private fun speak(text: String?, languageTag: String, onDone: () -> Unit) {
        val engine = tts
        if (text.isNullOrBlank() || engine == null || !ttsReady) {
            onDone(); return
        }
        engine.language = Locale.forLanguageTag(languageTag)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) = onDone()
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = onDone()
            override fun onError(utteranceId: String?, errorCode: Int) = onDone()
        })
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reply")
    }

    private fun releaseMedia() {
        media?.let {
            runCatching { it.reset() }
            it.release()
        }
        media = null
    }

    /** Guarantees [block] runs once, on the main thread. */
    private fun onceOnMain(block: () -> Unit): () -> Unit {
        var fired = false
        return {
            if (!fired) {
                fired = true
                main.post(block)
            }
        }
    }

    private companion object {
        const val TAG = "VoiceReply"
    }
}
