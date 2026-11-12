package com.gymplanner.app.feature.plan

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.core.data.PlanExercise
import com.gymplanner.app.core.data.targetText
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.EmptyState
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.PrimaryButton
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.SheetHeader
import com.gymplanner.app.core.ui.SwitchRow
import com.gymplanner.app.core.ui.appViewModel

/** Preview of a shared plan file before adding it. */
@Composable
fun ImportPlanScreen(uri: Uri?, onClose: () -> Unit, onImported: () -> Unit) {
    val vm = appViewModel(key = "import${uri}") { ImportViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var makeActive by remember { mutableStateOf(true) }
    var importing by remember { mutableStateOf(false) }

    LaunchedEffect(uri) {
        if (uri == null) vm.state.value = ImportState.Failed("No file was selected.")
        else vm.load { PlanShare.readText(context, uri) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        SheetHeader(title = "Import Plan", onCancel = onClose)
        when (val s = state) {
            ImportState.Loading -> Unit
            is ImportState.Failed -> EmptyState(Icons.Rounded.ErrorOutline, "Can’t open this file", s.message)
            is ImportState.Ready -> {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    item {
                        Column(Modifier.padding(horizontal = 24.dp)) {
                            Text(s.plan.name, style = AppTheme.type.title1, color = AppTheme.colors.label)
                            Text(
                                "${s.plan.days.size} days · ${s.plan.days.sumOf { it.exercises.size }} exercises. " +
                                    "Contains no weights, logs or personal info.",
                                style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel,
                            )
                        }
                    }
                    s.plan.days.forEach { day ->
                        item {
                            GroupedSection(
                                header = day.name + if (day.frequency == "biweekly") " · every 2 weeks" else "",
                                footer = day.note.ifBlank { null },
                            ) {
                                day.exercises.forEachIndexed { i, e ->
                                    val target = PlanExercise(
                                        planDayId = 0, exerciseId = 0, orderIndex = 0, sets = e.sets, repMin = e.repMin,
                                        repMax = e.repMax, toFailure = e.toFailure, restSeconds = e.restSeconds, perSide = e.perSide,
                                    ).targetText()
                                    ListRow(
                                        e.name,
                                        subtitle = listOfNotNull(
                                            target,
                                            if (e.isOptional) "optional" else null,
                                            if (e.isWarmup) "warm-up" else null,
                                            e.supersetGroup?.let { "superset ${'A' + it - 1}" },
                                            if (e.dropSets > 0) "+${e.dropSets} drop" else null,
                                        ).joinToString(" · "),
                                    )
                                    if (i < day.exercises.lastIndex) RowDivider()
                                }
                            }
                        }
                    }
                    item {
                        GroupedSection(footer = "Exercises you don’t have yet are added to your library.") {
                            SwitchRow("Make it my active plan", makeActive, { makeActive = it })
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                PrimaryButton(
                    if (importing) "Adding…" else "Add to My Plans",
                    enabled = !importing,
                    onClick = { importing = true; vm.import(s.plan, makeActive, onImported) },
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
