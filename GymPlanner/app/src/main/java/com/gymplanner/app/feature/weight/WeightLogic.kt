package com.gymplanner.app.feature.weight

import com.gymplanner.app.core.data.BodyWeightEntry
import com.gymplanner.app.core.ui.ChartPoint

enum class WeightRange(val label: String, val days: Long?) { M1("1M", 30), M3("3M", 90), Y1("1Y", 365), ALL("All", null) }

/** Pure helpers, kept separate so they're easy to test. */
object WeightLogic {
    /** Daily points (last entry of each day), oldest first. */
    fun dailyPoints(entries: List<BodyWeightEntry>): List<ChartPoint> =
        entries.groupBy { it.date }
            .map { (day, list) -> ChartPoint(day, list.maxBy { it.id }.weightKg) }
            .sortedBy { it.x }

    /** Average of the entries in the 7 days up to and including each point. */
    fun sevenDayAverage(points: List<ChartPoint>): List<ChartPoint> = points.map { p ->
        val window = points.filter { it.x in (p.x - 6)..p.x }
        ChartPoint(p.x, window.sumOf { it.y } / window.size)
    }

    fun filter(points: List<ChartPoint>, range: WeightRange, today: Long): List<ChartPoint> =
        range.days?.let { d -> points.filter { it.x > today - d } } ?: points

    /** Change over the last [days] days: latest minus the first entry inside that window (or just before it). */
    fun changeOver(points: List<ChartPoint>, days: Long, today: Long): Double? {
        if (points.size < 2) return null
        val latest = points.last()
        val base = points.lastOrNull { it.x <= today - days } ?: points.first()
        if (base == latest) return null
        return latest.y - base.y
    }
}
