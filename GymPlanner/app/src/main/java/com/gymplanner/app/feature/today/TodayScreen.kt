package com.gymplanner.app.feature.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.ProfileMenuTargets
import com.gymplanner.app.core.data.DayFrequency
import com.gymplanner.app.core.data.Exercise
import com.gymplanner.app.core.data.PlanExercise
import com.gymplanner.app.core.data.repsText
import com.gymplanner.app.core.model.Units
import com.gymplanner.app.core.ui.ActionSheet
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.Badge
import com.gymplanner.app.core.ui.ChartPoint
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.core.ui.EmptyState
import com.gymplanner.app.core.ui.PrimaryButton
import com.gymplanner.app.core.ui.ScreenPadding
import com.gymplanner.app.core.ui.SheetAction
import com.gymplanner.app.core.ui.TextAction
import com.gymplanner.app.core.ui.TintedButton
import com.gymplanner.app.core.ui.appViewModel
import com.gymplanner.app.core.ui.cardSurface
import com.gymplanner.app.feature.settings.ProfileMenuButton
import com.gymplanner.app.feature.weight.WeightEntrySheet
import com.gymplanner.app.feature.weight.signed
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScreen(
    menu: ProfileMenuTargets,
    onOpenWorkout: (Long) -> Unit,
    onGoToPlan: () -> Unit,
    onLogCardio: () -> Unit,
    onOpenProgress: () -> Unit,
) {
    val vm = appViewModel { TodayViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    var light by rememberSaveable { mutableStateOf(false) }
    var choosingDay by remember { mutableStateOf(false) }
    var addingWeight by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }
    fun start(day: com.gymplanner.app.core.data.PlanDay? = s.day) {
        if (starting) return
        starting = true
        vm.start(light, day) { starting = false; onOpenWorkout(it) }
    }

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Header(
                eyebrow = buildString {
                    append(eyebrowDate())
                    if (!s.offDay && s.day != null) append(if (s.day?.id == s.nextDay?.id) " · Next in rotation" else " · Your pick")
                },
                title = when {
                    !s.loaded -> ""
                    s.offDay -> "Off day"
                    else -> s.day?.name ?: "Today"
                },
                subtitle = if (s.offDay) {
                    val next = s.nextDay?.name
                    (if (s.unfinished == null && s.weekSessions > 0 && s.week.any { it.isToday && it.gym }) "Nice work today." else "Recovery counts too.") +
                        (next?.let { " Next up: $it." } ?: "")
                } else null,
                streak = s.streak.current,
                onStreak = onOpenProgress,
                menu = menu,
            )
        }

        s.unfinished?.let { w ->
            item {
                HeroCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        SetsRing(s.openDone, s.openTotal)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("${w.dayName} in progress · since ${Dates.time(w.startedAt)}", style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel)
                            PrimaryButton("Resume workout", onClick = { onOpenWorkout(w.id) }, icon = Icons.Rounded.PlayArrow)
                        }
                    }
                }
            }
        }

        when {
            !s.loaded -> Unit
            s.plan == null -> item {
                EmptyState(
                    Icons.Rounded.CalendarMonth, "Set up your plan",
                    "Create your split on the Plan tab, then come back here to train.",
                    actionText = "Go to Plan", onAction = onGoToPlan,
                )
            }
            s.day == null -> item {
                EmptyState(
                    Icons.Rounded.CalendarMonth, "Your plan has no days yet",
                    "Add a day (like Push or Legs) and some exercises.",
                    actionText = "Add days", onAction = onGoToPlan,
                )
            }
            s.offDay -> offDay(s, onLogCardio, onStartNext = { start(s.nextDay) }, onAddWeight = { addingWeight = true })
            else -> gymDay(
                s, light, starting,
                onToggleLight = { light = !light },
                onStart = { start() },
                onAddWeight = { addingWeight = true },
                onLogCardio = onLogCardio,
                onDifferentDay = { choosingDay = true },
                onNotToday = vm::notToday,
                onGoToPlan = onGoToPlan,
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (choosingDay) {
        ActionSheet(
            title = "Train a different day",
            actions = s.days.map { d ->
                val freq = if (d.frequency == DayFrequency.BIWEEKLY) " (every 2 weeks)" else ""
                SheetAction(d.name + freq + if (d.id == s.day?.id) " (current)" else "") { vm.chooseDay(d.id) }
            },
            onDismiss = { choosingDay = false },
        )
    }
    val p = s.profile
    if (addingWeight && p != null) {
        WeightEntrySheet(p.id, s.units, existing = null, lastWeightKg = s.latestWeight?.weightKg, onDismiss = { addingWeight = false })
    }
}

private fun eyebrowDate(d: LocalDate = LocalDate.now()): String =
    d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " · " +
        d.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + d.dayOfMonth

@Composable
private fun Header(
    eyebrow: String,
    title: String,
    subtitle: String?,
    streak: Int,
    onStreak: () -> Unit,
    menu: ProfileMenuTargets,
) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 12.dp, top = 16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(eyebrow.uppercase(), style = AppTheme.type.sectionHeader, color = AppTheme.colors.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(title, style = AppTheme.type.largeTitle, color = AppTheme.colors.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (streak > 0) StreakPill(streak, onStreak)
                ProfileMenuButton(menu)
            }
        }
        if (subtitle != null) {
            Text(subtitle, style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun StreakPill(weeks: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .heightIn(min = 44.dp)
            .cardSurface(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.LocalFireDepartment, null, tint = AppTheme.colors.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("$weeks-week streak", style = AppTheme.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.label)
    }
}

@Composable
private fun HeroCard(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = ScreenPadding.dp + 4.dp).cardSurface(RoundedCornerShape(22.dp)).padding(18.dp)) { content() }
}

/** Ring showing working sets done out of the total. */
@Composable
fun SetsRing(done: Int, total: Int, size: androidx.compose.ui.unit.Dp = 96.dp) {
    val accent = AppTheme.colors.accent
    val track = AppTheme.colors.border
    Box(Modifier.size(size).semantics { contentDescription = "$done of $total sets done" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2 + 4.dp.toPx()
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - inset * 2, this.size.height - inset * 2)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val frac = if (total == 0) 0f else done / total.toFloat()
            if (frac > 0f) drawArc(accent, -90f, 360f * frac, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$done/$total", style = AppTheme.type.number.copy(fontSize = 22.sp), color = AppTheme.colors.label)
            Text("sets", style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel)
        }
    }
}

private fun setsFor(pe: PlanExercise, light: Boolean) = if (light && !pe.isWarmup) (pe.sets - 1).coerceAtLeast(1) else pe.sets

private fun androidx.compose.foundation.lazy.LazyListScope.gymDay(
    s: TodayUiState,
    light: Boolean,
    starting: Boolean,
    onToggleLight: () -> Unit,
    onStart: () -> Unit,
    onAddWeight: () -> Unit,
    onLogCardio: () -> Unit,
    onDifferentDay: () -> Unit,
    onNotToday: () -> Unit,
    onGoToPlan: () -> Unit,
) {
    val visible = s.exercises.filter { (pe, _) -> !(light && pe.isOptional) }
    val working = visible.filter { !it.first.isWarmup }
    val totalSets = working.sumOf { setsFor(it.first, light) }
    val minutes = TodayLogic.estimateMinutes(visible.map { (pe, _) -> setsFor(pe, light) to pe.restSeconds })
    val day = s.day ?: return

    if (s.unfinished == null) {
        item {
            HeroCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    SetsRing(0, totalSets)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("${visible.size} exercises", style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel)
                            Text("~$minutes min", style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel)
                        }
                        PrimaryButton(
                            if (starting) "Starting…" else "Start workout",
                            onClick = onStart,
                            enabled = visible.isNotEmpty() && !starting,
                            icon = Icons.Rounded.PlayArrow,
                        )
                    }
                }
            }
        }
    }
    item {
        Row(Modifier.padding(horizontal = ScreenPadding.dp + 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(
                "Weight today",
                s.todayWeight?.let { Units.formatWeight(it.weightKg, s.units, withUnit = false) } ?: "Add",
                if (s.todayWeight != null) s.units.weightUnit else "",
                Modifier.weight(1f), onAddWeight,
            )
            Tile("Cardio this week", "${s.cardioKcalWeek}", "kcal", Modifier.weight(1f), onLogCardio)
        }
    }
    if (day.note.isNotBlank()) {
        item {
            Text(
                day.note, style = AppTheme.type.subhead.copy(fontStyle = FontStyle.Italic), color = AppTheme.colors.secondaryLabel,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
    item {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Exercises", style = AppTheme.type.headline, color = AppTheme.colors.label, modifier = Modifier.weight(1f))
            TextAction(
                if (light) "Light day: on" else "Light day",
                onToggleLight,
                bold = true,
                color = if (light) AppTheme.colors.accent else AppTheme.colors.secondaryLabel,
            )
        }
        if (light) {
            Text(
                "Optional exercises left out, one set less on the rest.",
                style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel, modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
    if (visible.isEmpty()) {
        item {
            Text(
                "No exercises on ${day.name} yet. Add some on the Plan tab.",
                style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel,
                modifier = Modifier.padding(horizontal = 20.dp).clickable(onClick = onGoToPlan),
            )
        }
    }
    visible.forEach { (pe, ex) ->
        item(key = "pe${pe.id}") { ExerciseRow(pe, ex, setsFor(pe, light)) }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            if (s.days.size > 1) TextAction("Different day", onDifferentDay, color = AppTheme.colors.secondaryLabel)
            if (s.unfinished == null) TextAction("Not today", onNotToday, color = AppTheme.colors.secondaryLabel)
        }
    }
}

@Composable
private fun Tile(label: String, value: String, unit: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .cardSurface(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = AppTheme.type.number, color = AppTheme.colors.label)
            if (unit.isNotEmpty()) {
                Text(" $unit", style = AppTheme.type.footnote.copy(fontWeight = FontWeight.Medium), color = AppTheme.colors.secondaryLabel, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
private fun ExerciseRow(pe: PlanExercise, ex: Exercise?, sets: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = ScreenPadding.dp + 4.dp).cardSurface(RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(ex?.name ?: "Exercise", style = AppTheme.type.callout.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.label)
            val detail = if (sets == 1) "1 set · ${pe.repsText()} reps" else "$sets × ${pe.repsText()}"
            Text("$detail · ${Dates.rest(pe.restSeconds)}", style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel)
        }
        val tag = when {
            pe.isWarmup -> "Warm-up"
            pe.isOptional -> "Optional"
            pe.toFailure -> "To failure"
            pe.supersetGroup != null -> "Superset"
            else -> ex?.equipment?.replaceFirstChar { it.uppercase() }
        }
        if (!tag.isNullOrBlank()) Badge(tag)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.offDay(
    s: TodayUiState,
    onLogCardio: () -> Unit,
    onStartNext: () -> Unit,
    onAddWeight: () -> Unit,
) {
    item {
        HeroCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("This week", style = AppTheme.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.label, modifier = Modifier.weight(1f))
                    Text("${s.weekSessions}", style = AppTheme.type.number.copy(fontSize = 20.sp), color = AppTheme.colors.label)
                    Text(" / ${s.profile?.weeklyTarget ?: 4} sessions", style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel)
                }
                WeekDots(s.week)
                Legend()
            }
        }
    }
    item {
        val w = s.todayWeight ?: s.latestWeight
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding.dp + 4.dp)
                .cardSurface(RoundedCornerShape(22.dp))
                .clickable(role = Role.Button, onClick = onAddWeight)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (s.todayWeight != null) "Weight this morning" else "Latest weight", style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel)
                if (w != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(Units.formatWeight(w.weightKg, s.units, withUnit = false), style = AppTheme.type.number.copy(fontSize = 30.sp), color = AppTheme.colors.label)
                        Text(" ${s.units.weightUnit}", style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    s.vsAverage?.let { Text("${signed(it, s.units)} vs 7-day average", style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel) }
                    if (s.todayWeight == null) Text("Tap to add today’s", style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel)
                } else {
                    Text("Add weight", style = AppTheme.type.number, color = AppTheme.colors.label)
                }
            }
            if (s.trend.size >= 2) Sparkline(s.trend)
        }
    }
    item {
        Column(Modifier.padding(horizontal = ScreenPadding.dp + 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Log cardio", onClick = onLogCardio, icon = Icons.Rounded.MonitorHeart)
            s.nextDay?.let { next ->
                TintedButton("Train anyway: start ${next.name}", onClick = onStartNext, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun WeekDots(week: List<WeekDot>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        week.forEach { d ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    d.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = AppTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = AppTheme.colors.secondaryLabel,
                )
                DayMark(d.gym, d.cardio, d.isToday, d.isFuture, if (d.isToday && !d.gym && !d.cardio) "${d.date.dayOfMonth}" else "")
            }
        }
    }
}

/** Gym day: filled accent square. Cardio only: blue ring. Today: white outline. */
@Composable
fun DayMark(gym: Boolean, cardio: Boolean, today: Boolean, future: Boolean, text: String, size: androidx.compose.ui.unit.Dp = 36.dp) {
    val c = AppTheme.colors
    val shape = if (cardio && !gym) CircleShape else RoundedCornerShape(10.dp)
    val desc = when {
        gym && cardio -> "gym and cardio"
        gym -> "gym"
        cardio -> "cardio"
        today -> "today"
        else -> "no session"
    }
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(
                when {
                    gym -> c.accent
                    cardio || today -> Color.Transparent
                    future -> Color.Transparent
                    else -> c.chip
                }
            )
            .then(
                when {
                    gym && cardio -> Modifier.border(3.dp, c.cardio, shape)
                    cardio -> Modifier.border(2.dp, c.cardio, shape)
                    today -> Modifier.border(2.dp, c.label, shape)
                    future -> Modifier.border(1.dp, c.border, shape)
                    else -> Modifier
                }
            )
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = AppTheme.type.caption.copy(fontWeight = FontWeight.Bold),
            color = when { gym -> c.onAccent; cardio -> c.cardio; else -> c.label },
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(AppTheme.colors.accent))
            Text("  Gym", style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).border(2.dp, AppTheme.colors.cardio, CircleShape))
            Text("  Cardio", style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel)
        }
    }
}

@Composable
private fun Sparkline(points: List<ChartPoint>) {
    val color = AppTheme.colors.accent
    Canvas(Modifier.size(width = 120.dp, height = 48.dp).semantics { contentDescription = "Weight trend, last two weeks" }) {
        val minY = points.minOf { it.y }; val maxY = points.maxOf { it.y }
        val minX = points.minOf { it.x }; val maxX = points.maxOf { it.x }.coerceAtLeast(minX + 1)
        val range = (maxY - minY).takeIf { it > 0.01 } ?: 1.0
        val path = Path()
        points.sortedBy { it.x }.forEachIndexed { i, p ->
            val x = (p.x - minX).toFloat() / (maxX - minX) * size.width
            val y = (size.height - 4.dp.toPx()) - ((p.y - minY) / range).toFloat() * (size.height - 8.dp.toPx())
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
