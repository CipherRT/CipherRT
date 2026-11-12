package com.gymplanner.app.feature.plan

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymplanner.app.AppContainer
import com.gymplanner.app.core.data.Exercise
import com.gymplanner.app.core.data.ExerciseGuide
import com.gymplanner.app.core.model.MuscleGroups
import com.gymplanner.app.core.model.Muscles
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.model.Units
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.BodyMap
import com.gymplanner.app.core.ui.Card
import com.gymplanner.app.core.ui.FieldRow
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.NavBar
import com.gymplanner.app.core.ui.Pill
import com.gymplanner.app.core.ui.ScreenPadding
import com.gymplanner.app.core.ui.TextAction
import com.gymplanner.app.core.ui.TintedButton
import com.gymplanner.app.core.ui.appViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DemoUiState(
    val exercise: Exercise? = null,
    val guide: ExerciseGuide? = null,
    val best: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDemoViewModel(private val c: AppContainer, private val exerciseId: Long) : ViewModel() {
    private val exercise = MutableStateFlow<Exercise?>(null)

    init { viewModelScope.launch { exercise.value = c.exercises.get(exerciseId) } }

    val state: StateFlow<DemoUiState> = c.profiles.currentProfile.filterNotNull().flatMapLatest { p ->
        val units = UnitSystem.from(p.unitSystem)
        combine(exercise, c.exercises.guide(exerciseId), c.workouts.exerciseHistory(p.id, exerciseId)) { ex, g, history ->
            val best = history.maxByOrNull { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
            DemoUiState(ex, g, best?.let { "${Units.formatWeight(it.weightKg ?: 0.0, units)} × ${it.reps ?: 0}" })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DemoUiState())

    fun save(guide: ExerciseGuide) = viewModelScope.launch { c.exercises.saveGuide(guide) }
}

/** "How to": body map, steps, common mistakes and a link to demo videos. */
@Composable
fun ExerciseDemoScreen(exerciseId: Long, onBack: () -> Unit) {
    val vm = appViewModel(key = "demo$exerciseId") { ExerciseDemoViewModel(it, exerciseId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    val ex = s.exercise
    val guide = s.guide ?: ExerciseGuide(exerciseId)

    Column(Modifier.fillMaxSize()) {
        NavBar("How to", onBack = onBack) {
            if (ex?.isCustom == true && !editing) TextAction("Edit", { editing = true })
        }
        if (ex == null) return@Column
        if (editing) {
            GuideEditor(ex, guide, onCancel = { editing = false }, onSave = { vm.save(it); editing = false })
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text(ex.name, style = AppTheme.type.title1, color = AppTheme.colors.label)
                    Text(
                        listOf(MuscleGroups.label(ex.muscleGroup), ex.equipment).filter { it.isNotBlank() }.joinToString(" · "),
                        style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel,
                    )
                }
            }
            item {
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        BodyMap(guide.primaryMuscles.toSet(), guide.secondaryMuscles.toSet(), height = 170.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("TARGETS", style = AppTheme.type.sectionHeader, color = AppTheme.colors.secondaryLabel)
                            if (guide.primaryMuscles.isEmpty() && guide.secondaryMuscles.isEmpty()) {
                                Text("Not set yet", style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel)
                            }
                            guide.primaryMuscles.forEach { Legend(Muscles.label(it), AppTheme.colors.accent, bold = true) }
                            guide.secondaryMuscles.forEach { Legend(Muscles.label(it), AppTheme.colors.accent.copy(alpha = 0.5f)) }
                            s.best?.let { Text("Your best: $it", style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel) }
                        }
                    }
                }
            }
            if (guide.steps.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = ScreenPadding.dp + 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Steps", style = AppTheme.type.headline, color = AppTheme.colors.label)
                        guide.steps.forEachIndexed { i, step ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(AppTheme.colors.chip),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${i + 1}", style = AppTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.accent)
                                }
                                Text(step, style = AppTheme.type.subhead, color = AppTheme.colors.label, modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                    }
                }
            }
            if (guide.mistakes.isNotEmpty()) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ScreenPadding.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(AppTheme.colors.warningCard)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Common mistakes", style = AppTheme.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.warning)
                        guide.mistakes.forEach {
                            Text("•  $it", style = AppTheme.type.footnote, color = Color(0xFFE6DCCF))
                        }
                    }
                }
            }
            if (ex.isCustom && guide.steps.isEmpty() && guide.mistakes.isEmpty()) {
                item {
                    Text(
                        "This is your own exercise. Tap Edit to add the muscles it works, steps and tips.",
                        style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
            item {
                TintedButton(
                    "Watch demo video",
                    icon = Icons.Rounded.OndemandVideo,
                    onClick = {
                        val q = guide.videoQuery.ifBlank { ex.name.lowercase() + " form" }
                        val uri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(q))
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenPadding.dp),
                )
                Text(
                    "Opens a YouTube search (needs internet).",
                    style = AppTheme.type.caption, color = AppTheme.colors.tertiaryLabel,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.navigationBarsPadding().height(24.dp))
            }
        }
    }
}

@Composable
private fun Legend(text: String, color: Color, bold: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = AppTheme.type.subhead.copy(fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal),
            color = if (bold) AppTheme.colors.label else AppTheme.colors.chipText,
        )
    }
}

/** For custom exercises: pick muscles (tap once = main, twice = also, three times = off), steps and mistakes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuideEditor(ex: Exercise, guide: ExerciseGuide, onCancel: () -> Unit, onSave: (ExerciseGuide) -> Unit) {
    var primary by remember { mutableStateOf(guide.primaryMuscles.toSet()) }
    var secondary by remember { mutableStateOf(guide.secondaryMuscles.toSet()) }
    var steps by remember { mutableStateOf(guide.steps.joinToString("\n")) }
    var mistakes by remember { mutableStateOf(guide.mistakes.joinToString("\n")) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(ex.name, style = AppTheme.type.title1, color = AppTheme.colors.label)
                Text("Tap a muscle once for main, twice for also worked.", style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel)
            }
        }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { BodyMap(primary, secondary, height = 160.dp) }
        }
        item {
            FlowRow(
                Modifier.padding(horizontal = ScreenPadding.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Muscles.labels.forEach { (key, label) ->
                    val state = when (key) { in primary -> " (main)"; in secondary -> " (also)"; else -> "" }
                    Pill(label + state, selected = key in primary || key in secondary) {
                        when (key) {
                            in primary -> { primary = primary - key; secondary = secondary + key }
                            in secondary -> secondary = secondary - key
                            else -> primary = primary + key
                        }
                    }
                }
            }
        }
        item {
            GroupedSection(header = "Steps", footer = "One step per line.") {
                FieldRow(steps, { steps = it }, placeholder = "e.g. Sit with your back flat against the pad", singleLine = false)
            }
        }
        item {
            GroupedSection(header = "Common mistakes", footer = "One per line.") {
                FieldRow(mistakes, { mistakes = it }, placeholder = "e.g. Swinging the weight", singleLine = false)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = ScreenPadding.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TintedButton("Cancel", onCancel, modifier = Modifier.weight(1f))
                com.gymplanner.app.core.ui.PrimaryButton(
                    "Save",
                    onClick = {
                        onSave(
                            guide.copy(
                                primaryMuscles = primary.toList(), secondaryMuscles = secondary.toList(),
                                steps = steps.lines().map { it.trim() }.filter { it.isNotEmpty() },
                                mistakes = mistakes.lines().map { it.trim() }.filter { it.isNotEmpty() },
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}
