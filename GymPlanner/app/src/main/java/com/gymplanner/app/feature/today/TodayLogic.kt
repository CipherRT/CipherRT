package com.gymplanner.app.feature.today

import com.gymplanner.app.core.data.PlanDay

object TodayLogic {
    /**
     * Which day to suggest today:
     * 1. a day pinned to today's weekday, otherwise
     * 2. the next day in order after the last finished workout (skipping days pinned to
     *    other weekdays, when there are unpinned ones), otherwise
     * 3. the first day.
     */
    fun pickDay(days: List<PlanDay>, weekday: Int, lastDayId: Long?): PlanDay? {
        if (days.isEmpty()) return null
        val sorted = days.sortedBy { it.orderIndex }
        sorted.firstOrNull { it.weekday == weekday }?.let { return it }
        val candidates = sorted.filter { it.weekday == null }.ifEmpty { sorted }
        val lastIndex = sorted.indexOfFirst { it.id == lastDayId }
        if (lastIndex == -1) return candidates.first()
        for (step in 1..sorted.size) {
            val d = sorted[(lastIndex + step) % sorted.size]
            if (d in candidates) return d
        }
        return candidates.first()
    }

    /** Rough length of a workout: each set takes ~40 s plus its rest. */
    fun estimateMinutes(sets: List<Pair<Int, Int>>): Int =
        (sets.sumOf { (count, rest) -> count * (40 + rest) } / 60.0).toInt().coerceAtLeast(1)
}
