package com.gymplanner.app.feature.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.gymplanner.app.core.ui.Accent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymplanner.app.AppContainer
import com.gymplanner.app.BuildConfigCompat
import com.gymplanner.app.core.data.Profile
import com.gymplanner.app.core.model.Goal
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.model.Units
import com.gymplanner.app.core.ui.ActionSheet
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.ConfirmDialog
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.core.ui.FieldRow
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.NavBar
import com.gymplanner.app.core.ui.NumberPadSheet
import com.gymplanner.app.core.ui.ProfileAvatar
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.SegmentedControl
import com.gymplanner.app.core.ui.SheetAction
import com.gymplanner.app.core.ui.Stepper
import com.gymplanner.app.core.ui.appViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class SettingsViewModel(val c: AppContainer) : ViewModel() {
    val profile = c.profiles.currentProfile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val profiles = c.profiles.profiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun update(p: Profile) = viewModelScope.launch { c.profiles.update(p) }
    fun delete(p: Profile) = viewModelScope.launch { c.profiles.delete(p) }

    fun backup(write: (String) -> Unit, onError: (String) -> Unit) = viewModelScope.launch {
        val id = profile.value?.id ?: return@launch
        runCatching { write(c.backup.export(id)) }.onFailure { onError(it.message ?: "Backup failed") }
    }

    fun restore(read: () -> String, onDone: (String) -> Unit) = viewModelScope.launch {
        runCatching { c.backup.restore(read()) }
            .onSuccess { onDone("Backup restored as a new profile.") }
            .onFailure { onDone(it.message ?: "This file couldn’t be restored.") }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onAddProfile: () -> Unit) {
    val vm = appViewModel { SettingsViewModel(it) }
    val profile by vm.profile.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val accent by vm.c.prefs.accent.collectAsStateWithLifecycle()
    val rest by vm.c.prefs.defaultRestSeconds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    var name by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(profile?.id) { name = profile?.name }
    var editHeight by remember { mutableStateOf(false) }
    var pickGoal by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.backup(
            write = { text ->
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
                toast("Backup saved.")
            },
            onError = ::toast,
        )
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.restore(
            read = { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Couldn’t open the file.") },
            onDone = ::toast,
        )
    }

    Column(Modifier.fillMaxSize()) {
        NavBar("Settings", onBack = onBack)
        val p = profile ?: return@Column
        val units = UnitSystem.from(p.unitSystem)
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item {
                GroupedSection(header = "Profile") {
                    FieldRow(name ?: "", { v -> name = v; if (v.isNotBlank()) vm.update(p.copy(name = v.trim())) }, placeholder = "Name", label = "Name")
                    RowDivider()
                    ListRow("Height", icon = Icons.Rounded.Height, iconColor = AppTheme.colors.teal, value = Units.formatHeight(p.heightCm, units), onClick = { editHeight = true })
                    RowDivider(startIndent = 60.dp)
                    ListRow("Goal", icon = Icons.Rounded.Flag, iconColor = AppTheme.colors.green, value = Goal.from(p.goal).label, onClick = { pickGoal = true })
                }
            }
            item {
                GroupedSection(header = "Units", footer = "Everything is stored in kg, so switching never changes your numbers.") {
                    ListRow("Units", icon = Icons.Rounded.Straighten, iconColor = AppTheme.colors.accent, trailing = {
                        SegmentedControl(
                            UnitSystem.entries, units, { vm.update(p.copy(unitSystem = it.name)) },
                            label = { it.weightUnit }, modifier = Modifier.width(120.dp),
                        )
                    })
                }
            }
            item {
                GroupedSection(
                    header = "Workout",
                    footer = "Your streak grows each Monday–Sunday week you hit the target. Default rest is used for new exercises.",
                ) {
                    ListRow("Weekly target", icon = Icons.Rounded.Flag, iconColor = AppTheme.colors.accent, trailing = {
                        Stepper(p.weeklyTarget, { vm.update(p.copy(weeklyTarget = it)) }, 1..7, format = { "$it / week" })
                    })
                    RowDivider(startIndent = 60.dp)
                    com.gymplanner.app.core.ui.SwitchRow(
                        "Cardio counts toward target", p.cardioCountsTowardTarget, { vm.update(p.copy(cardioCountsTowardTarget = it)) },
                        icon = Icons.Rounded.MonitorHeart,
                    )
                    RowDivider(startIndent = 60.dp)
                    ListRow("Default rest", icon = Icons.Rounded.Timer, iconColor = AppTheme.colors.indigo, trailing = {
                        Stepper(rest, vm.c.prefs::setDefaultRest, 15..600, step = 15, format = Dates::rest)
                    })
                }
            }
            item { WeighInSettings(p) { vm.update(it) } }
            item {
                GroupedSection(header = "Accent colour", footer = "Used for buttons, finished sets, your streak and records.") {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Accent.entries.forEach { a ->
                            AccentSwatch(a, selected = a == accent) { vm.c.prefs.setAccent(a) }
                        }
                    }
                }
            }
            item {
                GroupedSection(header = "Profiles", footer = "Each profile has its own plans, weight and workouts. Nobody sees anyone else’s data.") {
                    profiles.forEach { other ->
                        ListRow(
                            other.name,
                            leading = { ProfileAvatar(other.name, size = 30.dp) },
                            onClick = { if (other.id != p.id) vm.c.profiles.switchTo(other.id) },
                            trailing = if (other.id == p.id) { { Icon(Icons.Rounded.Check, "Current", tint = AppTheme.colors.accent) } } else null,
                        )
                        RowDivider(startIndent = 58.dp)
                    }
                    ListRow("Add Profile", icon = Icons.Rounded.PersonAdd, iconColor = AppTheme.colors.accent, titleColor = AppTheme.colors.accent, showChevron = false, onClick = onAddProfile)
                }
            }
            item {
                GroupedSection(
                    header = "Backup",
                    footer = "A backup holds this profile’s plans, weight and workouts. Keep it private — it’s for moving to a new phone. Restoring adds it as a new profile.",
                ) {
                    ListRow("Back Up ${p.name}", icon = Icons.Rounded.CloudUpload, iconColor = AppTheme.colors.accent, onClick = {
                        val safe = p.name.replace(Regex("[^A-Za-z0-9]"), "").ifBlank { "profile" }
                        backupLauncher.launch("gymplanner-backup-$safe-${LocalDate.now()}.json")
                    })
                    RowDivider(startIndent = 60.dp)
                    ListRow("Restore from Backup", icon = Icons.Rounded.CloudDownload, iconColor = AppTheme.colors.green, onClick = {
                        restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
                    })
                }
            }
            item {
                GroupedSection {
                    ListRow("Delete ${p.name}", titleColor = AppTheme.colors.red, showChevron = false, onClick = { confirmDelete = true })
                }
            }
            item {
                androidx.compose.material3.Text(
                    "Gym Planner ${BuildConfigCompat.versionName(context)}\nWorks offline. Your data never leaves this phone.",
                    style = AppTheme.type.footnote,
                    color = AppTheme.colors.secondaryLabel,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp).fillParentMaxWidth(),
                )
                Spacer(Modifier.height(32.dp))
            }
        }

        if (editHeight) {
            if (units == UnitSystem.METRIC) {
                NumberPadSheet(
                    "Height", initialValue = Math.round(p.heightCm).toDouble(), unit = "cm", allowDecimal = false, maxValue = 260.0,
                    onDismiss = { editHeight = false }, onConfirm = { vm.update(p.copy(heightCm = it)); editHeight = false },
                )
            } else {
                NumberPadSheet(
                    "Height (total inches)", initialValue = Math.round(p.heightCm / Units.CM_PER_IN).toDouble(), unit = "in",
                    allowDecimal = false, maxValue = 100.0,
                    onDismiss = { editHeight = false }, onConfirm = { vm.update(p.copy(heightCm = it * Units.CM_PER_IN)); editHeight = false },
                    extraContent = {
                        androidx.compose.material3.Text(
                            "e.g. 5′10″ = 70 in",
                            style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    },
                )
            }
        }
        if (pickGoal) {
            ActionSheet("Goal", Goal.entries.map { g -> SheetAction(g.label) { vm.update(p.copy(goal = g.name)) } }, onDismiss = { pickGoal = false })
        }
        if (confirmDelete) {
            ConfirmDialog(
                title = "Delete ${p.name}?",
                message = "This deletes ${p.name}’s plans, weight entries and workouts from this phone. This can’t be undone.",
                confirmText = "Delete Profile",
                onDismiss = { confirmDelete = false },
                onConfirm = {
                    confirmDelete = false
                    val last = profiles.size <= 1
                    vm.delete(p)
                    if (!last) onBack()
                },
            )
        }
    }
}

