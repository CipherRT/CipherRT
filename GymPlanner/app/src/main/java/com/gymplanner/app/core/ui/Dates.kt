package com.gymplanner.app.core.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object Dates {
    // Getters (not constants) so a language change while the app runs is picked up.
    private val monthDay get() = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    private val monthDayYear get() = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    private val shortMonthYear get() = DateTimeFormatter.ofPattern("MMM yy", Locale.getDefault())
    private val time get() = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    fun today(): Long = LocalDate.now().toEpochDay()

    fun toLocalDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    /** "Today", "Yesterday", "Monday", "Nov 3", or "Nov 3, 2025". */
    fun relative(epochDay: Long): String {
        val d = LocalDate.ofEpochDay(epochDay)
        val now = LocalDate.now()
        val diff = now.toEpochDay() - epochDay
        return when {
            diff == 0L -> "Today"
            diff == 1L -> "Yesterday"
            diff in 2..6 -> d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            d.year == now.year -> d.format(monthDay)
            else -> d.format(monthDayYear)
        }
    }

    fun relativeMillis(millis: Long): String = relative(toLocalDate(millis).toEpochDay())

    fun short(epochDay: Long): String {
        val d = LocalDate.ofEpochDay(epochDay)
        return if (d.year == LocalDate.now().year) d.format(monthDay) else d.format(shortMonthYear)
    }

    fun time(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(time)

    fun weekdayName(date: LocalDate = LocalDate.now()): String = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

    /** "1:05:09" or "42:10". */
    fun duration(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
    }

    /** "1 h 5 min" or "42 min". */
    fun durationWords(ms: Long): String {
        val min = (ms / 60000).coerceAtLeast(0)
        return if (min >= 60) "${min / 60} h ${min % 60} min" else "$min min"
    }

    /** "1:30" or "45 s" for rest times. */
    fun rest(seconds: Int): String =
        if (seconds >= 60) String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60) else "$seconds s"
}
