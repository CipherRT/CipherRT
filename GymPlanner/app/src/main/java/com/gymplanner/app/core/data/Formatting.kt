package com.gymplanner.app.core.data

import com.gymplanner.app.core.ui.Dates

/** "8–12", "8–12 to failure", "10 per arm/leg", "to failure" … */
fun PlanExercise.repsText(): String {
    val range = when {
        repMax == 0 && repMin == 0 -> ""
        repMin == repMax || repMax == 0 -> "$repMin"
        else -> "$repMin–$repMax"
    }
    val side = if (perSide) " per arm/leg" else ""
    return when {
        toFailure && range.isEmpty() -> "to failure"
        toFailure -> "$range$side to failure"
        else -> "$range$side"
    }
}

/** "3 × 8–10" or "3 × to failure". */
fun PlanExercise.targetText(): String = "$sets × ${repsText()}"

/** "3 × 8–10 · 1:30 rest" */
fun PlanExercise.summaryText(): String = "${targetText()} · ${Dates.rest(restSeconds)} rest"
