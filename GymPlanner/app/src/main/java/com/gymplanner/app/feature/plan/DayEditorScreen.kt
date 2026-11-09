package com.gymplanner.app.feature.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.core.data.summaryText
import com.gymplanner.app.core.model.Weekdays
import com.gymplanner.app.core.ui.ActionSheet
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.Badge
import com.gymplanner.app.core.ui.ConfirmDialog
import com.gymplanner.app.core.ui.FieldRow
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.NavBar
import com.gymplanner.app.core.ui.ReorderableColumn
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.SheetAction
import com.gymplanner.app.core.ui.appViewModel

@Composable
fun DayEditorScreen(dayId: Long, onBack: () -> Unit, onAddExercise: () -> Unit, onEditExercise: (Long) -> Unit) {
    val vm = appViewModel(key = "day$dayId") { DayEditorViewModel(it, dayId) }
    val day by vm.day.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()

    // Text fields keep their own state; every change is saved straight away.
    var name by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(day?.id) { day?.let { if (name == null) { name = it.name; note = it.note } } }
    var pickWeekday by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        NavBar(day?.name?.ifBlank { "Day" } ?: "", onBack = onBack, backLabel = "Plan")
        val d = day ?: return@Column
        // Always save from the local field values so quick edits never overwrite each other.
        fun save(weekday: Int? = d.weekday) = vm.update(
            d.copy(name = name?.trim()?.ifBlank { null } ?: d.name, note = note ?: d.note, weekday = weekday)
        )
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item {
                GroupedSection(header = "Day") {
                    FieldRow(name ?: "", { name = it; save() }, placeholder = "Day name", label = "Name")
                    RowDivider()
                    ListRow("Weekday", value = d.weekday?.let { Weekdays.long(it) } ?: "Any day", onClick = { pickWeekday = true })
                    RowDivider()
                    FieldRow(note ?: "", { note = it; save() }, placeholder = "Note for the whole day (optional)", singleLine = false)
                }
            }
            item {
                GroupedSection(
                    header = "Exercises",
                    footer = if (items.size > 1) "Drag ≡ to reorder. Tap an exercise to change sets, reps and rest." else null,
                ) {
                    ReorderableColumn(items = items, key = { it.pe.id }, onReorder = vm::reorder) { item, handle, dragging, index ->
                        Column(Modifier.background(if (dragging) AppTheme.colors.cardElevated else AppTheme.colors.card)) {
                            ListRow(
                                title = item.exercise?.name ?: "Exercise",
                                subtitle = item.pe.summaryText() + (if (item.pe.note.isNotBlank()) "\n${item.pe.note}" else ""),
                                onClick = { onEditExercise(item.pe.id) },
                                leading = { DayNumber(index + 1) },
                                trailing = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (item.pe.isWarmup) { Badge("Warm-up", AppTheme.colors.orange); Spacer(Modifier.width(6.dp)) }
                                        if (item.pe.isOptional) { Badge("Optional", AppTheme.colors.secondaryLabel); Spacer(Modifier.width(6.dp)) }
                                        Box(handle.size(40.dp), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.DragHandle, "Reorder", tint = AppTheme.colors.tertiaryLabel)
                                        }
                                    }
                                },
                            )
                            RowDivider(startIndent = 60.dp)
                        }
                    }
                    ListRow(
                        "Add Exercise",
                        titleColor = AppTheme.colors.accent,
                        showChevron = false,
                        onClick = onAddExercise,
                        leading = { Icon(Icons.Rounded.AddCircle, null, tint = AppTheme.colors.green, modifier = Modifier.size(32.dp)) },
                    )
                }
            }
            item {
                GroupedSection {
                    ListRow("Delete Day", titleColor = AppTheme.colors.red, showChevron = false, onClick = { confirmDelete = true })
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }

        if (pickWeekday) {
            ActionSheet(
                title = "Pin to a weekday",
                actions = listOf(SheetAction("Any day (follow the order)") { save(weekday = null) }) +
                    (1..7).map { w -> SheetAction(Weekdays.long(w) + if (d.weekday == w) "  ✓" else "") { save(weekday = w) } },
                onDismiss = { pickWeekday = false },
            )
        }
        if (confirmDelete) {
            ConfirmDialog(
                title = "Delete “${d.name}”?",
                message = "Its exercises are removed from the plan. Past workouts are kept.",
                confirmText = "Delete",
                onDismiss = { confirmDelete = false },
                onConfirm = { confirmDelete = false; vm.delete(onBack) },
            )
        }
    }
}
