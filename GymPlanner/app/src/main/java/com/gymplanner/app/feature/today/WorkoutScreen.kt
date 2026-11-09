package com.gymplanner.app.feature.today

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.core.data.SetLog
import com.gymplanner.app.core.data.summaryText
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.model.Units
import com.gymplanner.app.core.ui.ActionSheet
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.Badge
import com.gymplanner.app.core.ui.CardShape
import com.gymplanner.app.core.ui.CheckCircle
import com.gymplanner.app.core.ui.ConfirmDialog
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.NumberPadSheet
import com.gymplanner.app.core.ui.ScreenPadding
import com.gymplanner.app.core.ui.SheetAction
import com.gymplanner.app.core.ui.TintedButton
import com.gymplanner.app.core.ui.appViewModel
import kotlinx.coroutines.delay

private enum class Field { WEIGHT, REPS }
private data class Editing(val block: ExerciseBlock, val set: SetLog, val field: Field, val tickAfter: Boolean = false)

@Composable
fun WorkoutScreen(sessionId: Long, onClose: () -> Unit, onFinished: (Long) -> Unit) {
    val vm = appViewModel(key = "workout$sessionId") { WorkoutViewModel(it, sessionId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<Editing?>(null) }
    var menuFor by remember { mutableStateOf<ExerciseBlock?>(null) }
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    fun finish() { leaving = true; vm.finish(context) { onFinished(sessionId) } }
    fun discard() { leaving = true; vm.discard(context, onClose) }

    // Keep the screen awake while training.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Ask once for notification permission so the rest timer can show on the lock screen.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // If the session was finished or discarded elsewhere, leave.
    LaunchedEffect(s.loaded, s.session) {
        if (!leaving && s.loaded && (s.session == null || s.session?.finishedAt != null)) onClose()
    }
    val session = s.session ?: return

    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(1000) }
    }

    Column(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        // Top bar
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp)) {
                Box(
                    Modifier.align(Alignment.CenterStart).size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.KeyboardArrowDown, "Minimise workout", tint = AppTheme.colors.accent, modifier = Modifier.size(30.dp)) }
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(session.dayName, style = AppTheme.type.headline, color = AppTheme.colors.label, maxLines = 1)
                    Text(Dates.duration(now - session.startedAt), style = AppTheme.type.footnote.copy(fontWeight = FontWeight.Medium), color = AppTheme.colors.secondaryLabel)
                }
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .background(AppTheme.colors.green)
                        .clickable(role = Role.Button) {
                            if (s.doneSets == s.totalSets && s.doneSets > 0) finish() else confirmFinish = true
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) { Text("Finish", style = AppTheme.type.headline, color = Color.White) }
            }
            val progress = if (s.totalSets == 0) 0f else s.doneSets / s.totalSets.toFloat()
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(4.dp).clip(CircleShape),
                color = AppTheme.colors.green,
                trackColor = AppTheme.colors.fill,
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {},
            )
            Text(
                "${s.doneSets} of ${s.totalSets} sets" + if (session.isLight) "  ·  Light day" else "",
                style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 4.dp),
            )
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Spacer(Modifier.height(4.dp)) }
            if (session.isLight) {
                item {
                    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Eco, null, tint = AppTheme.colors.green, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Light day: optional exercises left out, one set less each.", style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel)
                    }
                }
            }
            items(s.blocks, key = { it.order }) { block ->
                ExerciseCard(
                    block = block,
                    units = s.units,
                    onEdit = { set, field -> editing = Editing(block, set, field) },
                    onToggle = { row ->
                        when (val r = vm.toggle(context, block, row)) {
                            is WorkoutViewModel.TickResult.NeedReps -> editing = Editing(block, r.set, Field.REPS, tickAfter = true)
                            WorkoutViewModel.TickResult.Done -> Unit
                        }
                    },
                    onAddSet = { vm.addSet(block) },
                    onMenu = { menuFor = block },
                    onSkip = { vm.skip(block) },
                )
            }
            if (s.blocks.isEmpty()) {
                item {
                    Text(
                        "No exercises left in this workout.",
                        style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
            }
            item {
                GroupedSection {
                    ListRow("Discard Workout", titleColor = AppTheme.colors.red, showChevron = false, onClick = { confirmDiscard = true })
                }
            }
            item { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
        }

        RestBar()
    }

    editing?.let { e ->
        val units = s.units
        if (e.field == Field.WEIGHT) {
            val prefill = e.set.weightKg ?: e.block.rows.firstOrNull { it.set.id == e.set.id }?.previous?.weightKg
            NumberPadSheet(
                title = "${e.block.name} · Set ${e.set.setNumber}",
                initialValue = prefill?.let { Units.kgToDisplay(it, units) },
                unit = units.weightUnit,
                step = if (units == UnitSystem.METRIC) 2.5 else 5.0,
                allowZero = true,
                maxValue = 2000.0,
                confirmText = "Set Weight",
                onDismiss = { editing = null },
                onConfirm = { v -> vm.setWeight(e.set, Units.displayToKg(v, units)); editing = null },
            )
        } else {
            val prefill = e.set.reps ?: e.block.rows.firstOrNull { it.set.id == e.set.id }?.previous?.reps
            NumberPadSheet(
                title = "${e.block.name} · Set ${e.set.setNumber}",
                initialValue = prefill?.toDouble(),
                unit = "reps",
                allowDecimal = false,
                step = 1.0,
                maxValue = 200.0,
                confirmText = if (e.tickAfter) "Done" else "Set Reps",
                onDismiss = { editing = null },
                onConfirm = { v ->
                    if (e.tickAfter) vm.complete(context, e.block, e.set.copy(reps = v.toInt())) else vm.setReps(e.set, v.toInt())
                    editing = null
                },
            )
        }
    }

    menuFor?.let { b ->
        ActionSheet(
            title = b.name,
            actions = listOfNotNull(
                SheetAction("Add Set") { vm.addSet(b) },
                if (b.rows.size > 1) SheetAction("Remove Last Set") { vm.removeLastSet(b) } else null,
                SheetAction("Skip Exercise", destructive = true) { vm.skip(b) },
            ),
            onDismiss = { menuFor = null },
        )
    }
    if (confirmFinish) {
        val none = s.doneSets == 0
        ConfirmDialog(
            title = if (none) "No sets ticked" else "Finish workout?",
            message = if (none) "You haven’t ticked any sets yet. Discard this workout instead?"
            else "${s.totalSets - s.doneSets} set(s) aren’t ticked and won’t be saved.",
            confirmText = if (none) "Discard" else "Finish",
            destructive = none,
            onDismiss = { confirmFinish = false },
            onConfirm = {
                confirmFinish = false
                if (none) discard() else finish()
            },
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = "Discard workout?",
            message = "Everything logged in this workout will be deleted.",
            confirmText = "Discard",
            onDismiss = { confirmDiscard = false },
            onConfirm = { confirmDiscard = false; discard() },
        )
    }
}

