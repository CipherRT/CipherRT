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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.ProfileMenuTargets
import com.gymplanner.app.core.model.MuscleGroups
import com.gymplanner.app.core.data.DayFrequency
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.Badge
import com.gymplanner.app.core.ui.CircleIconButton
import com.gymplanner.app.core.ui.EmptyState
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.LargeTitle
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.ReorderableColumn
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.TextAction
import com.gymplanner.app.core.ui.TextInputDialog
import com.gymplanner.app.core.ui.TintedButton
import com.gymplanner.app.core.ui.appViewModel
import com.gymplanner.app.feature.settings.ProfileMenuButton
import kotlinx.coroutines.launch

@Composable
fun PlanScreen(menu: ProfileMenuTargets, onOpenDay: (Long) -> Unit, onOpenPlans: () -> Unit) {
    val vm = appViewModel { PlanViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var addingDay by remember { mutableStateOf(false) }
    var creatingPlan by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            LargeTitle(title = s.plan?.name ?: "Plan", eyebrow = if (s.plan != null) "Active plan" else null) {
                if (s.plan != null) {
                    CircleIconButton(Icons.Rounded.IosShare, "Share plan", onClick = {
                        scope.launch { vm.exportActive()?.let { PlanShare.share(context, it) } }
                    })
                }
                CircleIconButton(Icons.AutoMirrored.Rounded.List, "All plans", onClick = onOpenPlans)
                ProfileMenuButton(menu)
            }
        }
        if (s.loaded && s.plan == null) {
            item {
                EmptyState(
                    Icons.Rounded.CalendarMonth, "No active plan",
                    "Create a plan, add a template, or import one a friend shared.",
                    actionText = "Create Plan", onAction = { creatingPlan = true },
                )
            }
            item {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TextAction("Templates & import", onOpenPlans)
                }
            }
        }
        val plan = s.plan
        if (plan != null) {
            item {
                GroupedSection(
                    header = "${s.days.size} ${if (s.days.size == 1) "day" else "days"}",
                    footer = if (s.days.size > 1) "Drag ≡ to change the order. Tap a day to edit its exercises." else null,
                ) {
                    if (s.days.isEmpty()) {
                        ListRow("No days yet", subtitle = "Add a day like “Push”, “Legs” or “Upper”.")
                    }
                    ReorderableColumn(
                        items = s.days,
                        key = { it.day.id },
                        onReorder = { list -> vm.reorder(list.map { it.day }) },
                    ) { card, handle, dragging, index ->
                        Column(Modifier.background(if (dragging) AppTheme.colors.cardElevated else AppTheme.colors.card)) {
                            ListRow(
                                title = card.day.name,
                                subtitle = buildString {
                                    append("${card.exerciseCount} exercise${if (card.exerciseCount == 1) "" else "s"}")
                                    if (card.muscles.isNotEmpty()) append(" · " + card.muscles.take(3).joinToString(", ") { MuscleGroups.label(it) })
                                },
                                onClick = { onOpenDay(card.day.id) },
                                leading = { DayNumber(index + 1) },
                                trailing = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (card.day.frequency == DayFrequency.BIWEEKLY) { Badge("Every 2 wks"); Spacer(Modifier.width(8.dp)) }
                                        Box(handle.size(40.dp), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.DragHandle, "Reorder", tint = AppTheme.colors.tertiaryLabel)
                                        }
                                    }
                                },
                            )
                            if (index < s.days.lastIndex) RowDivider(startIndent = 60.dp)
                        }
                    }
                }
            }
            item {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TintedButton("Add Day", onClick = { addingDay = true }, icon = Icons.Rounded.Add)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (addingDay) {
        TextInputDialog(
            title = "New Day",
            message = "Give it a short name, like “Push” or “Legs”.",
            placeholder = "Day name",
            confirmText = "Add",
            onDismiss = { addingDay = false },
            onConfirm = { vm.addDay(it); addingDay = false },
        )
    }
    if (creatingPlan) {
        TextInputDialog(
            title = "New Plan",
            initial = "My Plan",
            confirmText = "Create",
            onDismiss = { creatingPlan = false },
            onConfirm = { vm.createPlan(it); creatingPlan = false },
        )
    }
}

@Composable
fun DayNumber(n: Int) {
    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(AppTheme.colors.chip), contentAlignment = Alignment.Center) {
        Text("$n", style = AppTheme.type.subhead.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.chipText)
    }
}
