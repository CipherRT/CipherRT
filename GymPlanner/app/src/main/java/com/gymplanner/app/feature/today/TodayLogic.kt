package com.gymplanner.app.feature.today

import com.gymplanner.app.core.data.DayFrequency
import com.gymplanner.app.core.data.PlanDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** A finished workout, reduced to what the rotation needs. */
data class DoneWorkout(val planDayId: Long?, val dayName: String, val date: LocalDate, val startedAt: Long)

object TodayLogic {
    /**
     * The next day in the rotation, with no fixed weekdays:
     * the day after the last finished workout of this plan (wrapping around), so
     * skipped days never pile up. A day set to "every 2 weeks" is passed over if it
     * was done in the last 14 days.
     */
    fun nextDay(days: List<PlanDay>, history: List<DoneWorkout>, today: LocalDate): PlanDay? {
        if (days.isEmpty()) return null
        val sorted = days.sortedBy { it.orderIndex }
        fun matches(w: DoneWorkout, d: PlanDay) = w.planDayId == d.id || (w.planDayId == null && w.dayName == d.name)
        val last = history.filter { w -> sorted.any { matches(w, it) } }.maxByOrNull { it.startedAt }
        val start = last?.let { w -> sorted.indexOfFirst { matches(w, it) } + 1 } ?: 0
        for (step in sorted.indices) {
            val d = sorted[(start + step) % sorted.size]
            if (d.frequency == DayFrequency.BIWEEKLY) {
                val lastDone = history.filter { matches(it, d) }.maxOfOrNull { it.date }
                if (lastDone != null && ChronoUnit.DAYS.between(lastDone, today) < 14) continue
            }
            return d
        }
        return sorted[start % sorted.size]
    }

    /** Rough length of a workout: each set takes ~40 s plus its rest. */
    fun estimateMinutes(sets: List<Pair<Int, Int>>): Int =
        (sets.sumOf { (count, rest) -> count * (40 + rest) } / 60.0).toInt().coerceAtLeast(1)
}

data class Streak(val current: Int, val best: Int, val thisWeek: Int)

object StreakLogic {
    fun weekStart(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /**
     * Weekly streak: consecutive Monday–Sunday weeks with at least [target] sessions.
     * The current week counts once the target is hit, and an unfinished current week
     * never breaks the streak.
     */
    fun compute(sessionDates: List<LocalDate>, target: Int, today: LocalDate): Streak {
        val perWeek = sessionDates.groupingBy { weekStart(it) }.eachCount()
        val thisWeekStart = weekStart(today)
        val thisWeek = perWeek[thisWeekStart] ?: 0
        val goal = target.coerceAtLeast(1)

        var current = if (thisWeek >= goal) 1 else 0
        var w = thisWeekStart.minusWeeks(1)
        while ((perWeek[w] ?: 0) >= goal) { current++; w = w.minusWeeks(1) }

        var best = 0
        var run = 0
        val first = perWeek.keys.minOrNull() ?: thisWeekStart
        var cursor = first
        while (!cursor.isAfter(thisWeekStart)) {
            if ((perWeek[cursor] ?: 0) >= goal) { run++; best = maxOf(best, run) }
            else if (cursor != thisWeekStart) run = 0
            cursor = cursor.plusWeeks(1)
        }
        return Streak(current, maxOf(best, current), thisWeek)
    }
}