@Composable
private fun ExerciseCard(
    block: ExerciseBlock,
    units: UnitSystem,
    onEdit: (SetLog, Field) -> Unit,
    onToggle: (SetRow) -> Unit,
    onAddSet: () -> Unit,
    onMenu: () -> Unit,
    onSkip: () -> Unit,
) {
    val plan = block.plan
    val greyed = plan?.isOptional == true && !block.anyDone
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding.dp)
            .clip(CardShape)
            .background(AppTheme.colors.card)
    ) {
        // Header
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).alpha(if (greyed) 0.55f else 1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(block.name, style = AppTheme.type.title3, color = AppTheme.colors.accent, modifier = Modifier.weight(1f, fill = false))
                    if (plan?.isWarmup == true) { Spacer(Modifier.width(8.dp)); Badge("Warm-up", AppTheme.colors.orange) }
                }
                if (plan != null) {
                    Text(plan.summaryText(), style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel)
                    if (plan.note.isNotBlank()) {
                        Text(plan.note, style = AppTheme.type.footnote.copy(fontStyle = FontStyle.Italic), color = AppTheme.colors.secondaryLabel)
                    }
                }
            }
            if (greyed) {
                TintedButton("Skip", onClick = onSkip, compact = true, color = AppTheme.colors.secondaryLabel, modifier = Modifier.padding(top = 2.dp))
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onMenu),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.MoreHoriz, "More options for ${block.name}", tint = AppTheme.colors.accent) }
        }
        // Column labels
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            HeaderCell("SET", Modifier.width(40.dp))
            HeaderCell("PREVIOUS", Modifier.weight(1f))
            HeaderCell(units.weightUnit.uppercase(), Modifier.width(84.dp))
            Spacer(Modifier.width(8.dp))
            HeaderCell("REPS", Modifier.width(68.dp))
            Spacer(Modifier.width(48.dp))
        }
        block.rows.forEach { row -> SetRowView(row, units, plan?.isWarmup == true, onEdit, onToggle) }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onAddSet)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, tint = AppTheme.colors.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add Set", style = AppTheme.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.accent)
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(text, style = AppTheme.type.caption.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.secondaryLabel, modifier = modifier, textAlign = TextAlign.Center)
}

