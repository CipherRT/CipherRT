package com.gymplanner.app.core.data

import com.gymplanner.app.core.ui.Dates

/** "8–10", "10 / arm", "AMRAP" … */
fun PlanExercise.repsText(): String = when {
    toFailure -> "to failure"
    repMin == repMax || repMax == 0 -> "$repMin" + if (perSide) " / side" else ""
    else -> "$repMin–$repMax" + if (perSide) " / side" else ""
}

/** "3 × 8–10" or "3 × to failure". */
fun PlanExercise.targetText(): String = "$sets × ${repsText()}"

/** "3 × 8–10 · 1:30 rest" */
fun PlanExercise.summaryText(): String = "${targetText()} · ${Dates.rest(restSeconds)} rest"
