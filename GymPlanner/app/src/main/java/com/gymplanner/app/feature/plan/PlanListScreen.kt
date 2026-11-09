package com.gymplanner.app.feature.plan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.core.data.Plan
import com.gymplanner.app.core.ui.ActionSheet
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.ConfirmDialog
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.NavBar
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.SheetAction
import com.gymplanner.app.core.ui.TextInputDialog
import com.gymplanner.app.core.ui.appViewModel
import kotlinx.coroutines.launch

@Composable
fun PlanListScreen(onBack: () -> Unit, onImport: (Uri) -> Unit) {
    val vm = appViewModel { PlanListViewModel(it) }
    val plans by vm.plans.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var actionsFor by remember { mutableStateOf<Plan?>(null) }
    var renaming by remember { mutableStateOf<Plan?>(null) }
    var deleting by remember { mutableStateOf<Plan?>(null) }
    var creating by remember { mutableStateOf(false) }
    var addedTemplate by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(onImport) }

    Column(Modifier.fillMaxSize()) {
        NavBar("Plans", onBack = onBack, backLabel = "Plan")
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item {
                GroupedSection(header = "My plans", footer = "The active plan is the one used on the Today tab. Tap a plan for options.") {
                    if (plans.isEmpty()) ListRow("No plans yet")
                    plans.forEachIndexed { i, p ->
                        ListRow(
                            title = p.name,
                            subtitle = "${p.daysPerWeek} days · created ${Dates.relativeMillis(p.createdAt)}",
                            onClick = { actionsFor = p },
                            trailing = if (p.isActive) {
                                { Icon(Icons.Rounded.CheckCircle, "Active", tint = AppTheme.colors.accent) }
                            } else null,
                        )
                        if (i < plans.lastIndex) RowDivider()
                    }
                }
            }
            item {
                GroupedSection(header = "Add a plan") {
                    ListRow("New Empty Plan", icon = Icons.Rounded.Add, iconColor = AppTheme.colors.accent, onClick = { creating = true })
                    RowDivider(startIndent = 60.dp)
                    ListRow(
                        "Import from File", subtitle = "Open a .gymplan.json a friend sent you",
                        icon = Icons.Rounded.FileOpen, iconColor = AppTheme.colors.green,
                        onClick = { picker.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")) },
                    )
                }
            }
            if (vm.templates.isNotEmpty()) {
                item {
                    GroupedSection(header = "Templates", footer = addedTemplate?.let { "“$it” was added to your plans." }) {
                        vm.templates.forEachIndexed { i, t ->
                            ListRow(
                                t.plan.name,
                                subtitle = t.plan.days.joinToString(" · ") { it.name },
                                icon = Icons.Rounded.AutoAwesome, iconColor = AppTheme.colors.orange,
                                onClick = { vm.addTemplate(t); addedTemplate = t.plan.name },
                                trailing = { Icon(Icons.Rounded.Add, "Add", tint = AppTheme.colors.accent) },
                            )
                            if (i < vm.templates.lastIndex) RowDivider(startIndent = 60.dp)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    actionsFor?.let { p ->
        ActionSheet(
            title = p.name,
            actions = buildList {
                if (!p.isActive) add(SheetAction("Make Active Plan") { vm.setActive(p) })
                add(SheetAction("Rename") { renaming = p })
                add(SheetAction("Duplicate") { vm.duplicate(p) })
                add(SheetAction("Share") { scope.launch { PlanShare.share(context, vm.export(p)) } })
                add(SheetAction("Delete Plan", destructive = true) { deleting = p })
            },
            onDismiss = { actionsFor = null },
        )
    }
    renaming?.let { p ->
        TextInputDialog("Rename Plan", initial = p.name, onDismiss = { renaming = null }, onConfirm = { vm.rename(p, it); renaming = null })
    }
    deleting?.let { p ->
        ConfirmDialog(
            title = "Delete “${p.name}”?",
            message = "The plan is removed. Your past workouts and their numbers are kept.",
            confirmText = "Delete",
            onDismiss = { deleting = null },
            onConfirm = { vm.delete(p); deleting = null },
        )
    }
    if (creating) {
        TextInputDialog("New Plan", placeholder = "Plan name", confirmText = "Create", onDismiss = { creating = false }, onConfirm = { vm.createEmpty(it); creating = false })
    }
}