@Composable
private fun SetRowView(
    row: SetRow,
    units: UnitSystem,
    warmup: Boolean,
    onEdit: (SetLog, Field) -> Unit,
    onToggle: (SetRow) -> Unit,
) {
    val s = row.set
    val prev = row.previous
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (s.completed) AppTheme.colors.green.copy(alpha = 0.13f) else Color.Transparent)
            .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (warmup) "W${s.setNumber}" else "${s.setNumber}",
                    style = AppTheme.type.headline,
                    color = if (warmup) AppTheme.colors.orange else AppTheme.colors.label,
                )
                if (row.isPr) Badge("PR", AppTheme.colors.orange)
            }
        }
        Text(
            prev?.let { "${Units.formatWeight(it.weightKg ?: 0.0, units, withUnit = false)} × ${it.reps ?: 0}" } ?: "—",
            style = AppTheme.type.subhead,
            color = AppTheme.colors.secondaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        ValueBox(
            text = s.weightKg?.let { Units.formatWeight(it, units, withUnit = false) }
                ?: prev?.weightKg?.let { Units.formatWeight(it, units, withUnit = false) } ?: "–",
            isPlaceholder = s.weightKg == null,
            label = "Weight",
            modifier = Modifier.width(84.dp),
        ) { onEdit(s, Field.WEIGHT) }
        Spacer(Modifier.width(8.dp))
        ValueBox(
            text = s.reps?.toString() ?: prev?.reps?.toString() ?: "–",
            isPlaceholder = s.reps == null,
            label = "Reps",
            modifier = Modifier.width(68.dp),
        ) { onEdit(s, Field.REPS) }
        CheckCircle(checked = s.completed, onClick = { onToggle(row) })
    }
}

/** Tappable number cell. Grey text means "pre-filled from last time". */
@Composable
private fun ValueBox(text: String, isPlaceholder: Boolean, label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppTheme.colors.fill)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$label $text" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = AppTheme.type.title3.copy(fontWeight = if (isPlaceholder) FontWeight.Normal else FontWeight.SemiBold),
            color = if (isPlaceholder) AppTheme.colors.tertiaryLabel else AppTheme.colors.label,
        )
    }
}

/** Countdown bar shown while resting, with −15 / +15 / Skip. */
@Composable
private fun RestBar() {
    val context = LocalContext.current
    val rest by RestTimer.state.collectAsStateWithLifecycle()
    val now by produceState(System.currentTimeMillis(), rest) {
        while (rest != null) { value = System.currentTimeMillis(); delay(250) }
    }
    AnimatedVisibility(visible = rest != null, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
        val r = rest ?: return@AnimatedVisibility
        val remaining = r.remainingMs(now)
        Column(Modifier.fillMaxWidth().background(AppTheme.colors.card).navigationBarsPadding()) {
            LinearProgressIndicator(
                progress = { (remaining / (r.totalSeconds * 1000f)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = AppTheme.colors.accent,
                trackColor = Color.Transparent,
                drawStopIndicator = {},
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Rest  ${Dates.duration(remaining + 999)}", style = AppTheme.type.title2, color = AppTheme.colors.label)
                    Text(r.label, style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel, maxLines = 1)
                }
                TintedButton("−15", onClick = { RestTimer.add(context, -15) }, compact = true)
                Spacer(Modifier.width(6.dp))
                TintedButton("+15", onClick = { RestTimer.add(context, 15) }, compact = true)
                Spacer(Modifier.width(6.dp))
                TintedButton("Skip", onClick = { RestTimer.stop(context) }, compact = true, color = AppTheme.colors.secondaryLabel)
            }
        }
    }
}
