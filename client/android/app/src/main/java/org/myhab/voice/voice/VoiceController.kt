package org.myhab.voice.voice

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.myhab.voice.data.Session
import org.myhab.voice.net.UnauthorizedException
import org.myhab.voice.net.VoiceApi

/**
 * App-scoped orchestrator of a voice turn, mirroring the web client's flow:
 *
 *   (wake | tap) → listen → transcript → voiceCommand → speak reply
 *                → if awaitingReply: listen again (no wake word)  → else finish
 *
 * Drives [SpeechRecognizerController] and [ReplyPlayer], exposes [status] and the
 * [log] for Compose, and coordinates with the wake-word service via [WakeControl]
 * so the microphone is never contended (the wake engine is paused for the
 * duration of a turn and resumed at the end). All state mutation happens on the
 * main thread.
 */
object VoiceController {

    enum class Status { IDLE, WAKE, LISTENING, THINKING, SPEAKING }

    data class Exchange(
        val user: String? = null,
        val assistant: String? = null,
        val actions: List<String> = emptyList(),
        val isError: Boolean = false
    )

    /** Implemented by [WakeWordService] so a turn can free/reclaim the mic. */
    interface WakeControl {
        fun pause()
        fun resume()
    }

    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var stt: SpeechRecognizerController? = null
    private var player: ReplyPlayer? = null

    @Volatile
    var wakeControl: WakeControl? = null

    private val _status = MutableStateFlow(Status.IDLE)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _log = MutableStateFlow<List<Exchange>>(emptyList())
    val log: StateFlow<List<Exchange>> = _log.asStateFlow()

    @Volatile
    private var busy = false

    /** Timestamp when listening started, for STT-duration timing logs. */
    private var tListen = 0L
    private var tone: ToneGenerator? = null

    private const val TAG = "Voice"

    fun init(context: Context) {
        app = context.applicationContext
    }

    /** Clears the multi-turn conversation and the on-screen exchange log. */
    fun newConversation() {
        Session.conversationId = null
        _log.value = emptyList()
    }

    /** Entry point for both the wake word and the manual tap-to-talk button. */
    fun beginTurn() = scope.launch {
        if (busy) return@launch
        busy = true
        wakeControl?.pause()
        listenOnce()
    }

    // ---- turn steps ---------------------------------------------------------

    private fun listenOnce() {
        _status.value = Status.LISTENING
        earcon()
        tListen = SystemClock.elapsedRealtime()
        ensureStt().start(Session.speechTag(), object : SpeechRecognizerController.Callbacks {
            override fun onResult(text: String) = onTranscript(text)
            override fun onError(message: String) {
                appendError(message)
                finishTurn()
            }
        })
    }

    /** Short beep when listening opens, so the STT wait doesn't feel like dead air. */
    private fun earcon() {
        try {
            val t = tone ?: ToneGenerator(AudioManager.STREAM_MUSIC, 60).also { tone = it }
            t.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Exception) {
            // Tone resources may be unavailable; not worth failing the turn over.
        }
    }

    private fun onTranscript(text: String) {
        val sttMs = SystemClock.elapsedRealtime() - tListen
        appendUser(text)
        _status.value = Status.THINKING
        scope.launch {
            val tServer = SystemClock.elapsedRealtime()
            val result = try {
                VoiceApi.voiceCommand(text)
            } catch (e: UnauthorizedException) {
                appendError("Session expired — please sign in again")
                Session.logout() // flips loggedIn → MainActivity routes to Login
                finishTurn()
                return@launch
            } catch (e: Exception) {
                appendError(e.message ?: "Could not reach the server")
                finishTurn()
                return@launch
            }

            Log.d(TAG, "timing: stt=${sttMs}ms server=${SystemClock.elapsedRealtime() - tServer}ms")
            if (!result.success && !result.error.isNullOrBlank()) {
                appendError(result.error)
            } else {
                appendAssistant(result.spokenResponse, result.actions ?: emptyList())
            }

            _status.value = Status.SPEAKING
            ensurePlayer().play(result, Session.speechTag(), Session.speakerEnabled.value) {
                if (result.awaitingReply) listenOnce() // auto-continue, skip wake word
                else finishTurn()
            }
        }
    }

    private fun finishTurn() {
        busy = false
        _status.value = Status.IDLE
        wakeControl?.resume()
    }

    /** Called by the wake-word service to reflect "listening for Hey myHAB". */
    fun reportWakeIdle() {
        if (!busy) _status.value = Status.WAKE
    }

    fun reportStopped() {
        if (!busy) _status.value = Status.IDLE
    }

    // ---- log helpers --------------------------------------------------------

    private fun appendUser(text: String) {
        _log.value = _log.value + Exchange(user = text)
    }

    private fun appendAssistant(text: String?, actions: List<String>) {
        _log.value = _log.value + Exchange(assistant = text, actions = actions)
    }

    private fun appendError(text: String) {
        _log.value = _log.value + Exchange(assistant = text, isError = true)
    }

    private fun ensureStt(): SpeechRecognizerController =
        stt ?: SpeechRecognizerController(app).also { stt = it }

    private fun ensurePlayer(): ReplyPlayer =
        player ?: ReplyPlayer(app).also { player = it }
}
