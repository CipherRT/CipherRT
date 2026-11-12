package com.gymplanner.app.feature.weighin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.gymplanner.app.GymApp
import com.gymplanner.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Fires at weigh-in time. Shows the full-screen prompt unless a weight was already
 * logged today, then sets tomorrow's alarm. Also re-sets alarms after a reboot or a
 * clock change.
 */
class WeighInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as GymApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val c = app.container
                when (intent.action) {
                    WeighInScheduler.ACTION_PROMPT -> {
                        val profileId = intent.getLongExtra(WeighInScheduler.EXTRA_PROFILE, -1)
                        val profile = c.profiles.get(profileId)
                        if (profile != null) {
                            val today = LocalDate.now()
                            val logged = c.weights.latest(profileId)?.date == today.toEpochDay()
                            val snooze = intent.getBooleanExtra(WeighInScheduler.EXTRA_SNOOZE, false)
                            val dayOn = snooze || WeighInTimes.isDayOn(profile.weighInDays, today)
                            if (profile.weighInEnabled && dayOn && !logged) showPrompt(context, profile.id, profile.name)
                            if (!snooze) WeighInScheduler.schedule(context, profile)
                        }
                    }
                    else -> c.profiles.profiles.first().forEach { WeighInScheduler.schedule(context, it) }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val CHANNEL = "weigh_in"

        fun notificationId(profileId: Long) = 2000 + profileId.toInt()

        fun showPrompt(context: Context, profileId: Long, name: String) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Morning weigh-in", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "A full-screen reminder to step on the scale"
                }
            )
            val open = Intent(context, WeighInActivity::class.java)
                .putExtra(WeighInScheduler.EXTRA_PROFILE, profileId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pi = PendingIntent.getActivity(
                context, profileId.toInt(), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            // Full-screen over the lock screen when allowed; otherwise Android shows a
            // normal high-priority notification instead.
            val n: Notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_scale)
                .setContentTitle("Good morning, $name")
                .setContentText("Step on the scale and log today’s weight.")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setFullScreenIntent(pi, true)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setTimeoutAfter(2 * 60 * 60 * 1000L)
                .build()
            runCatching { nm.notify(notificationId(profileId), n) }
        }

        fun dismiss(context: Context, profileId: Long) =
            context.getSystemService(NotificationManager::class.java).cancel(notificationId(profileId))
    }
}
