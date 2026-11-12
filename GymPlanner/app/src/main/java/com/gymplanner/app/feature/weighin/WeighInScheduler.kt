package com.gymplanner.app.feature.weighin

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.gymplanner.app.core.data.Profile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object WeighInTimes {
    /** Bit 0 = Monday … bit 6 = Sunday. */
    fun isDayOn(mask: Int, date: LocalDate) = mask and (1 shl (date.dayOfWeek.value - 1)) != 0

    /** The next prompt time strictly after [now], or null if no day is switched on. */
    fun next(now: LocalDateTime, minuteOfDay: Int, mask: Int): LocalDateTime? {
        if (mask and 0x7F == 0) return null
        for (i in 0..7) {
            val d = now.toLocalDate().plusDays(i.toLong())
            val t = d.atTime(minuteOfDay / 60, minuteOfDay % 60)
            if (t.isAfter(now) && isDayOn(mask, d)) return t
        }
        return null
    }

    fun label(minuteOfDay: Int): String = String.format(java.util.Locale.US, "%d:%02d", minuteOfDay / 60, minuteOfDay % 60)
}

/** Sets (or clears) the alarm for each profile's morning weigh-in. */
object WeighInScheduler {
    const val ACTION_PROMPT = "com.gymplanner.app.WEIGH_IN"
    const val EXTRA_PROFILE = "profileId"
    const val EXTRA_SNOOZE = "snooze"
    /** Opened from Settings to try it: show even if a weight is already logged today. */
    const val EXTRA_PREVIEW = "preview"

    private fun pending(context: Context, profileId: Long, snooze: Boolean): PendingIntent {
        val intent = Intent(context, WeighInReceiver::class.java)
            .setAction(ACTION_PROMPT)
            .putExtra(EXTRA_PROFILE, profileId)
            .putExtra(EXTRA_SNOOZE, snooze)
        val code = (profileId * 2 + if (snooze) 1 else 0).toInt()
        return PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun schedule(context: Context, profile: Profile) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context, profile.id, snooze = false)
        am.cancel(pi)
        if (!profile.weighInEnabled) return
        val at = WeighInTimes.next(LocalDateTime.now(), profile.weighInTime, profile.weighInDays) ?: return
        set(am, at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), pi)
    }

    fun snooze(context: Context, profileId: Long, minutes: Int = 10) {
        val am = context.getSystemService(AlarmManager::class.java)
        set(am, System.currentTimeMillis() + minutes * 60_000L, pending(context, profileId, snooze = true))
    }

    fun cancelSnooze(context: Context, profileId: Long) =
        context.getSystemService(AlarmManager::class.java).cancel(pending(context, profileId, snooze = true))

    fun cancel(context: Context, profileId: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(pending(context, profileId, snooze = false))
        am.cancel(pending(context, profileId, snooze = true))
    }

    private fun set(am: AlarmManager, atMillis: Long, pi: PendingIntent) {
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
    }
}
