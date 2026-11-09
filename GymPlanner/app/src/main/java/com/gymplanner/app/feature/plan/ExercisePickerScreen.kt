package com.gymplanner.app.feature.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.core.model.MuscleGroups
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.Badge
import com.gymplanner.app.core.ui.FieldRow
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.Pill
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.SearchBar
import com.gymplanner.app.core.ui.SheetHeader
import com.gymplanner.app.core.ui.appViewModel

@Composable
fun ExercisePickerScreen(dayId: Long, onClose: () -> Unit, onAdded: (Long) -> Unit) {
    val vm = appViewModel(key = "picker$dayId") { PickerViewModel(it, dayId) }
    val results by vm.results.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val group by vm.group.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SheetHeader(title = "Add Exercise", onCancel = onClose)
        SearchBar(query, { vm.query.value = it }, placeholder = "Search exercises")
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Pill("All", group == null) { vm.group.value = null } }
            items(MuscleGroups.all) { g -> Pill(MuscleGroups.label(g), group == g) { vm.group.value = if (group == g) null else g } }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                GroupedSection {
                    ListRow(
                        if (query.isNotBlank() && results.none { it.name.equals(query.trim(), true) }) "Create “${query.trim()}”" else "Create Custom Exercise",
                        titleColor = AppTheme.colors.accent,
                        showChevron = false,
                        onClick = { creating = true },
                        leading = { Icon(Icons.Rounded.AddCircle, null, tint = AppTheme.colors.green, modifier = Modifier.size(30.dp)) },
                    )
                }
            }
            val grouped = results.groupBy { it.muscleGroup }
            val order = MuscleGroups.all + grouped.keys.filter { it !in MuscleGroups.all }
            order.filter { it in grouped }.forEach { g ->
                item(key = g) {
                    GroupedSection(header = MuscleGroups.label(g)) {
                        val list = grouped.getValue(g)
                        list.forEachIndexed { i, ex ->
                            ListRow(
                                ex.name,
                                subtitle = ex.equipment.replaceFirstChar { it.uppercase() }.ifBlank { null },
                                showChevron = false,
                                onClick = { if (!busy) { busy = true; vm.add(ex, onAdded) } },
                                trailing = if (ex.isCustom) { { Badge("Custom", AppTheme.colors.purple) } } else null,
                            )
                            if (i < list.lastIndex) RowDivider()
                        }
                    }
                }
            }
            if (results.isEmpty()) {
                item {
                    Text(
                        "No exercises match. Create your own above.",
                        style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            }
            item { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
        }
    }

    if (creating) {
        CustomExerciseSheet(
            initialName = query.trim(),
            initialGroup = group ?: "back",
            onDismiss = { creating = false },
            onCreate = { name, g, equipment ->
                creating = false
                if (!busy) { busy = true; vm.createAndAdd(name, g, equipment, onAdded) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomExerciseSheet(
    initialName: String,
    initialGroup: String,
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var group by remember { mutableStateOf(initialGroup) }
    var equipment by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppTheme.colors.background,
        dragHandle = null,
    ) {
        Column(Modifier.navigationBarsPadding().verticalScroll(rememberScrollState())) {
            SheetHeader(
                title = "New Exercise",
                onCancel = onDismiss,
                doneText = "Add",
                doneEnabled = name.isNotBlank(),
                onDone = { onCreate(name.trim(), group, equipment.trim()) },
            )
            GroupedSection {
                FieldRow(name, { if (it.length <= 40) name = it }, placeholder = "Exercise name", label = "Name")
                RowDivider()
                FieldRow(equipment, { if (it.length <= 30) equipment = it }, placeholder = "e.g. Cable, Dumbbell", label = "Equipment")
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "MUSCLE GROUP",
                style = AppTheme.type.sectionHeader, color = AppTheme.colors.secondaryLabel,
                modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
            )
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MuscleGroups.all) { g -> Pill(MuscleGroups.label(g), group == g) { group = g } }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
