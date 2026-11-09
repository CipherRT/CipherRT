package com.gymplanner.app.feature.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Flag
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
import com.gymplanner.app.core.data.ThemeMode
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
    val theme by vm.c.prefs.theme.collectAsStateWithLifecycle()
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
                GroupedSection(header = "Workout", footer = "Used for new exercises you add. Each exercise can have its own rest time.") {
                    ListRow("Default rest", icon = Icons.Rounded.Timer, iconColor = AppTheme.colors.indigo, trailing = {
                        Stepper(rest, vm.c.prefs::setDefaultRest, 15..600, step = 15, format = Dates::rest)
                    })
                }
            }
            item {
                GroupedSection(header = "Appearance") {
                    ListRow("Theme", icon = Icons.Rounded.DarkMode, iconColor = AppTheme.colors.purple, trailing = {
                        SegmentedControl(ThemeMode.entries, theme, vm.c.prefs::setTheme, label = { it.label }, modifier = Modifier.width(200.dp))
                    })
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
