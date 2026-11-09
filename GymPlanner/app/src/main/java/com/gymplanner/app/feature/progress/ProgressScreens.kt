package com.gymplanner.app.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.ProfileMenuTargets
import com.gymplanner.app.core.model.MuscleGroups
import com.gymplanner.app.core.model.Units
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.Card
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.core.ui.EmptyState
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.LargeTitle
import com.gymplanner.app.core.ui.LineChart
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.NavBar
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.StatTile
import com.gymplanner.app.core.ui.appViewModel
import com.gymplanner.app.feature.settings.ProfileMenuButton

@Composable
fun ProgressScreen(menu: ProfileMenuTargets, onOpenExercise: (Long) -> Unit) {
    val vm = appViewModel { ProgressViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { LargeTitle("Progress") { ProfileMenuButton(menu) } }
        item {
            Column {
                Text(
                    "THIS WEEK", style = AppTheme.type.sectionHeader, color = AppTheme.colors.secondaryLabel,
                    modifier = Modifier.padding(start = 32.dp, bottom = 6.dp),
                )
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Workouts", "${s.week.sessions}", Modifier.weight(1f))
                    StatTile("Sets", "${s.week.sets}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.padding(horizontal = 16.dp)) {
                    StatTile("Volume (weight × reps)", Units.formatVolume(s.week.volumeKg, s.units), Modifier.weight(1f))
                }
            }
        }
        if (s.loaded && s.exercises.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.BarChart, "No workouts yet",
                    "Finish your first workout and your progress for each exercise shows up here.",
                )
            }
        }
        if (s.exercises.isNotEmpty()) {
            item {
                GroupedSection(header = "Exercises", footer = "Tap an exercise to see its chart and past sessions.") {
                    s.exercises.forEachIndexed { i, e ->
                        ListRow(
                            e.name,
                            subtitle = "${MuscleGroups.label(e.muscleGroup)} · ${e.sessions} workout${if (e.sessions == 1) "" else "s"} · last ${Dates.relativeMillis(e.lastDone)}",
                            onClick = { onOpenExercise(e.exerciseId) },
                        )
                        if (i < s.exercises.lastIndex) RowDivider()
                    }
                }
            }
        }
        if (s.recent.isNotEmpty()) {
            item {
                GroupedSection(header = "Recent workouts") {
                    s.recent.forEachIndexed { i, w ->
                        ListRow(
                            w.dayName + if (w.isLight) " (light)" else "",
                            value = w.finishedAt?.let { Dates.durationWords(it - w.startedAt) },
                            subtitle = Dates.relativeMillis(w.startedAt) + " · " + Dates.time(w.startedAt),
                        )
                        if (i < s.recent.lastIndex) RowDivider()
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun ExerciseHistoryScreen(exerciseId: Long, onBack: () -> Unit) {
    val vm = appViewModel(key = "history$exerciseId") { ExerciseHistoryViewModel(it, exerciseId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val units = s.units

    Column(Modifier.fillMaxSize()) {
        NavBar(s.exercise?.name ?: "", onBack = onBack, backLabel = "Progress")
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Heaviest", s.bestWeight?.let { Units.formatWeight(it, units) } ?: "—", Modifier.weight(1f))
                    StatTile(
                        "Best set",
                        s.bestSet?.let { "${Units.formatWeight(it.weightKg ?: 0.0, units, withUnit = false)} × ${it.reps ?: 0}" } ?: "—",
                        Modifier.weight(1f),
                    )
                }
            }
            if (s.chart.isNotEmpty()) {
                item {
                    Card {
                        Text("Heaviest set per workout (${units.weightUnit})", style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel)
                        Spacer(Modifier.height(12.dp))
                        LineChart(
                            points = s.chart.map { it.copy(y = Units.kgToDisplay(it.y, units)) },
                            xLabel = { Dates.short(it) },
                            yLabel = { Units.formatNumber(it, 0) },
                        )
                    }
                }
            }
            s.sessions.forEach { session ->
                item(key = session.sessionId) {
                    GroupedSection(header = Dates.relativeMillis(session.startedAt)) {
                        session.sets.forEachIndexed { i, set ->
                            ListRow(
                                "Set ${set.setNumber}",
                                value = "${Units.formatWeight(set.weightKg ?: 0.0, units)} × ${set.reps ?: 0}",
                            )
                            if (i < session.sets.lastIndex) RowDivider()
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
