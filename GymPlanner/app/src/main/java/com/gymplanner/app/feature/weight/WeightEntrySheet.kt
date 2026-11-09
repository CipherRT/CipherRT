package com.gymplanner.app.feature.weight

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymplanner.app.core.data.BodyWeightEntry
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.model.Units
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.core.ui.FieldRow
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.NumberPadSheet
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.appContainer
import kotlinx.coroutines.launch

private const val DAY_MS = 86_400_000L

/**
 * Add or edit a body-weight entry. When adding, the last weight is pre-filled and the
 * date defaults to today — so "+ Weight" → "Save" is two taps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightEntrySheet(
    profileId: Long,
    units: UnitSystem,
    existing: BodyWeightEntry?,
    lastWeightKg: Double?,
    onDismiss: () -> Unit,
) {
    val c = appContainer()
    var date by remember { mutableLongStateOf(existing?.date ?: Dates.today()) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var pickDate by remember { mutableStateOf(false) }
    val step = if (units == UnitSystem.METRIC) 0.1 else 0.2

    NumberPadSheet(
        title = if (existing == null) "Add Weight" else "Edit Weight",
        initialValue = (existing?.weightKg ?: lastWeightKg)?.let { Units.kgToDisplay(it, units) },
        unit = units.weightUnit,
        step = step,
        maxValue = if (units == UnitSystem.METRIC) 400.0 else 880.0,
        onDismiss = onDismiss,
        onConfirm = { value ->
            val kg = Units.displayToKg(value, units)
            c.scope.launch {
                if (existing == null) c.weights.add(profileId, date, kg, note.trim())
                else c.weights.update(existing.copy(date = date, weightKg = kg, note = note.trim()))
            }
            onDismiss()
        },
        extraContent = {
            GroupedSection(modifier = Modifier.padding(vertical = 4.dp), containerColor = AppTheme.colors.cardElevated) {
                ListRow("Date", value = Dates.relative(date), onClick = { pickDate = true })
                RowDivider()
                FieldRow(note, { if (it.length <= 80) note = it }, placeholder = "Note (optional)")
                if (existing != null) {
                    RowDivider()
                    ListRow("Delete Entry", titleColor = AppTheme.colors.red, showChevron = false, onClick = {
                        c.scope.launch { c.weights.delete(existing) }
                        onDismiss()
                    })
                }
            }
        },
    )

    if (pickDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date * DAY_MS,
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / DAY_MS <= Dates.today()
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = it / DAY_MS }
                    pickDate = false
                }) { Text("Done", color = AppTheme.colors.accent) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel", color = AppTheme.colors.accent) } },
        ) { DatePicker(state = pickerState, showModeToggle = false) }
    }
}
