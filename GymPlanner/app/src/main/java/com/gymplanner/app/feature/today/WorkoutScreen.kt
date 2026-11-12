package com.gymplanner.app.feature.today

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.core.data.repsText
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.model.Units
import com.gymplanner.app.core.ui.ActionSheet
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.Badge
import com.gymplanner.app.core.ui.ConfirmDialog
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.core.ui.NumberPadSheet
import com.gymplanner.app.core.ui.PrimaryButton
import com.gymplanner.app.core.ui.SheetAction
import com.gymplanner.app.core.ui.SheetHeader
import com.gymplanner.app.core.ui.Stepper
import com.gymplanner.app.core.ui.TextAction
import com.gymplanner.app.core.ui.appViewModel
import com.gymplanner.app.core.ui.cardSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Field { WEIGHT, REPS }
private data class Editing(val page: WorkoutPage, val block: ExerciseBlock, val row: SetRow, val field: Field, val logAfter: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(sessionId: Long, onClose: () -> Unit, onFinished: (Long) -> Unit, onHowTo: (Long) -> Unit) {
    val vm = appViewModel(key = "workout$sessionId") { WorkoutViewModel(it, sessionId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Editing?>(null) }
    var rowMenu by remember { mutableStateOf<Triple<WorkoutPage, ExerciseBlock, SetRow>?>(null) }
    var plates by remember { mutableStateOf<SetRow?>(null) }
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    // Row picked by tapping it, per page (otherwise the next set to do is used).
    val selected = remember { mutableStateMapOf<Int, Long>() }

    fun finish() { leaving = true; vm.finish(context) { onFinished(sessionId) } }
    fun discard() { leaving = true; vm.discard(context, onClose) }
    fun haptic() {
        view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.KEYBOARD_TAP)
    }
    fun log(page: WorkoutPage, block: ExerciseBlock, row: SetRow, toggle: Boolean = false) {
        val r = if (toggle) vm.toggle(context, page, block, row) else vm.log(context, page, block, row)
        when (r) {
            is WorkoutViewModel.LogResult.NeedReps -> editing = Editing(page, r.block, r.row, Field.REPS, logAfter = true)
            WorkoutViewModel.LogResult.Done -> if (!row.set.completed) haptic()
        }
        selected.clear()
    }

    // Keep the screen awake while training.
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
    val units = s.units
    val pager = rememberPagerState(pageCount = { s.pages.size.coerceAtLeast(1) })
    // Open on the first page with work left.
    var positioned by remember { mutableStateOf(false) }
    LaunchedEffect(s.pages.size) {
        if (!positioned && s.pages.isNotEmpty()) {
            positioned = true
            pager.scrollToPage(s.pages.indexOfFirst { !it.allDone }.takeIf { it >= 0 } ?: 0)
        }
    }
    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(1000) }
    }

    Column(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        // Top bar
        Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp).height(60.dp)) {
            Box(
                Modifier.align(Alignment.CenterStart).size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, "Back to Today (workout keeps running)", tint = AppTheme.colors.label, modifier = Modifier.size(20.dp)) }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(session.dayName, style = AppTheme.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.label, maxLines = 1)
                Text(
                    "Exercise ${pager.currentPage + 1} of ${s.pages.size} · ${Dates.duration(now - session.startedAt)}" + if (session.isLight) " · Light" else "",
                    style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel,
                )
            }
            TextAction(
                "Finish",
                { if (s.doneSets == s.totalSets && s.doneSets > 0) finish() else confirmFinish = true },
                modifier = Modifier.align(Alignment.CenterEnd), bold = true,
            )
        }
        // One segment per exercise
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            s.pages.forEachIndexed { i, p ->
                Box(
                    Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(
                        when {
                            p.allDone -> AppTheme.colors.accent
                            i == pager.currentPage -> AppTheme.colors.accent.copy(alpha = 0.45f)
                            else -> AppTheme.colors.border
                        }
                    )
                )
            }
        }

        if (s.pages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No exercises left in this workout.", style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel)
            }
        } else {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f), key = { s.pages.getOrNull(it)?.blocks?.first()?.order ?: it }) { index ->
                val page = s.pages.getOrNull(index) ?: return@HorizontalPager
                val pick = selected[index]?.let { id -> page.ordered.firstOrNull { it.second.set.id == id } }
                val target = pick ?: page.active
                PageContent(
                    page = page,
                    target = target,
                    units = units,
                    nextName = s.pages.getOrNull(index + 1)?.blocks?.joinToString(" + ") { it.name },
                    onSelect = { row -> selected[index] = row.set.id },
                    onToggle = { block, row -> log(page, block, row, toggle = true) },
                    onLongPress = { block, row -> rowMenu = Triple(page, block, row) },
                    onEdit = { block, row, field -> editing = Editing(page, block, row, field) },
                    onAdjustWeight = { row, d -> vm.adjustWeight(row, d, units) },
                    onAdjustReps = { row, d -> vm.adjustReps(row, d) },
                    onLog = { block, row -> log(page, block, row) },
                    onNext = { scope.launch { pager.animateScrollToPage((index + 1).coerceAtMost(s.pages.lastIndex)) } },
                    onFinish = { finish() },
                    onAddSet = vm::addSet,
                    onAddDrop = { vm.addDrop(it) },
                    onSkip = vm::skip,
                    onHowTo = onHowTo,
                    onPlates = { plates = it },
                    onDiscard = { confirmDiscard = true },
                    isLastPage = index == s.pages.lastIndex,
                )
            }
        }
        RestBar()
    }

    editing?.let { e ->
        if (e.field == Field.WEIGHT) {
            NumberPadSheet(
                title = "${e.block.name} · ${setLabel(e.row)}",
                initialValue = e.row.weightKg?.let { Units.kgToDisplay(it, units) },
                unit = units.weightUnit,
                step = if (units == UnitSystem.METRIC) 2.5 else 5.0,
                allowZero = true,
                maxValue = 2000.0,
                confirmText = "Set weight",
                onDismiss = { editing = null },
                onConfirm = { v -> vm.setWeight(e.row.set, Units.displayToKg(v, units)); editing = null },
            )
        } else {
            NumberPadSheet(
                title = "${e.block.name} · ${setLabel(e.row)}",
                initialValue = e.row.reps?.toDouble(),
                unit = "reps",
                allowDecimal = false,
                step = 1.0,
                maxValue = 200.0,
                confirmText = if (e.logAfter) "Log set" else "Set reps",
                onDismiss = { editing = null },
                onConfirm = { v ->
                    if (e.logAfter) { vm.log(context, e.page, e.block, e.row, repsOverride = v.toInt()); haptic() } else vm.setReps(e.row.set, v.toInt())
                    editing = null
                },
            )
        }
    }
    rowMenu?.let { (_, block, row) ->
        ActionSheet(
            title = "${block.name} · ${setLabel(row)}",
            actions = listOfNotNull(
                if (!row.isDrop && !row.isWarmup) SheetAction("Add drop set below") { vm.addDrop(block, row) } else null,
                SheetAction("Add set") { vm.addSet(block) },
                SheetAction(if (row.isDrop) "Delete drop set" else "Delete set", destructive = true) { vm.removeSet(row) },
            ),
            onDismiss = { rowMenu = null },
        )
    }
    plates?.let { row ->
        PlateSheet(
            weightKg = row.weightKg ?: 0.0,
            barKg = s.profile?.barWeightKg ?: 20.0,
            units = units,
            onBarChange = vm::setBarWeight,
            onDismiss = { plates = null },
        )
    }
    if (confirmFinish) {
        val none = s.doneSets == 0
        ConfirmDialog(
            title = if (none) "No sets logged" else "Finish workout?",
            message = if (none) "You haven’t logged any sets yet. Discard this workout instead?"
            else "${s.totalSets - s.doneSets} set(s) aren’t logged and won’t be saved.",
            confirmText = if (none) "Discard" else "Finish",
            destructive = none,
            onDismiss = { confirmFinish = false },
            onConfirm = { confirmFinish = false; if (none) discard() else finish() },
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

private fun setLabel(row: SetRow) = when {
    row.isDrop -> "drop set"
    row.isWarmup -> "warm-up set ${row.set.setNumber}"
    else -> "set ${row.set.setNumber}"
}

@Composable
private fun PageContent(
    page: WorkoutPage,
    target: Pair<ExerciseBlock, SetRow>?,
    units: UnitSystem,
    nextName: String?,
    onSelect: (SetRow) -> Unit,
    onToggle: (ExerciseBlock, SetRow) -> Unit,
    onLongPress: (ExerciseBlock, SetRow) -> Unit,
    onEdit: (ExerciseBlock, SetRow, Field) -> Unit,
    onAdjustWeight: (SetRow, Double) -> Unit,
    onAdjustReps: (SetRow, Int) -> Unit,
    onLog: (ExerciseBlock, SetRow) -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onAddSet: (ExerciseBlock) -> Unit,
    onAddDrop: (ExerciseBlock) -> Unit,
    onSkip: (ExerciseBlock) -> Unit,
    onHowTo: (Long) -> Unit,
    onPlates: (SetRow) -> Unit,
    onDiscard: () -> Unit,
    isLastPage: Boolean,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (page.isSuperset) {
            Text("SUPERSET · ALTERNATE SETS, REST AFTER EACH ROUND", style = AppTheme.type.sectionHeader, color = AppTheme.colors.secondaryLabel)
        }
        page.blocks.forEach { block ->
            ExerciseHeader(block, units, onHowTo, onSkip)
            SetTable(block, target?.second, units, onSelect, onToggle, onLongPress, onEdit, onAddSet, onAddDrop)
        }

        if (target != null) {
            val (block, row) = target
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StepperCard(
                    label = "Weight (${units.weightUnit})",
                    value = row.weightKg?.let { Units.formatWeight(it, units, withUnit = false) } ?: "—",
                    modifier = Modifier.weight(1f),
                    onMinus = { onAdjustWeight(row, -(if (units == UnitSystem.METRIC) 2.5 else 5.0)) },
                    onPlus = { onAdjustWeight(row, if (units == UnitSystem.METRIC) 2.5 else 5.0) },
                    onTapValue = { onEdit(block, row, Field.WEIGHT) },
                    extra = if (block.isBarbell) ({ TextAction("Plates", { onPlates(row) }, color = AppTheme.colors.secondaryLabel) }) else null,
                )
                StepperCard(
                    label = "Reps",
                    value = row.reps?.toString() ?: "—",
                    modifier = Modifier.weight(1f),
                    onMinus = { onAdjustReps(row, -1) },
                    onPlus = { onAdjustReps(row, 1) },
                    onTapValue = { onEdit(block, row, Field.REPS) },
                )
            }
            PrimaryButton(
                when {
                    row.set.completed -> "Update ${setLabel(row)}"
                    page.isSuperset -> "Log ${block.name}, ${setLabel(row)}"
                    else -> "Log ${setLabel(row)}"
                },
                onClick = { onLog(block, row) },
            )
        } else {
            PrimaryButton(
                if (isLastPage) "All sets done · Finish workout" else "Next: ${nextName ?: "exercise"}",
                onClick = if (isLastPage) onFinish else onNext,
            )
        }
        if (nextName != null) {
            Text("Swipe for next: $nextName", style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "Discard workout",
                style = AppTheme.type.footnote, color = AppTheme.colors.tertiaryLabel,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onDiscard).padding(12.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ExerciseHeader(block: ExerciseBlock, units: UnitSystem, onHowTo: (Long) -> Unit, onSkip: (ExerciseBlock) -> Unit) {
    val plan = block.plan
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(block.name, style = AppTheme.type.title1, color = AppTheme.colors.label)
            if (plan != null) {
                Text(
                    listOfNotNull(
                        "${plan.sets} × ${plan.repsText()} reps",
                        block.exercise?.equipment?.takeIf { it.isNotBlank() },
                        "${Dates.rest(plan.restSeconds)} rest",
                    ).joinToString(" · "),
                    style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel,
                )
                if (plan.note.isNotBlank()) {
                    Text(plan.note, style = AppTheme.type.footnote.copy(fontStyle = FontStyle.Italic), color = AppTheme.colors.secondaryLabel)
                }
            }
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (plan?.isWarmup == true) Badge("Warm-up")
                if (plan?.isOptional == true) Badge("Optional")
                if (block.rows.any { it.isPr }) Badge("PR", AppTheme.colors.accent)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (plan?.isOptional == true && !block.anyDone) {
                TextAction("Skip", { onSkip(block) }, color = AppTheme.colors.secondaryLabel)
            }
            block.exercise?.let { ex ->
                Row(
                    Modifier
                        .heightIn(min = 44.dp)
                        .cardSurface(CircleShape)
                        .clickable(role = Role.Button) { onHowTo(ex.id) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Info, null, tint = AppTheme.colors.label, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("How to", style = AppTheme.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.label)
                }
            }
        }
    }
}

private val COL_SET = 52.dp
private val COL_KG = 64.dp
private val COL_REPS = 52.dp
private val COL_TICK = 48.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetTable(
    block: ExerciseBlock,
    target: SetRow?,
    units: UnitSystem,
    onSelect: (SetRow) -> Unit,
    onToggle: (ExerciseBlock, SetRow) -> Unit,
    onLongPress: (ExerciseBlock, SetRow) -> Unit,
    onEdit: (ExerciseBlock, SetRow, Field) -> Unit,
    onAddSet: (ExerciseBlock) -> Unit,
    onAddDrop: (ExerciseBlock) -> Unit,
) {
    val c = AppTheme.colors
    Column(Modifier.fillMaxWidth().cardSurface(RoundedCornerShape(20.dp))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Head("SET", Modifier.width(COL_SET)); Head("LAST TIME", Modifier.weight(1f))
            Head(units.weightUnit.uppercase(), Modifier.width(COL_KG)); Head("REPS", Modifier.width(COL_REPS))
            Spacer(Modifier.width(COL_TICK))
        }
        val mains = block.rows.filter { !it.isDrop }
        block.rows.forEach { row ->
            val isTarget = target?.set?.id == row.set.id
            val s = row.set
            HorizontalDivider(thickness = 1.dp, color = c.separator)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (isTarget && !s.completed) c.cardElevated else Color.Transparent)
                    .combinedClickable(onClick = { onSelect(row) }, onLongClick = { onLongPress(block, row) })
                    .padding(horizontal = 14.dp, vertical = if (row.isDrop) 6.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.width(COL_SET)) {
                    Text(
                        when {
                            row.isDrop -> "DROP"
                            row.isWarmup -> "W${s.setNumber}"
                            else -> "${mains.filter { !it.isWarmup }.indexOf(row) + 1}"
                        },
                        style = if (row.isDrop) AppTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold) else AppTheme.type.headline.copy(fontWeight = FontWeight.Bold),
                        color = when { row.isDrop -> c.warning; row.isWarmup -> c.secondaryLabel; isTarget && !s.completed -> c.accent; else -> c.label },
                    )
                    if (row.isPr) Badge("PR", c.accent)
                }
                Text(
                    row.previous?.let { "${Units.formatWeight(it.weightKg ?: 0.0, units, withUnit = false)} × ${it.reps ?: 0}" }
                        ?: if (row.isDrop) "−20%" else "—",
                    style = AppTheme.type.footnote, color = c.secondaryLabel, modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Cell(
                    s.weightKg?.let { Units.formatWeight(it, units, withUnit = false) } ?: row.placeholderWeightKg?.let { Units.formatWeight(it, units, withUnit = false) } ?: "—",
                    typed = s.weightKg != null, label = "Weight", modifier = Modifier.width(COL_KG),
                ) { onEdit(block, row, Field.WEIGHT) }
                Cell(
                    s.reps?.toString() ?: row.placeholderReps?.toString() ?: "—",
                    typed = s.reps != null, label = "Reps", modifier = Modifier.width(COL_REPS),
                ) { onEdit(block, row, Field.REPS) }
                Box(Modifier.width(COL_TICK), contentAlignment = Alignment.CenterEnd) {
                    Tick(s.completed, dashed = row.isDrop) { onToggle(block, row) }
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = c.separator)
        Row(Modifier.fillMaxWidth().height(48.dp)) {
            Box(Modifier.weight(1f).fillMaxSize().clickable(role = Role.Button) { onAddSet(block) }, contentAlignment = Alignment.Center) {
                Text("+ Add set", style = AppTheme.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = c.chipText)
            }
            Box(Modifier.width(1.dp).fillMaxSize().background(c.separator))
            Box(Modifier.weight(1f).fillMaxSize().clickable(role = Role.Button) { onAddDrop(block) }, contentAlignment = Alignment.Center) {
                Text("+ Drop set", style = AppTheme.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = c.chipText)
            }
        }
    }
}

@Composable
private fun Head(text: String, modifier: Modifier) =
    Text(text, style = AppTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp), color = AppTheme.colors.secondaryLabel, modifier = modifier)

@Composable
private fun Cell(text: String, typed: Boolean, label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$label $text" },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text,
            style = AppTheme.type.number.copy(fontSize = 18.sp),
            color = if (typed) AppTheme.colors.label else AppTheme.colors.secondaryLabel,
        )
    }
}

