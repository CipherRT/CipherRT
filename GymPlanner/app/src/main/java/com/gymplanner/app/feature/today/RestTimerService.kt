package com.gymplanner.app.feature.today

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.gymplanner.app.MainActivity
import com.gymplanner.app.R

/**
 * Foreground service for the rest timer: shows a live countdown notification, keeps
 * the CPU awake just long enough, and vibrates when the rest is over — even with the
 * screen off.
 */
class RestTimerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private val onDone = Runnable { finish() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_ADD_30 -> if (RestTimer.state.value == null) stop() else RestTimer.add(this, 30)
            ACTION_SKIP -> if (RestTimer.state.value == null) stop() else RestTimer.stop(this)
            ACTION_STOP -> stop()
        }
        return START_NOT_STICKY
    }

    private fun start() {
        createChannels(this)
        val state = RestTimer.state.value
        // startForeground must always be called after startForegroundService, even if we stop right away.
        ServiceCompat.startForeground(
            this, NOTIF_ID, countdownNotification(state ?: RestState(System.currentTimeMillis(), 1, "")),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        if (state == null) { stop(); return }
        handler.removeCallbacks(onDone)
        val remaining = state.remainingMs()
        handler.postDelayed(onDone, remaining)
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GymPlanner:rest")
            .apply { acquire(remaining + 5_000) }
    }

    private fun finish() {
        val label = RestTimer.state.value?.label
        RestTimer.finished()
        vibrate()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(
            DONE_ID,
            NotificationCompat.Builder(this, CHANNEL_DONE)
                .setSmallIcon(R.drawable.ic_stat_timer)
                .setContentTitle("Rest over — time for your next set")
                .setContentText(label ?: "")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setTimeoutAfter(60_000)
                .setContentIntent(openApp())
                .build()
        )
        stop()
    }

    private fun stop() {
        handler.removeCallbacks(onDone)
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(onDone)
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    private fun countdownNotification(state: RestState): Notification =
        NotificationCompat.Builder(this, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Resting")
            .setContentText(state.label)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(state.endAt)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp())
            .addAction(0, "+30 s", action(ACTION_ADD_30))
            .addAction(0, "Skip", action(ACTION_SKIP))
            .build()

    private fun action(name: String): PendingIntent = PendingIntent.getService(
        this, name.hashCode(), Intent(this, RestTimerService::class.java).setAction(name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 600), -1)
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }

    companion object {
        const val ACTION_START = "rest.start"
        const val ACTION_STOP = "rest.stop"
        const val ACTION_ADD_30 = "rest.add30"
        const val ACTION_SKIP = "rest.skip"
        private const val CHANNEL_TIMER = "rest_timer"
        private const val CHANNEL_DONE = "rest_done"
        private const val NOTIF_ID = 1001
        private const val DONE_ID = 1002

        fun createChannels(context: Context) {
            val nm = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_TIMER, "Rest timer", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Countdown while you rest between sets"
                    setShowBadge(false)
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_DONE, "Rest finished", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Alerts you when it’s time for the next set"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                }
            )
        }
    }
}
