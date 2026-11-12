package com.gymplanner.app.feature.today

import com.gymplanner.app.core.model.UnitSystem
import kotlin.math.abs
import kotlin.math.roundToInt

/** Which plates go on each side of the bar for a target weight. Pure, so it's easy to test. */
object PlateCalculator {
    val KG_PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    val LB_PLATES = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)

    data class Result(val perSide: List<Pair<Double, Int>>, val achieved: Double, val leftover: Double)

    /** [total] and [bar] in the display unit (kg or lb). */
    fun compute(total: Double, bar: Double, units: UnitSystem): Result {
        val plates = if (units == UnitSystem.METRIC) KG_PLATES else LB_PLATES
        var side = ((total - bar) / 2.0).coerceAtLeast(0.0)
        val out = mutableListOf<Pair<Double, Int>>()
        for (p in plates) {
            val n = ((side + 1e-9) / p).toInt()
            if (n > 0) { out += p to n; side -= n * p }
        }
        val achieved = bar + 2 * out.sumOf { it.first * it.second }
        return Result(out, achieved, if (abs(side) < 1e-6) 0.0 else side * 2)
    }

    /** Rounds a drop-set weight to something you can load: 2.5 kg or 5 lb steps. */
    fun roundToLoadable(display: Double, units: UnitSystem): Double {
        val step = if (units == UnitSystem.METRIC) 2.5 else 5.0
        return ((display / step).roundToInt() * step).coerceAtLeast(0.0)
    }
}
