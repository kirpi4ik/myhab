package org.myhab.voice.voice

import android.content.Context
import android.content.res.AssetManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.homeassistant.companion.android.microwakeword.MicroWakeWord
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Captures 16 kHz mono PCM and streams it into the vendored [MicroWakeWord]
 * native engine (Home Assistant's microWakeWord: real TF microfrontend +
 * TFLite-Micro via JNI). Reads `wake_word.json` + its `.tflite` from assets, and
 * invokes [start]'s callback on the main thread when the wake word fires.
 *
 * @throws IllegalStateException if the model assets are missing/invalid or the
 *         native engine fails to initialize.
 */
class WakeWordEngine(context: Context) {

    @Serializable
    private data class Config(
        val wake_word: String = "wake word",
        val model: String,
        val micro: Micro = Micro()
    ) {
        @Serializable
        data class Micro(
            val probability_cutoff: Float = 0.95f,
            val feature_step_size: Int = 10,
            val sliding_window_size: Int = 5
        )
    }

    private val appContext = context.applicationContext
    private val config: Config = loadConfig(appContext.assets)

    // The native engine references this flatbuffer for its lifetime — keep it
    // alive (direct buffer, not GC-moved) as long as [detector] exists.
    private val modelBuffer: ByteBuffer = loadModelDirect(appContext.assets, config.model)
    private val detector = MicroWakeWord(
        modelBuffer = modelBuffer,
        featureStepSizeMs = config.micro.feature_step_size,
        probabilityCutoff = config.micro.probability_cutoff,
        slidingWindowSize = config.micro.sliding_window_size
    )

    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var running = false
    private var worker: Thread? = null

    fun start(onDetected: () -> Unit) {
        if (running) return
        running = true
        detector.reset()
        worker = Thread {
            runCatching { loop(onDetected) }
                .onFailure { Log.e(TAG, "Wake loop failed", it) }
        }.also { it.start() }
    }

    fun stop() {
        running = false
        worker?.join(500)
        worker = null
    }

    fun close() {
        stop()
        detector.close()
    }

    private fun loop(onDetected: () -> Unit) {
        val minBuf = AudioRecord.getMinBufferSize(
            16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, CHUNK * 4)
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("AudioRecord could not initialize")
        }
        Log.d(TAG, "Wake engine listening for '${config.wake_word}'")
        val chunk = ShortArray(CHUNK)
        try {
            record.startRecording()
            while (running) {
                val n = record.read(chunk, 0, chunk.size)
                if (n <= 0) continue
                val frame = if (n == chunk.size) chunk else chunk.copyOf(n)
                if (detector.processAudio(frame)) {
                    detector.reset()
                    main.post(onDetected)
                }
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }

    private fun loadConfig(assets: AssetManager): Config {
        val text = try {
            assets.open(WAKE_CONFIG_ASSET).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            throw IllegalStateException("Missing wake-word config asset '$WAKE_CONFIG_ASSET'")
        }
        return try {
            JSON.decodeFromString(Config.serializer(), text)
        } catch (e: Exception) {
            throw IllegalStateException("Invalid '$WAKE_CONFIG_ASSET': ${e.message}")
        }
    }

    private fun loadModelDirect(assets: AssetManager, name: String): ByteBuffer {
        val bytes = try {
            assets.open(name).use { it.readBytes() }
        } catch (e: Exception) {
            throw IllegalStateException("Missing wake-word model asset '$name'")
        }
        return ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply {
            put(bytes)
            rewind()
        }
    }

    companion object {
        private const val TAG = "MicroWakeWord"

        /** Companion JSON describing the bundled model (names the .tflite, cutoffs, step). */
        const val WAKE_CONFIG_ASSET = "wake_word.json"

        /** ~30 ms of audio per read; the native engine buffers feature frames internally. */
        private const val CHUNK = 480

        private val JSON = Json { ignoreUnknownKeys = true }
    }
}
