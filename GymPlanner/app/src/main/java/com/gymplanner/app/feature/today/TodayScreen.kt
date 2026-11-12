package com.gymplanner.app.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.ProfileMenuTargets
import com.gymplanner.app.core.data.PlanExercise
import com.gymplanner.app.core.data.repsText
import com.gymplanner.app.core.model.Units
import com.gymplanner.app.core.model.Weekdays
import com.gymplanner.app.core.ui.ActionSheet
import com.gymplanner.app.core.ui.AppSwitch
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.Badge
import com.gymplanner.app.core.ui.Card
import com.gymplanner.app.core.ui.CardShape
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.core.ui.EmptyState
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.LargeTitle
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.PrimaryButton
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.ScreenPadding
import com.gymplanner.app.core.ui.SheetAction
import com.gymplanner.app.core.ui.TextAction
import com.gymplanner.app.core.ui.TintedButton
import com.gymplanner.app.core.ui.appViewModel
import com.gymplanner.app.feature.settings.ProfileMenuButton
import com.gymplanner.app.feature.weight.WeightEntrySheet

@Composable
fun TodayScreen(
    menu: ProfileMenuTargets,
    onOpenWorkout: (Long) -> Unit,
    onGoToPlan: () -> Unit,
    onGoToWeight: () -> Unit,
) {
    val vm = appViewModel { TodayViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    var light by rememberSaveable { mutableStateOf(false) }
    var choosingDay by remember { mutableStateOf(false) }
    var addingWeight by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }

    val day = s.day
    val eyebrow = buildString {
        append(Dates.weekdayName())
        s.plan?.let { append(" · ${it.name}") }
    }

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            LargeTitle(
                title = when {
                    !s.loaded -> ""
                    day != null -> day.name
                    else -> "Today"
                },
                eyebrow = eyebrow,
            ) { ProfileMenuButton(menu) }
        }

        // A workout that was left open (even if the app was closed) can be resumed.
        s.unfinished?.let { w ->
            item {
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(AppTheme.colors.green))
                        Spacer(Modifier.width(8.dp))
                        Text("WORKOUT IN PROGRESS", style = AppTheme.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.green)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(w.dayName, style = AppTheme.type.title2, color = AppTheme.colors.label)
                    Text("Started at ${Dates.time(w.startedAt)}", style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel)
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("Resume Workout", onClick = { onOpenWorkout(w.id) }, color = AppTheme.colors.green, icon = Icons.Rounded.PlayArrow)
                }
            }
        }

        if (s.loaded && s.plan == null) {
            item {
                EmptyState(
                    Icons.Rounded.CalendarMonth, "Set up your plan",
                    "Create your weekly split on the Plan tab, then come back here to train.",
                    actionText = "Go to Plan", onAction = onGoToPlan,
                )
            }
        } else if (s.loaded && day == null) {
            item {
                EmptyState(
                    Icons.Rounded.CalendarMonth, "Your plan has no days yet",
                    "Add a day (like “Push” or “Legs”) and some exercises.",
                    actionText = "Add Days", onAction = onGoToPlan,
                )
            }
        }

        if (day != null) {
            val visible = s.exercises.filter { (pe, _) -> !(light && pe.isOptional) }
            val sets = visible.map { (pe, _) -> setsFor(pe, light) to pe.restSeconds }
            item {
                Card {
                    if (s.trainedToday && s.isSuggested && s.unfinished == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = AppTheme.colors.green, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Nice work — you already trained today. Up next:", style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel)
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Fact(Icons.Rounded.SwapVert, "${visible.size} exercises")
                        Fact(Icons.Rounded.Timer, "${sets.sumOf { it.first }} sets")
                        Fact(Icons.Rounded.Schedule, "~${TodayLogic.estimateMinutes(sets)} min")
                    }
                    if (day.note.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(day.note, style = AppTheme.type.subhead.copy(fontStyle = FontStyle.Italic), color = AppTheme.colors.secondaryLabel)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(CardShape).clickable { light = !light }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Eco, null, tint = AppTheme.colors.green, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Light day", style = AppTheme.type.body, color = AppTheme.colors.label)
                            Text("Skip optional exercises, one set less each", style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel)
                        }
                        AppSwitch(light, { light = it })
                    }
                    Spacer(Modifier.height(12.dp))
                    if (s.unfinished == null) {
                        PrimaryButton(
                            "Start Workout",
                            icon = Icons.Rounded.PlayArrow,
                            enabled = visible.isNotEmpty() && !starting,
                            onClick = { starting = true; vm.start(light) { starting = false; onOpenWorkout(it) } },
                        )
                    }
                    if (s.days.size > 1) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextAction(if (s.isSuggested) "Train a different day" else "Change day", { choosingDay = true })
                        }
                    }
                }
            }

            if (visible.isEmpty()) {
                item {
                    GroupedSection {
                        ListRow("No exercises yet", subtitle = "Add exercises to ${day.name} on the Plan tab.", onClick = onGoToPlan)
                    }
                }
            }
            val warmups = visible.filter { it.first.isWarmup }
            val main = visible.filter { !it.first.isWarmup }
            if (warmups.isNotEmpty()) item { ExerciseList("Warm-up", warmups, light) }
            if (main.isNotEmpty()) item { ExerciseList("Workout", main, light) }
        }

        if (s.loaded && s.profile != null) {
            item { WeightCard(s, onAdd = { addingWeight = true }, onOpen = onGoToWeight) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (choosingDay) {
        ActionSheet(
            title = "Choose a day",
            actions = s.days.map { d ->
                val pin = d.weekday?.let { " (${Weekdays.short(it)})" } ?: ""
                SheetAction(d.name + pin + if (d.id == day?.id) " (current)" else "") { vm.chooseDay(d.id) }
            },
            onDismiss = { choosingDay = false },
        )
    }
    val p = s.profile
    if (addingWeight && p != null) {
        WeightEntrySheet(p.id, s.units, existing = null, lastWeightKg = s.latestWeight?.weightKg, onDismiss = { addingWeight = false })
    }
}

private fun setsFor(pe: PlanExercise, light: Boolean) = if (light && !pe.isWarmup) (pe.sets - 1).coerceAtLeast(1) else pe.sets

@Composable
private fun Fact(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = AppTheme.colors.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = AppTheme.type.subhead.copy(fontWeight = FontWeight.Medium), color = AppTheme.colors.label)
    }
}

