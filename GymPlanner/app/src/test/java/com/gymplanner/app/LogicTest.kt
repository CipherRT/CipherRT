package com.gymplanner.app

import com.gymplanner.app.core.data.BodyWeightEntry
import com.gymplanner.app.core.data.PlanDay
import com.gymplanner.app.feature.today.TodayLogic
import com.gymplanner.app.feature.weight.WeightLogic
import com.gymplanner.app.feature.weight.WeightRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodayLogicTest {
    private fun day(id: Long, order: Int, weekday: Int? = null) = PlanDay(id = id, planId = 1, name = "D$id", orderIndex = order, weekday = weekday)
    private val days = listOf(day(1, 0), day(2, 1), day(3, 2))

    @Test fun firstDayWhenNoHistory() = assertEquals(1L, TodayLogic.pickDay(days, 3, null)?.id)
    @Test fun nextDayAfterLastWorkout() = assertEquals(2L, TodayLogic.pickDay(days, 3, 1)?.id)
    @Test fun wrapsAround() = assertEquals(1L, TodayLogic.pickDay(days, 3, 3)?.id)
    @Test fun noDays() = assertNull(TodayLogic.pickDay(emptyList(), 1, null))

    @Test fun pinnedWeekdayWins() {
        val pinned = listOf(day(1, 0), day(2, 1, weekday = 5), day(3, 2))
        assertEquals(2L, TodayLogic.pickDay(pinned, 5, 2)?.id)
    }

    @Test fun skipsDaysPinnedToOtherWeekdays() {
        val pinned = listOf(day(1, 0), day(2, 1, weekday = 5), day(3, 2))
        assertEquals(3L, TodayLogic.pickDay(pinned, 2, 1)?.id)
    }
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
        assertEquals(86.0, avg.last().y, 0.001) // mean of 83..89
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
