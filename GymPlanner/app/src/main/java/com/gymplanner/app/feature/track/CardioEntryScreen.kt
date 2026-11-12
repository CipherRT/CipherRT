package com.gymplanner.app.feature.track

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.core.data.CardioMachine
import com.gymplanner.app.core.data.CardioSession
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.model.Units
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.ConfirmDialog
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.core.ui.FieldRow
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.NavBar
import com.gymplanner.app.core.ui.NumberPadSheet
import com.gymplanner.app.core.ui.PrimaryButton
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.ScreenPadding
import com.gymplanner.app.core.ui.appViewModel
import com.gymplanner.app.core.ui.cardSurface

private enum class CardioField { MINUTES, KCAL, DISTANCE, HR }

/** Log (or edit) a cardio session on any machine: minutes and kcal, plus optional distance and heart rate. */
@Composable
fun CardioEntryScreen(editId: Long?, onDone: () -> Unit) {
    val vm = appViewModel { CardioViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    var existing by remember { mutableStateOf<CardioSession?>(null) }
    val units = UnitSystem.from(s.profile?.unitSystem)

    var machine by remember { mutableStateOf(CardioMachine.BIKE) }
    var customName by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf<Int?>(null) }
    var kcal by remember { mutableStateOf<Int?>(null) }
    var distanceKm by remember { mutableStateOf<Double?>(null) }
    var hr by remember { mutableStateOf<Int?>(null) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(Dates.today()) }

    var editing by remember { mutableStateOf<CardioField?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(editId) {
        val e = editId?.let { vm.load(it) } ?: return@LaunchedEffect
        existing = e
        machine = CardioMachine.from(e.machine); customName = e.customName
        minutes = e.minutes; kcal = e.kcal; distanceKm = e.distanceKm
        hr = e.avgHeartRate; note = e.note; date = e.date
    }
    val distUnit = if (units == UnitSystem.METRIC) "km" else "mi"
    val kmPerMile = 1.609344
    val valid = (minutes ?: 0) > 0 && (kcal ?: 0) > 0 && (machine != CardioMachine.CUSTOM || customName.isNotBlank())

    Column(Modifier.fillMaxSize().imePadding()) {
        NavBar(if (editId == null) "Log cardio" else "Edit cardio", onBack = onDone)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text(
                    Dates.relative(date), style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item {
                // Machine buttons, three per row
                Column(Modifier.padding(horizontal = ScreenPadding.dp + 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardioMachine.entries.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { m ->
                                MachineButton(if (m == CardioMachine.CUSTOM) "+ Custom" else m.label, machine == m, Modifier.weight(1f)) { machine = m }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            if (machine == CardioMachine.CUSTOM) {
                item {
                    GroupedSection {
                        FieldRow(customName, { if (it.length <= 30) customName = it }, placeholder = "Machine or activity, e.g. Swimming", label = "Name")
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = ScreenPadding.dp + 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BigField("Minutes", minutes?.toString(), Modifier.weight(1f), highlight = minutes == null) { editing = CardioField.MINUTES }
                        BigField("Calories (kcal)", kcal?.toString(), Modifier.weight(1f), highlight = minutes != null && kcal == null) { editing = CardioField.KCAL }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallField("Distance ($distUnit) · optional", distanceKm?.let { Units.formatNumber(if (units == UnitSystem.METRIC) it else it / kmPerMile, 2) }, Modifier.weight(1f)) { editing = CardioField.DISTANCE }
                        SmallField("Avg heart rate · optional", hr?.toString(), Modifier.weight(1f)) { editing = CardioField.HR }
                    }
                }
            }
            item {
                GroupedSection {
                    FieldRow(note, { if (it.length <= 100) note = it }, placeholder = "Note (optional)")
                    if (existing != null) {
                        RowDivider()
                        ListRow("Delete", titleColor = AppTheme.colors.red, showChevron = false, onClick = { confirmDelete = true })
                    }
                }
            }
        }
        PrimaryButton(
            "Save cardio",
            enabled = valid,
            onClick = {
                val p = s.profile ?: return@PrimaryButton
                vm.save(
                    CardioSession(
                        id = existing?.id ?: 0, profileId = p.id, date = date, machine = machine.key,
                        customName = if (machine == CardioMachine.CUSTOM) customName.trim() else "",
                        minutes = minutes ?: 0, kcal = kcal ?: 0, distanceKm = distanceKm, avgHeartRate = hr, note = note.trim(),
                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    ),
                    onDone,
                )
            },
            modifier = Modifier.navigationBarsPadding().padding(16.dp),
        )
    }

    when (editing) {
        CardioField.MINUTES -> NumberPadSheet("Minutes", minutes?.toDouble(), "min", allowDecimal = false, maxValue = 600.0, step = 5.0,
            onDismiss = { editing = null }, onConfirm = { minutes = it.toInt(); editing = if (kcal == null) CardioField.KCAL else null })
        CardioField.KCAL -> NumberPadSheet("Calories", kcal?.toDouble(), "kcal", allowDecimal = false, maxValue = 5000.0, step = 10.0,
            onDismiss = { editing = null }, onConfirm = { kcal = it.toInt(); editing = null })
        CardioField.DISTANCE -> NumberPadSheet(
            "Distance", distanceKm?.let { if (units == UnitSystem.METRIC) it else it / kmPerMile }, distUnit, maxValue = 300.0, allowZero = true,
            onDismiss = { editing = null },
            onConfirm = { v -> distanceKm = if (v <= 0) null else if (units == UnitSystem.METRIC) v else v * kmPerMile; editing = null },
        )
        CardioField.HR -> NumberPadSheet("Average heart rate", hr?.toDouble(), "bpm", allowDecimal = false, maxValue = 230.0, allowZero = true,
            onDismiss = { editing = null }, onConfirm = { v -> hr = v.toInt().takeIf { it > 0 }; editing = null })
        null -> Unit
    }
    val toDelete = existing
    if (confirmDelete && toDelete != null) {
        ConfirmDialog("Delete this cardio session?", "It’s removed from your week and calendar.", "Delete",
            onDismiss = { confirmDelete = false }, onConfirm = { confirmDelete = false; vm.delete(toDelete, onDone) })
    }
}

@Composable
private fun MachineButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val c = AppTheme.colors
    Text(
        label,
        style = AppTheme.type.subhead.copy(fontWeight = FontWeight.SemiBold),
        color = if (selected) c.onAccent else c.label,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(if (selected) Modifier.clip(shape).background(c.accent) else Modifier.cardSurface(shape))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 14.dp),
    )
}

/** A big tap-to-type number box (opens the number pad). [highlight] marks the one to fill next. */
@Composable
private fun BigField(label: String, value: String?, modifier: Modifier, highlight: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .cardSurface(shape)
            .then(if (highlight) Modifier.border(2.dp, AppTheme.colors.accent, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel)
        Text(value ?: "—", style = AppTheme.type.number.copy(fontSize = 34.sp), color = if (value == null) AppTheme.colors.tertiaryLabel else AppTheme.colors.label)
    }
}

@Composable
private fun SmallField(label: String, value: String?, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.cardSurface(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel, maxLines = 1)
        Text(value ?: "—", style = AppTheme.type.headline.copy(fontSize = 18.sp), color = if (value == null) AppTheme.colors.tertiaryLabel else AppTheme.colors.label)
    }
}