@Composable
private fun Tick(done: Boolean, dashed: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Checkbox, onClick = onClick), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(36.dp)
                .clip(shape)
                .background(if (done) (if (dashed) c.warning else c.accent) else Color.Transparent)
                .then(if (!done) Modifier.border(2.dp, if (dashed) c.warning.copy(alpha = 0.6f) else c.outline, shape) else Modifier)
                .semantics { contentDescription = if (done) "Done" else "Not done" },
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Rounded.Check, null, tint = c.onAccent, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun StepperCard(
    label: String,
    value: String,
    modifier: Modifier,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onTapValue: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    val view = LocalView.current
    Column(
        modifier.cardSurface(RoundedCornerShape(20.dp)).padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BigKey("−", "Decrease $label") { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); onMinus() }
            Box(
                Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onTapValue).padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(value, style = AppTheme.type.number.copy(fontSize = 30.sp), color = AppTheme.colors.label, maxLines = 1)
            }
            BigKey("+", "Increase $label") { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); onPlus() }
        }
        extra?.invoke()
    }
}

@Composable
private fun BigKey(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(AppTheme.colors.fill)
            .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Text(symbol, style = AppTheme.type.title2.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.label) }
}

/** Plates per side for the current weight; the bar weight can be changed and is remembered. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PlateSheet(weightKg: Double, barKg: Double, units: UnitSystem, onBarChange: (Double) -> Unit, onDismiss: () -> Unit) {
    // A 20 kg bar shows as 45 lb for people using pounds.
    val barDisplayDefault = if (units == UnitSystem.IMPERIAL && barKg == 20.0) 45.0 else Units.formatNumber(Units.kgToDisplay(barKg, units)).toDouble()
    var bar by remember { mutableStateOf(barDisplayDefault) }
    val total = Units.kgToDisplay(weightKg, units)
    val result = PlateCalculator.compute(total, bar, units)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppTheme.colors.card,
        dragHandle = null,
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SheetHeader("Plates", onCancel = null, doneText = "Done", onDone = onDismiss)
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(Units.formatNumber(total), style = AppTheme.type.bigNumber, color = AppTheme.colors.accent)
                    Text(" ${units.weightUnit} total", style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text("EACH SIDE", style = AppTheme.type.sectionHeader, color = AppTheme.colors.secondaryLabel)
                if (result.perSide.isEmpty()) {
                    Text("Just the bar.", style = AppTheme.type.body, color = AppTheme.colors.label)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        result.perSide.forEach { (plate, n) ->
                            repeat(n) {
                                Box(
                                    Modifier
                                        .size(width = 54.dp, height = (40 + plate.coerceAtMost(45.0) * 1.6).dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppTheme.colors.cardElevated)
                                        .border(1.dp, AppTheme.colors.border, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center,
                                ) { Text(Units.formatNumber(plate, 2), style = AppTheme.type.number.copy(fontSize = 16.sp), color = AppTheme.colors.label) }
                            }
                        }
                    }
                }
                if (result.leftover > 0.01) {
                    Text(
                        "Closest you can load: ${Units.formatNumber(result.achieved, 2)} ${units.weightUnit}",
                        style = AppTheme.type.footnote, color = AppTheme.colors.warning,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bar", style = AppTheme.type.body, color = AppTheme.colors.label, modifier = Modifier.weight(1f))
                    val step = if (units == UnitSystem.METRIC) 2.5 else 5.0
                    Stepper(
                        value = (bar / step).toInt(),
                        onValueChange = { v -> bar = v * step; onBarChange(Units.displayToKg(bar, units)) },
                        range = 0..(if (units == UnitSystem.METRIC) 16 else 14),
                        format = { "${Units.formatNumber(it * step)} ${units.weightUnit}" },
                    )
                }
            }
        }
    }
}

/** Rest countdown at the bottom, with +15 s and Skip. Keeps running with the screen off (see RestTimerService). */
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
        Column(Modifier.fillMaxWidth().background(AppTheme.colors.bottomBar)) {
            HorizontalDivider(thickness = 1.dp, color = AppTheme.colors.separator)
            Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Rest", style = AppTheme.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.label)
                        Text(r.label, style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(Dates.duration(remaining + 999), style = AppTheme.type.number, color = AppTheme.colors.accent, modifier = Modifier.padding(horizontal = 12.dp))
                    SmallOutlined("+15 s") { RestTimer.add(context, 15) }
                    Spacer(Modifier.width(8.dp))
                    SmallOutlined("Skip") { RestTimer.stop(context) }
                }
                LinearProgressIndicator(
                    progress = { (remaining / (r.totalSeconds * 1000f)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = AppTheme.colors.accent,
                    trackColor = AppTheme.colors.border,
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                    gapSize = 0.dp,
                )
            }
        }
    }
}

@Composable
private fun SmallOutlined(text: String, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = AppTheme.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.label) }
}
