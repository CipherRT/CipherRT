package com.gymplanner.app.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.gymplanner.app.core.data.displayName
import com.gymplanner.app.core.ui.ScreenPadding
import com.gymplanner.app.core.ui.SheetHeader
import com.gymplanner.app.feature.today.Legend
import com.gymplanner.app.feature.today.Streak
import com.gymplanner.app.feature.weight.signed
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
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
    var dayDetail by remember { mutableStateOf<LocalDate?>(null) }

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { LargeTitle("Progress") { ProfileMenuButton(menu) } }
        item { StreakCard(s.streak, s.profile?.weeklyTarget ?: 4) }
        item {
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButtonSmall(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous month") { vm.shiftMonth(-1) }
                    Text(
                        s.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                        style = AppTheme.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.label,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                    )
                    val canNext = s.month.isBefore(YearMonth.now())
                    IconButtonSmall(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next month", enabled = canNext) { vm.shiftMonth(1) }
                }
                Spacer(Modifier.height(8.dp))
                MonthCalendar(s.month, s.gymDays, s.cardioDays) { dayDetail = it }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Legend()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).border(2.dp, AppTheme.colors.label, RoundedCornerShape(3.dp)))
                        Text("  Today", style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel)
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = ScreenPadding.dp + 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("gym sessions", "${s.stats.gymSessions}", Modifier.weight(1f))
                StatTile("cardio", "${s.stats.cardioSessions}", Modifier.weight(1f))
                StatTile(
                    "${s.units.weightUnit} this month",
                    s.stats.weightChangeKg?.let { signed(it, s.units, withUnit = false) } ?: "—",
                    Modifier.weight(1f),
                )
            }
        }
        if (s.loaded && s.exercises.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.BarChart, "No workouts yet",
                    "Finish your first workout and each exercise gets its own chart here.",
                )
            }
        }
        if (s.exercises.isNotEmpty()) {
            item {
                GroupedSection(header = "Exercises", footer = "Tap an exercise for its chart and past sessions.") {
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
        item { Spacer(Modifier.height(24.dp)) }
    }

    dayDetail?.let { d -> DayDetailSheet(d, s, onDismiss = { dayDetail = null }) }
}

@Composable
private fun StreakCard(streak: Streak, target: Int) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(AppTheme.colors.accent),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.LocalFireDepartment, null, tint = AppTheme.colors.onAccent, modifier = Modifier.size(34.dp)) }
            Column(Modifier.weight(1f)) {
                Text(
                    "${streak.current} week${if (streak.current == 1) "" else "s"}",
                    style = AppTheme.type.number.copy(fontSize = 30.sp), color = AppTheme.colors.label,
                )
                Text("Streak · best ${streak.best} week${if (streak.best == 1) "" else "s"}", style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${streak.thisWeek}", style = AppTheme.type.number.copy(fontSize = 20.sp), color = AppTheme.colors.label)
                    Text("/$target", style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel)
                }
                Text("this week", style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel)
            }
        }
    }
}

@Composable
private fun IconButtonSmall(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = if (enabled) AppTheme.colors.label else AppTheme.colors.tertiaryLabel) }
}

/** Month grid, Monday first. Gym days filled with the accent, cardio days a blue ring, today outlined. */
@Composable
private fun MonthCalendar(month: YearMonth, gym: Set<LocalDate>, cardio: Set<LocalDate>, onDay: (LocalDate) -> Unit) {
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value - 1
    val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val today = LocalDate.now()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            DayOfWeek.entries.forEach {
                Text(
                    it.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = AppTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = AppTheme.colors.secondaryLabel, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { d ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (d != null) {
                            val g = d in gym; val c = d in cardio
                            CalendarDay(d.dayOfMonth, g, c, d == today, d.isAfter(today)) { onDay(d) }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CalendarDay(n: Int, gym: Boolean, cardio: Boolean, today: Boolean, future: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    val shape = if (cardio && !gym) CircleShape else RoundedCornerShape(10.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(shape)
            .background(if (gym) c.accent else androidx.compose.ui.graphics.Color.Transparent)
            .then(
                when {
                    gym && cardio -> Modifier.border(3.dp, c.cardio, shape)
                    cardio -> Modifier.border(2.dp, c.cardio, shape)
                    today -> Modifier.border(2.dp, c.label, shape)
                    else -> Modifier
                }
            )
            .clickable(enabled = !future, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = "$n" + when { gym && cardio -> ", gym and cardio"; gym -> ", gym"; cardio -> ", cardio"; else -> "" } + if (today) ", today" else ""
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$n",
            style = AppTheme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
            color = when { gym -> c.onAccent; cardio -> c.cardio; future -> c.tertiaryLabel; else -> c.chipText },
        )
    }
}

/** What happened on one day: workouts, cardio, weight. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(day: LocalDate, s: ProgressUiState, onDismiss: () -> Unit) {
    val workouts = s.sessions.filter { Dates.toLocalDate(it.startedAt) == day }
    val cardio = s.cardio.filter { it.date == day.toEpochDay() }
    val weight = s.weights.firstOrNull { it.date == day.toEpochDay() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppTheme.colors.background,
        dragHandle = null,
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SheetHeader(Dates.relative(day.toEpochDay()), onCancel = null, doneText = "Done", onDone = onDismiss)
            if (workouts.isEmpty() && cardio.isEmpty() && weight == null) {
                Text("Nothing logged on this day.", style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel, modifier = Modifier.padding(horizontal = 24.dp))
            }
            if (workouts.isNotEmpty()) {
                GroupedSection(header = "Gym") {
                    workouts.forEachIndexed { i, w ->
                        ListRow(
                            w.dayName + if (w.isLight) " (light)" else "",
                            subtitle = Dates.time(w.startedAt),
                            value = w.finishedAt?.let { Dates.durationWords(it - w.startedAt) },
                        )
                        if (i < workouts.lastIndex) RowDivider()
                    }
                }
            }
            if (cardio.isNotEmpty()) {
                GroupedSection(header = "Cardio") {
                    cardio.forEachIndexed { i, c ->
                        ListRow(c.displayName(), subtitle = "${c.minutes} min", value = "${c.kcal} kcal")
                        if (i < cardio.lastIndex) RowDivider()
                    }
                }
            }
            weight?.let { w ->
                GroupedSection(header = "Weight") { ListRow("Body weight", value = Units.formatWeight(w.weightKg, s.units)) }
            }
        }
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
                                when (set.setType) {
                                    com.gymplanner.app.core.data.SetType.DROP -> "Drop set"
                                    com.gymplanner.app.core.data.SetType.WARMUP -> "Warm-up ${set.setNumber}"
                                    else -> "Set ${set.setNumber}"
                                },
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
