package com.gymplanner.app.feature.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.ProfileMenuTargets
import com.gymplanner.app.core.data.BodyWeightEntry
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.model.Units
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.Card
import com.gymplanner.app.core.ui.CircleIconButton
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.core.ui.EmptyState
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.LargeTitle
import com.gymplanner.app.core.ui.LineChart
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.SegmentedControl
import com.gymplanner.app.core.ui.appViewModel
import com.gymplanner.app.feature.settings.ProfileMenuButton

@Composable
fun WeightScreen(menu: ProfileMenuTargets) {
    val vm = appViewModel { WeightViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BodyWeightEntry?>(null) }
    val units = s.units

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            LargeTitle("Weight") {
                CircleIconButton(Icons.Rounded.Add, "Add weight", onClick = { adding = true })
                ProfileMenuButton(menu)
            }
        }
        if (s.entries.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.MonitorWeight, "No weight yet", "Add your weight to start your chart.",
                    actionText = "Add Weight", onAction = { adding = true },
                )
            }
        } else {
            item { SummaryCard(s, units) }
            item {
                Card {
                    SegmentedControl(WeightRange.entries, s.range, vm::setRange, label = { it.label })
                    Spacer(Modifier.height(16.dp))
                    if (s.points.isEmpty()) {
                        Text("No entries in this period.", style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel)
                    } else {
                        LineChart(
                            points = s.points.map { it.copy(y = Units.kgToDisplay(it.y, units)) },
                            line = s.average.map { it.copy(y = Units.kgToDisplay(it.y, units)) },
                            xLabel = { Dates.short(it) },
                            yLabel = { Units.formatNumber(it, 0) },
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Line: 7-day average · Dots: each entry",
                            style = AppTheme.type.caption,
                            color = AppTheme.colors.secondaryLabel,
                        )
                    }
                }
            }
            item {
                GroupedSection(header = "History") {
                    s.entries.forEachIndexed { i, e ->
                        val prev = s.entries.getOrNull(i + 1)
                        val delta = prev?.let { e.weightKg - it.weightKg }
                        ListRow(
                            title = Dates.relative(e.date),
                            subtitle = e.note.ifBlank { null },
                            value = Units.formatWeight(e.weightKg, units),
                            onClick = { editing = e },
                            trailing = { DeltaText(delta, units) },
                        )
                        if (i < s.entries.lastIndex) RowDivider()
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    val p = s.profile
    if (p != null && (adding || editing != null)) {
        WeightEntrySheet(
            profileId = p.id,
            units = units,
            existing = editing,
            lastWeightKg = s.latest?.weightKg,
            onDismiss = { adding = false; editing = null },
        )
    }
}

@Composable
private fun SummaryCard(s: WeightUiState, units: UnitSystem) {
    val latest = s.latest ?: return
    Card {
        Text("CURRENT", style = AppTheme.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.secondaryLabel)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Units.formatWeight(latest.weightKg, units, withUnit = false), style = AppTheme.type.bigNumber, color = AppTheme.colors.label)
            Text(" ${units.weightUnit}", style = AppTheme.type.title3, color = AppTheme.colors.secondaryLabel, modifier = Modifier.padding(bottom = 8.dp))
            Spacer(Modifier.weight(1f))
            Text(Dates.relative(latest.date), style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel, modifier = Modifier.padding(bottom = 10.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            MiniStat("Since start", s.changeSinceStart?.let { signed(it, units) } ?: "—")
            MiniStat("Last 30 days", s.change30?.let { signed(it, units) } ?: "—")
            MiniStat("BMI", s.bmi?.let { Units.formatNumber(it) } ?: "—")
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(label, style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel)
        Text(value, style = AppTheme.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.label)
    }
}

@Composable
private fun DeltaText(delta: Double?, units: UnitSystem) {
    if (delta == null || kotlin.math.abs(delta) < 0.05) return
    Text(
        signed(delta, units, withUnit = false),
        style = AppTheme.type.footnote,
        color = AppTheme.colors.secondaryLabel,
        modifier = Modifier.padding(start = 4.dp),
    )
}

fun signed(kg: Double, units: UnitSystem, withUnit: Boolean = true): String {
    val v = Units.kgToDisplay(kg, units)
    val sign = if (v > 0.05) "+" else if (v < -0.05) "−" else ""
    val n = Units.formatNumber(kotlin.math.abs(v))
    return if (withUnit) "$sign$n ${units.weightUnit}" else "$sign$n"
}
