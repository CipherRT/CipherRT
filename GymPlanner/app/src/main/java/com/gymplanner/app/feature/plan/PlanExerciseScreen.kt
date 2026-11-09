package com.gymplanner.app.feature.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.core.data.PlanExercise
import com.gymplanner.app.core.data.targetText
import com.gymplanner.app.core.model.MuscleGroups
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.ConfirmDialog
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.core.ui.FieldRow
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.NavBar
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.Stepper
import com.gymplanner.app.core.ui.SwitchRow
import com.gymplanner.app.core.ui.appViewModel

/** Sets, reps, rest, note and options for one exercise in a day. Changes save instantly. */
@Composable
fun PlanExerciseScreen(id: Long, onBack: () -> Unit) {
    val vm = appViewModel(key = "pe$id") { PlanExerciseViewModel(it, id) }
    val stored by vm.item.collectAsStateWithLifecycle()
    val exercise by vm.exercise.collectAsStateWithLifecycle()
    var pe by remember { mutableStateOf<PlanExercise?>(null) }
    LaunchedEffect(stored) { if (pe == null) pe = stored }
    var confirmRemove by remember { mutableStateOf(false) }

    fun change(update: PlanExercise.() -> PlanExercise) {
        val next = pe?.update() ?: return
        pe = next
        vm.save(next)
    }

    Column(Modifier.fillMaxSize()) {
        NavBar(exercise?.name ?: "", onBack = onBack, backLabel = "Day")
        val e = pe ?: return@Column
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item {
                GroupedSection(
                    header = "Sets & reps",
                    footer = "Target: ${e.targetText()}" + (exercise?.let { " · ${MuscleGroups.label(it.muscleGroup)}" } ?: ""),
                ) {
                    ListRow("Sets", trailing = { Stepper(e.sets, { v -> change { copy(sets = v) } }, 1..10) })
                    RowDivider()
                    SwitchRow(
                        "To failure (AMRAP)", e.toFailure, { v -> change { copy(toFailure = v) } },
                        icon = Icons.Rounded.Whatshot, iconColor = AppTheme.colors.red,
                    )
                    if (!e.toFailure) {
                        RowDivider()
                        ListRow("Min reps", trailing = {
                            Stepper(e.repMin, { v -> change { copy(repMin = v, repMax = maxOf(repMax, v)) } }, 1..50)
                        })
                        RowDivider()
                        ListRow("Max reps", trailing = {
                            Stepper(e.repMax, { v -> change { copy(repMax = v, repMin = minOf(repMin, v)) } }, 1..50)
                        })
                    }
                    RowDivider()
                    SwitchRow(
                        "Reps per arm / leg", e.perSide, { v -> change { copy(perSide = v) } },
                        icon = Icons.Rounded.SwapHoriz, iconColor = AppTheme.colors.teal,
                    )
                }
            }
            item {
                GroupedSection(header = "Rest", footer = "The rest timer starts by itself when you tick off a set.") {
                    ListRow(
                        "Rest between sets",
                        icon = Icons.Rounded.Timer, iconColor = AppTheme.colors.indigo,
                        trailing = { Stepper(e.restSeconds, { v -> change { copy(restSeconds = v) } }, 0..600, step = 15, format = Dates::rest) },
                    )
                }
            }
            item {
                GroupedSection(
                    header = "Options",
                    footer = "Optional exercises can be skipped on a tired day, and are left out on a light day.",
                ) {
                    SwitchRow(
                        "Optional", e.isOptional, { v -> change { copy(isOptional = v) } },
                        icon = Icons.Rounded.SelfImprovement, iconColor = AppTheme.colors.secondaryLabel,
                    )
                    RowDivider(startIndent = 60.dp)
                    SwitchRow(
                        "Warm-up", e.isWarmup, { v -> change { copy(isWarmup = v) } },
                        icon = Icons.Rounded.LocalFireDepartment, iconColor = AppTheme.colors.orange,
                    )
                }
            }
            item {
                GroupedSection(header = "Note") {
                    FieldRow(e.note, { v -> if (v.length <= 200) change { copy(note = v) } }, placeholder = "e.g. 1-second squeeze at the top", singleLine = false)
                }
            }
            item {
                GroupedSection {
                    ListRow("Remove from Day", titleColor = AppTheme.colors.red, showChevron = false, onClick = { confirmRemove = true })
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = "Remove ${exercise?.name ?: "exercise"}?",
            message = "It’s removed from this day only. Past workouts are kept.",
            confirmText = "Remove",
            onDismiss = { confirmRemove = false },
            onConfirm = { confirmRemove = false; vm.remove(onBack) },
        )
    }
}
