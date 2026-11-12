package com.gymplanner.app.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymplanner.app.AppContainer
import com.gymplanner.app.core.data.Exercise
import com.gymplanner.app.core.data.ExerciseSummary
import com.gymplanner.app.core.data.SetWithSession
import com.gymplanner.app.core.data.WorkoutSession
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.ui.ChartPoint
import com.gymplanner.app.core.ui.Dates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeekSummary(val sessions: Int = 0, val sets: Int = 0, val volumeKg: Double = 0.0)

data class ProgressUiState(
    val loaded: Boolean = false,
    val units: UnitSystem = UnitSystem.METRIC,
    val week: WeekSummary = WeekSummary(),
    val exercises: List<ExerciseSummary> = emptyList(),
    val recent: List<WorkoutSession> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(c: AppContainer) : ViewModel() {
    private val profile = c.profiles.currentProfile.filterNotNull()

    val state: StateFlow<ProgressUiState> = profile.flatMapLatest { p ->
        combine(
            c.workouts.completedSetsThisWeek(p.id),
            c.workouts.exerciseSummaries(p.id),
            c.workouts.finishedSessions(p.id),
        ) { weekSets, summaries, sessions ->
            ProgressUiState(
                loaded = true,
                units = UnitSystem.from(p.unitSystem),
                week = WeekSummary(
                    sessions = weekSets.map { it.sessionId }.distinct().size,
                    sets = weekSets.size,
                    volumeKg = weekSets.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) },
                ),
                exercises = summaries,
                recent = sessions.take(10),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressUiState())
}

data class SessionHistory(val sessionId: Long, val startedAt: Long, val sets: List<SetWithSession>) {
    val bestWeight: Double get() = sets.filter { it.setType != com.gymplanner.app.core.data.SetType.WARMUP }.maxOfOrNull { it.weightKg ?: 0.0 } ?: 0.0
}

data class HistoryUiState(
    val exercise: Exercise? = null,
    val units: UnitSystem = UnitSystem.METRIC,
    val sessions: List<SessionHistory> = emptyList(),
    val chart: List<ChartPoint> = emptyList(),
    val bestWeight: Double? = null,
    val bestSet: SetWithSession? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseHistoryViewModel(c: AppContainer, exerciseId: Long) : ViewModel() {
    private val exercise = MutableStateFlow<Exercise?>(null)

    init { viewModelScope.launch { exercise.value = c.exercises.get(exerciseId) } }

    val state: StateFlow<HistoryUiState> = c.profiles.currentProfile.filterNotNull().flatMapLatest { p ->
        combine(c.workouts.exerciseHistory(p.id, exerciseId), exercise) { rows, ex ->
            val sessions = rows.groupBy { it.sessionId }.map { (id, list) -> SessionHistory(id, list.first().startedAt, list.sortedBy { it.setNumber }) }
                .sortedByDescending { it.startedAt }
            // One point per day: the heaviest set that day.
            val chart = sessions.groupBy { Dates.toLocalDate(it.startedAt).toEpochDay() }
                .map { (day, list) -> ChartPoint(day, list.maxOf { it.bestWeight }) }
                .sortedBy { it.x }
            HistoryUiState(
                exercise = ex,
                units = UnitSystem.from(p.unitSystem),
                sessions = sessions,
                chart = chart,
                bestWeight = rows.filter { it.setType != com.gymplanner.app.core.data.SetType.WARMUP }.maxOfOrNull { it.weightKg ?: 0.0 },
                bestSet = rows.filter { it.setType != com.gymplanner.app.core.data.SetType.WARMUP }.maxByOrNull { (it.weightKg ?: 0.0) * (it.reps ?: 0) },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())
}
