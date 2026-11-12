package com.gymplanner.app.feature.today

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymplanner.app.AppContainer
import com.gymplanner.app.core.data.WorkoutSession
import com.gymplanner.app.core.data.SetType
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.model.Units
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.Badge
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.PrimaryButton
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.StatTile
import com.gymplanner.app.core.ui.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class SummaryLine(val name: String, val sets: Int, val best: String, val isPr: Boolean)
data class Summary(
    val session: WorkoutSession,
    val units: UnitSystem,
    val sets: Int,
    val volumeKg: Double,
    val prs: Int,
    val lines: List<SummaryLine>,
)

class SummaryViewModel(private val c: AppContainer, private val sessionId: Long) : ViewModel() {
    val summary = MutableStateFlow<Summary?>(null)

    init {
        viewModelScope.launch {
            val session = c.workouts.getSession(sessionId) ?: return@launch
            val units = UnitSystem.from(c.profiles.get(session.profileId)?.unitSystem)
            // Warm-up sets don't count toward set totals or volume.
            val sets = c.workouts.getSets(sessionId).filter { it.completed && it.setType != SetType.WARMUP }
            val names = c.exercises.getAll(sets.map { it.exerciseId }.distinct()).associate { it.id to it.name }
            val lines = sets.groupBy { it.exerciseOrder }.toSortedMap().values.map { list ->
                val exId = list.first().exerciseId
                val best = list.maxBy { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
                val prior = c.workouts.memory(session.profileId, exId, sessionId).bestScore
                val score = (best.weightKg ?: 0.0) * (best.reps ?: 0)
                SummaryLine(
                    name = names[exId] ?: "Exercise",
                    sets = list.size,
                    best = "${Units.formatWeight(best.weightKg ?: 0.0, units)} × ${best.reps ?: 0}",
                    isPr = prior != null && prior > 0 && score > prior,
                )
            }
            summary.value = Summary(
                session = session, units = units, sets = sets.size,
                volumeKg = sets.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) },
                prs = lines.count { it.isPr }, lines = lines,
            )
        }
    }
}

@Composable
fun SummaryScreen(sessionId: Long, onDone: () -> Unit) {
    val vm = appViewModel(key = "summary$sessionId") { SummaryViewModel(it, sessionId) }
    val summary by vm.summary.collectAsStateWithLifecycle()
    BackHandler(onBack = onDone)
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(if (appeared) 1f else 0.4f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "pop")

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = AppTheme.colors.green, modifier = Modifier.size(84.dp).scale(scale))
                    Spacer(Modifier.height(12.dp))
                    Text("Workout Complete", style = AppTheme.type.title1, color = AppTheme.colors.label)
                    summary?.let {
                        Text(
                            "${it.session.dayName} · ${Dates.relativeMillis(it.session.startedAt)}",
                            style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            summary?.let { s ->
                item {
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Time", Dates.durationWords((s.session.finishedAt ?: s.session.startedAt) - s.session.startedAt), Modifier.weight(1f))
                        StatTile("Sets", "${s.sets}", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Volume", Units.formatVolume(s.volumeKg, s.units), Modifier.weight(1f))
                        StatTile("Records", "${s.prs}", Modifier.weight(1f), valueColor = if (s.prs > 0) AppTheme.colors.orange else AppTheme.colors.label)
                    }
                }
                if (s.lines.isNotEmpty()) {
                    item {
                        GroupedSection(header = "Best sets") {
                            s.lines.forEachIndexed { i, l ->
                                ListRow(
                                    l.name, subtitle = "${l.sets} set${if (l.sets == 1) "" else "s"}", value = l.best,
                                    trailing = if (l.isPr) { { Badge("PR", AppTheme.colors.orange) } } else null,
                                )
                                if (i < s.lines.lastIndex) RowDivider()
                            }
                        }
                    }
                }
            }
        }
        PrimaryButton("Done", onClick = onDone, modifier = Modifier.padding(16.dp))
    }
}