/** A round colour swatch; the chosen one gets a white ring. */
@Composable
private fun AccentSwatch(accent: Accent, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(if (selected) Modifier.border(2.dp, AppTheme.colors.label, CircleShape) else Modifier)
            .clickable(role = Role.RadioButton, onClickLabel = accent.label, onClick = onClick)
            .padding(if (selected) 6.dp else 0.dp)
            .clip(CircleShape)
            .background(accent.color)
            .semantics { contentDescription = accent.label + if (selected) ", selected" else "" },
    )
}

/** Morning weigh-in: on/off, time, days, and a way to try it. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun WeighInSettings(p: Profile, onChange: (Profile) -> Unit) {
    val context = LocalContext.current
    var pickTime by remember { mutableStateOf(false) }
    val fullScreenOk = com.gymplanner.app.feature.weighin.WeighInPermission.canFullScreen(context)
    GroupedSection(
        header = "Morning weigh-in",
        footer = "A full-screen prompt at this time, like an alarm. It doesn’t show if you’ve already logged today’s weight.",
    ) {
        com.gymplanner.app.core.ui.SwitchRow(
            "Weigh-in prompt", p.weighInEnabled, { onChange(p.copy(weighInEnabled = it)) },
            icon = Icons.Rounded.Alarm,
        )
        if (p.weighInEnabled) {
            RowDivider(startIndent = 60.dp)
            ListRow("Time", value = com.gymplanner.app.feature.weighin.WeighInTimes.label(p.weighInTime), onClick = { pickTime = true })
            RowDivider()
            Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                java.time.DayOfWeek.entries.forEachIndexed { i, d ->
                    val on = p.weighInDays and (1 shl i) != 0
                    Box(
                        Modifier
                            .weight(1f)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (on) AppTheme.colors.accent else AppTheme.colors.chip)
                            .clickable(role = Role.Checkbox) { onChange(p.copy(weighInDays = p.weighInDays xor (1 shl i))) }
                            .semantics { contentDescription = d.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()) + if (on) ", on" else ", off" },
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        androidx.compose.material3.Text(
                            d.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()),
                            style = AppTheme.type.subhead.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                            color = if (on) AppTheme.colors.onAccent else AppTheme.colors.chipText,
                        )
                    }
                }
            }
            if (!fullScreenOk) {
                RowDivider()
                ListRow(
                    "Allow full-screen alerts", subtitle = "Needed to show over the lock screen",
                    onClick = { com.gymplanner.app.feature.weighin.WeighInPermission.openFullScreenSettings(context) },
                )
            }
            RowDivider()
            ListRow("Try it now", titleColor = AppTheme.colors.accent, showChevron = false, onClick = {
                context.startActivity(
                    android.content.Intent(context, com.gymplanner.app.feature.weighin.WeighInActivity::class.java)
                        .putExtra(com.gymplanner.app.feature.weighin.WeighInScheduler.EXTRA_PROFILE, p.id)
                        .putExtra(com.gymplanner.app.feature.weighin.WeighInScheduler.EXTRA_PREVIEW, true)
                )
            })
        }
    }
    if (pickTime) {
        val state = androidx.compose.material3.rememberTimePickerState(p.weighInTime / 60, p.weighInTime % 60, is24Hour = android.text.format.DateFormat.is24HourFormat(context))
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickTime = false },
            containerColor = AppTheme.colors.cardElevated,
            text = { androidx.compose.material3.TimePicker(state) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { onChange(p.copy(weighInTime = state.hour * 60 + state.minute)); pickTime = false }) {
                    androidx.compose.material3.Text("Done", color = AppTheme.colors.accent)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pickTime = false }) { androidx.compose.material3.Text("Cancel", color = AppTheme.colors.accent) }
            },
        )
    }
}
