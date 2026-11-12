package com.gymplanner.app.feature.track

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.ProfileMenuTargets
import com.gymplanner.app.core.data.BodyMeasurement
import com.gymplanner.app.core.data.displayName
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.model.Units
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.Card
import com.gymplanner.app.core.ui.ChartPoint
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.core.ui.EmptyState
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.LargeTitle
import com.gymplanner.app.core.ui.LineChart
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.PrimaryButton
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.ScreenPadding
import com.gymplanner.app.core.ui.SegmentedControl
import com.gymplanner.app.core.ui.appViewModel
import com.gymplanner.app.feature.settings.ProfileMenuButton
import com.gymplanner.app.feature.weight.WeightSection
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Track tab: weight chart, cardio log and body measurements behind one segmented control. */
@Composable
fun TrackScreen(menu: ProfileMenuTargets, onLogCardio: (Long?) -> Unit, initial: TrackSection = TrackSection.WEIGHT) {
    var section by rememberSaveable { mutableStateOf(initial) }
    val top: @Composable () -> Unit = {
        Column {
            LargeTitle("Track") { ProfileMenuButton(menu) }
            SegmentedControl(
                TrackSection.entries, section, { section = it }, label = { it.label },
                modifier = Modifier.padding(horizontal = ScreenPadding.dp + 4.dp),
            )
        }
    }
    when (section) {
        TrackSection.WEIGHT -> WeightSection(top)
        TrackSection.CARDIO -> CardioSection(top, onLogCardio)
        TrackSection.BODY -> BodySection(top)
    }
}

@Composable
private fun CardioSection(top: @Composable () -> Unit, onLogCardio: (Long?) -> Unit) {
    val vm = appViewModel { CardioViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val units = UnitSystem.from(s.profile?.unitSystem)
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { top() }
        item {
            PrimaryButton("Log cardio", onClick = { onLogCardio(null) }, icon = Icons.Rounded.MonitorHeart, modifier = Modifier.padding(horizontal = 20.dp))
        }
        item {
            Card {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("This week", style = AppTheme.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.label, modifier = Modifier.weight(1f))
                    Text("${s.weekKcal}", style = AppTheme.type.number.copy(fontSize = 20.sp), color = AppTheme.colors.label)
                    Text(" kcal · ${s.weekSessions} session${if (s.weekSessions == 1) "" else "s"}", style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel)
                }
                Spacer(Modifier.height(12.dp))
                WeekBars(s.week)
            }
        }
        if (s.sessions.isEmpty()) {
            item { EmptyState(Icons.Rounded.MonitorHeart, "No cardio yet", "Log bike, treadmill, rower or any machine in minutes and kcal.") }
        } else {
            item {
                GroupedSection(header = "History") {
                    s.sessions.forEachIndexed { i, c ->
                        val extra = listOfNotNull(
                            "${c.minutes} min",
                            c.distanceKm?.let { Units.formatNumber(if (units == UnitSystem.METRIC) it else it / 1.609344, 2) + if (units == UnitSystem.METRIC) " km" else " mi" },
                            c.avgHeartRate?.let { "$it bpm" },
                        ).joinToString(" · ")
                        ListRow(
                            c.displayName(),
                            subtitle = "${Dates.relative(c.date)} · $extra" + if (c.note.isNotBlank()) "\n${c.note}" else "",
                            value = "${c.kcal} kcal",
                            onClick = { onLogCardio(c.id) },
                        )
                        if (i < s.sessions.lastIndex) RowDivider()
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** 7 bars, Monday to Sunday, in the cardio colour. */
@Composable
fun WeekBars(kcal: List<Int>) {
    val color = AppTheme.colors.cardio
    val empty = AppTheme.colors.border
    val max = (kcal.maxOrNull() ?: 0).coerceAtLeast(1)
    Column {
        Canvas(Modifier.fillMaxWidth().height(70.dp).semantics { contentDescription = "Cardio kcal per day this week: ${kcal.joinToString()}" }) {
            val gap = 8.dp.toPx()
            val w = (size.width - gap * 6) / 7
            kcal.forEachIndexed { i, v ->
                val h = if (v == 0) 4.dp.toPx() else (v / max.toFloat()) * size.height
                drawRoundRect(
                    if (v == 0) empty else color,
                    Offset(i * (w + gap), size.height - h), Size(w, h), CornerRadius(6.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { d ->
                Text(
                    d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = AppTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = AppTheme.colors.secondaryLabel,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun BodySection(top: @Composable () -> Unit) {
    val vm = appViewModel { BodyViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<BodyMeasurement?>(null) }
    var adding by remember { mutableStateOf(false) }
    val units = s.units
    fun fmt(cm: Double) = Units.formatNumber(Units.cmToDisplay(cm, units)) + if (units == UnitSystem.METRIC) " cm" else " in"

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { top() }
        item { PrimaryButton("Add measurements", onClick = { adding = true }, icon = Icons.Rounded.Add, modifier = Modifier.padding(horizontal = 20.dp)) }
        if (s.entries.isEmpty()) {
            item {
                EmptyState(Icons.Rounded.Straighten, "No measurements yet", "Optional: track waist, arm, chest and thigh to see changes the scale misses.")
            }
        } else {
            item {
                Row(Modifier.padding(horizontal = ScreenPadding.dp + 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Measure.entries.forEach { m ->
                        val latest = s.latest(m)
                        com.gymplanner.app.core.ui.StatTile(
                            m.label + (latest?.second?.let { d -> " " + (if (d > 0) "+" else "−") + Units.formatNumber(kotlin.math.abs(Units.cmToDisplay(d, units))) } ?: ""),
                            latest?.first?.let { Units.formatNumber(Units.cmToDisplay(it, units)) } ?: "—",
                            Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                Card {
                    SegmentedControl(Measure.entries, s.selected, vm::select, label = { it.label })
                    Spacer(Modifier.height(16.dp))
                    val points = s.entries.mapNotNull { e -> e.value(s.selected)?.let { ChartPoint(e.date, Units.cmToDisplay(it, units)) } }.sortedBy { it.x }
                    if (points.isEmpty()) {
                        Text("No ${s.selected.label.lowercase()} entries yet.", style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel)
                    } else {
                        LineChart(points = points, xLabel = { Dates.short(it) }, yLabel = { Units.formatNumber(it, 0) })
                    }
                }
            }
            item {
                GroupedSection(header = "History") {
                    s.entries.forEachIndexed { i, e ->
                        ListRow(
                            Dates.relative(e.date),
                            subtitle = Measure.entries.mapNotNull { m -> e.value(m)?.let { "${m.label} ${fmt(it)}" } }.joinToString(" · "),
                            onClick = { editing = e },
                        )
                        if (i < s.entries.lastIndex) RowDivider()
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    val p = s.profile
    if (p != null && (adding || editing != null)) {
        MeasurementSheet(
            profileId = p.id, units = units, existing = editing,
            onSave = { vm.save(it); adding = false; editing = null },
            onDelete = { vm.delete(it); editing = null },
            onDismiss = { adding = false; editing = null },
        )
    }
}
