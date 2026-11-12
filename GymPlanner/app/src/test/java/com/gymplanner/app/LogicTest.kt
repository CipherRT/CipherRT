package com.gymplanner.app

import com.gymplanner.app.core.data.BodyWeightEntry
import com.gymplanner.app.core.data.DayFrequency
import com.gymplanner.app.core.data.PlanDay
import com.gymplanner.app.feature.today.DoneWorkout
import com.gymplanner.app.feature.today.StreakLogic
import com.gymplanner.app.feature.today.TodayLogic
import com.gymplanner.app.feature.weight.WeightLogic
import com.gymplanner.app.feature.weight.WeightRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TodayLogicTest {
    private fun day(id: Long, order: Int, freq: String = DayFrequency.WEEKLY) =
        PlanDay(id = id, planId = 1, name = "D$id", orderIndex = order, frequency = freq)
    private val today = LocalDate.of(2026, 11, 12)
    private fun done(id: Long?, daysAgo: Long, name: String = "D$id") =
        DoneWorkout(id, name, today.minusDays(daysAgo), 1_000_000L - daysAgo * 1000)

    // Back, Chest, Shoulders & Arms, Leg (every 2 weeks)
    private val days = listOf(day(1, 0), day(2, 1), day(3, 2), day(4, 3, DayFrequency.BIWEEKLY))

    @Test fun firstDayWhenNoHistory() = assertEquals(1L, TodayLogic.nextDay(days, emptyList(), today)?.id)
    @Test fun nextAfterLastWorkout() = assertEquals(2L, TodayLogic.nextDay(days, listOf(done(1, 2)), today)?.id)
    @Test fun noDays() = assertNull(TodayLogic.nextDay(emptyList(), emptyList(), today))

    /** Skipping days in real life doesn't skip days in the plan: a week off still resumes where you were. */
    @Test fun skippedDaysDontPileUp() = assertEquals(3L, TodayLogic.nextDay(days, listOf(done(1, 20), done(2, 12)), today)?.id)

    @Test fun biweeklyDaySkippedIfDoneWithin14Days() {
        val history = listOf(done(4, 6), done(1, 4), done(2, 3), done(3, 1))
        assertEquals(1L, TodayLogic.nextDay(days, history, today)?.id)
    }

    @Test fun biweeklyDayComesBackAfter14Days() {
        val history = listOf(done(4, 15), done(1, 4), done(2, 3), done(3, 1))
        assertEquals(4L, TodayLogic.nextDay(days, history, today)?.id)
    }

    @Test fun restoredSessionsMatchByName() =
        assertEquals(3L, TodayLogic.nextDay(days, listOf(done(null, 1, name = "D2")), today)?.id)
}

class StreakLogicTest {
    private val today = LocalDate.of(2026, 11, 12) // a Thursday
    private fun weekDays(weeksAgo: Long, count: Int) =
        (0 until count).map { StreakLogic.weekStart(today).minusWeeks(weeksAgo).plusDays(it.toLong()) }

    @Test fun unfinishedWeekDoesNotBreakStreak() {
        val dates = weekDays(1, 4) + weekDays(2, 4) + weekDays(0, 1)
        val s = StreakLogic.compute(dates, 4, today)
        assertEquals(2, s.current); assertEquals(1, s.thisWeek)
    }

    @Test fun currentWeekCountsOnceHit() =
        assertEquals(3, StreakLogic.compute(weekDays(0, 4) + weekDays(1, 4) + weekDays(2, 5), 4, today).current)

    @Test fun missedWeekBreaksIt() {
        val s = StreakLogic.compute(weekDays(1, 4) + weekDays(2, 2) + weekDays(3, 4) + weekDays(4, 4) + weekDays(5, 4), 4, today)
        assertEquals(1, s.current); assertEquals(3, s.best)
    }

    @Test fun empty() = assertEquals(0, StreakLogic.compute(emptyList(), 4, today).current)
}

class WeightLogicTest {
    private fun e(id: Long, day: Long, kg: Double) = BodyWeightEntry(id = id, profileId = 1, date = day, weightKg = kg)

    @Test fun lastEntryPerDayWins() {
        val pts = WeightLogic.dailyPoints(listOf(e(1, 10, 80.0), e(2, 10, 81.0), e(3, 11, 82.0)))
        assertEquals(listOf(81.0, 82.0), pts.map { it.y })
    }

    @Test fun sevenDayAverage() {
        val pts = WeightLogic.dailyPoints((0L..9L).map { e(it + 1, it, 80.0 + it) })
        val avg = WeightLogic.sevenDayAverage(pts)
        assertEquals(80.0, avg.first().y, 0.001)
        assertEquals(86.0, avg.last().y, 0.001)
    }

    @Test fun rangeFilter() {
        val pts = WeightLogic.dailyPoints((0L..99L).map { e(it + 1, it, 80.0) })
        assertEquals(30, WeightLogic.filter(pts, WeightRange.M1, 99).size)
        assertEquals(100, WeightLogic.filter(pts, WeightRange.ALL, 99).size)
    }

    @Test fun changeOver30Days() {
        val pts = WeightLogic.dailyPoints(listOf(e(1, 0, 90.0), e(2, 50, 85.0), e(3, 100, 83.0)))
        assertEquals(-2.0, WeightLogic.changeOver(pts, 30, 100)!!, 0.001)
    }
}

class WeighInTimesTest {
    private val t = com.gymplanner.app.feature.weighin.WeighInTimes
    private val thu7 = java.time.LocalDateTime.of(2026, 11, 12, 7, 0) // Thursday

    @Test fun laterTodayIfNotPassed() =
        assertEquals(java.time.LocalDateTime.of(2026, 11, 12, 7, 0), t.next(thu7.minusMinutes(1), 420, 127))

    @Test fun tomorrowOnceTodayPassed() =
        assertEquals(java.time.LocalDateTime.of(2026, 11, 13, 7, 0), t.next(thu7, 420, 127))

    @Test fun onlyChosenDays() {
        val monOnly = 1 // bit 0 = Monday
        assertEquals(java.time.LocalDateTime.of(2026, 11, 16, 6, 30), t.next(thu7, 390, monOnly))
    }

    @Test fun noDays() = assertNull(t.next(thu7, 420, 0))
}
