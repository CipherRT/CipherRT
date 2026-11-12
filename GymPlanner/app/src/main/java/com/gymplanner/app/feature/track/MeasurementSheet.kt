package com.gymplanner.app.feature.track

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymplanner.app.core.data.BodyMeasurement
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.model.Units
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.NumberPadSheet
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.SheetHeader

/** Add or edit one day's measurements. Every field is optional; values stored in cm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementSheet(
    profileId: Long,
    units: UnitSystem,
    existing: BodyMeasurement?,
    onSave: (BodyMeasurement) -> Unit,
    onDelete: (BodyMeasurement) -> Unit,
    onDismiss: () -> Unit,
) {
    var m by remember { mutableStateOf(existing ?: BodyMeasurement(profileId = profileId, date = Dates.today())) }
    var editing by remember { mutableStateOf<Measure?>(null) }
    val unit = if (units == UnitSystem.METRIC) "cm" else "in"
    val anyValue = Measure.entries.any { m.value(it) != null }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppTheme.colors.background,
        dragHandle = null,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            SheetHeader(
                if (existing == null) "Measurements" else "Edit measurements",
                onCancel = onDismiss, doneText = "Save", doneEnabled = anyValue, onDone = { onSave(m) },
            )
            GroupedSection(footer = "Measure at the widest point, relaxed, same time of day. ${Dates.relative(m.date)}.") {
                Measure.entries.forEachIndexed { i, k ->
                    ListRow(
                        k.label,
                        value = m.value(k)?.let { Units.formatNumber(Units.cmToDisplay(it, units)) + " $unit" } ?: "Add",
                        onClick = { editing = k },
                    )
                    if (i < Measure.entries.lastIndex) RowDivider()
                }
            }
            if (existing != null) {
                Spacer(Modifier.height(16.dp))
                GroupedSection {
                    ListRow("Delete", titleColor = AppTheme.colors.red, showChevron = false, onClick = { onDelete(existing) })
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    editing?.let { k ->
        NumberPadSheet(
            k.label, m.value(k)?.let { Units.cmToDisplay(it, units) }, unit, maxValue = if (units == UnitSystem.METRIC) 250.0 else 100.0,
            allowZero = true,
            onDismiss = { editing = null },
            onConfirm = { v ->
                val cm = if (v <= 0) null else Units.displayToCm(v, units)
                m = when (k) {
                    Measure.WAIST -> m.copy(waistCm = cm); Measure.ARM -> m.copy(armCm = cm)
                    Measure.CHEST -> m.copy(chestCm = cm); Measure.THIGH -> m.copy(thighCm = cm)
                }
                editing = null
            },
        )
    }
}