@Composable
private fun ExerciseList(title: String, items: List<Pair<PlanExercise, com.gymplanner.app.core.data.Exercise?>>, light: Boolean) {
    GroupedSection(header = title) {
        items.forEachIndexed { i, (pe, ex) ->
            val sets = setsFor(pe, light)
            ListRow(
                title = ex?.name ?: "Exercise",
                subtitle = "$sets × ${pe.repsText()} · ${Dates.rest(pe.restSeconds)} rest" + if (pe.note.isNotBlank()) "\n${pe.note}" else "",
                trailing = if (pe.isOptional) { { Badge("Optional", AppTheme.colors.secondaryLabel) } } else null,
            )
            if (i < items.lastIndex) RowDivider()
        }
    }
}

@Composable
private fun WeightCard(s: TodayUiState, onAdd: () -> Unit, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding.dp)
            .clip(CardShape)
            .background(AppTheme.colors.card)
            .clickable(onClick = onOpen)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.MonitorWeight, null, tint = AppTheme.colors.orange, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Body weight", style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel)
            val w = s.latestWeight
            if (w != null) {
                Text(Units.formatWeight(w.weightKg, s.units), style = AppTheme.type.title3, color = AppTheme.colors.label)
                Text(Dates.relative(w.date), style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel)
            } else {
                Text("Not added yet", style = AppTheme.type.title3, color = AppTheme.colors.label)
            }
        }
        TintedButton("Weight", onClick = onAdd, icon = Icons.Rounded.Add, compact = true, color = AppTheme.colors.orange)
    }
}
