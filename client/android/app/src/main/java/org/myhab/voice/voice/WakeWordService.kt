package org.myhab.voice.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.myhab.voice.MainActivity
import org.myhab.voice.R
import org.myhab.voice.data.Session

/**
 * Always-on wake-word listener. Runs as a foreground service (type microphone) so
 * Android keeps it alive with the screen off, holding a [WakeWordEngine] that fires
 * on the bundled wake word. On detection it hands off to [VoiceController.beginTurn];
 * it implements [VoiceController.WakeControl] so the controller can pause the engine
 * (free the mic) during a turn and resume after.
 *
 * Detection runs on-device via the vendored microWakeWord native module — no access
 * key, fully offline. The model files ([WakeWordEngine.WAKE_CONFIG_ASSET] + its
 * .tflite) must be in src/main/assets/. If they're missing the service Toasts the
 * reason, flips the wake toggle off and stops; tap-to-talk still works regardless.
 */
class WakeWordService : Service(), VoiceController.WakeControl {

    private var engine: WakeWordEngine? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundNotification()
        VoiceController.wakeControl = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (engine == null && !startEngine()) {
            stopSelf()
            return START_NOT_STICKY
        }
        VoiceController.reportWakeIdle()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        VoiceController.wakeControl = null
        engine?.close()
        engine = null
        VoiceController.reportStopped()
        super.onDestroy()
    }

    // ---- WakeControl --------------------------------------------------------

    /** Free the mic so [SpeechRecognizerController] can use it during a turn. */
    override fun pause() {
        runCatching { engine?.stop() }
    }

    /** Reclaim the mic after a turn, unless the user disabled wake word meanwhile. */
    override fun resume() {
        if (!Session.wakeEnabled.value) return
        runCatching { engine?.start { VoiceController.beginTurn() } }
            .onSuccess { VoiceController.reportWakeIdle() }
    }

    // ---- engine -------------------------------------------------------------

    private fun startEngine(): Boolean = try {
        val mww = WakeWordEngine(applicationContext)
        mww.start { VoiceController.beginTurn() }
        engine = mww
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to start wake word engine", e)
        toast("Wake word unavailable: ${e.message}")
        Session.setWakeEnabled(false)
        engine = null
        false
    }

    // ---- notification -------------------------------------------------------

    private fun startForegroundNotification() {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.wake_notification_title))
            .setContentText(getString(R.string.wake_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun createChannel() {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.wake_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        mgr.createNotificationChannel(channel)
    }

    private fun toast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "WakeWordService"
        private const val CHANNEL_ID = "wake_word"
        private const val NOTIF_ID = 1001

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, WakeWordService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WakeWordService::class.java))
        }
    }
}
