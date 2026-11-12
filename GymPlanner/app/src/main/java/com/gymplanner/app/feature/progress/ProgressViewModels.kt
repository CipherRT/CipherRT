package com.gymplanner.app.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymplanner.app.AppContainer
import com.gymplanner.app.core.data.BodyWeightEntry
import com.gymplanner.app.core.data.CardioSession
import com.gymplanner.app.core.data.Exercise
import com.gymplanner.app.core.data.ExerciseSummary
import com.gymplanner.app.core.data.Profile
import com.gymplanner.app.core.data.SetType
import com.gymplanner.app.core.data.SetWithSession
import com.gymplanner.app.core.data.WorkoutSession
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.ui.ChartPoint
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.feature.today.Streak
import com.gymplanner.app.feature.today.StreakLogic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class MonthStats(val gymSessions: Int, val cardioSessions: Int, val weightChangeKg: Double?)

data class ProgressUiState(
    val loaded: Boolean = false,
    val profile: Profile? = null,
    val streak: Streak = Streak(0, 0, 0),
    val month: YearMonth = YearMonth.now(),
    val gymDays: Set<LocalDate> = emptySet(),
    val cardioDays: Set<LocalDate> = emptySet(),
    val stats: MonthStats = MonthStats(0, 0, null),
    val sessions: List<WorkoutSession> = emptyList(),
    val cardio: List<CardioSession> = emptyList(),
    val weights: List<BodyWeightEntry> = emptyList(),
    val exercises: List<ExerciseSummary> = emptyList(),
) {
    val units: UnitSystem get() = UnitSystem.from(profile?.unitSystem)
}

object MonthLogic {
    fun stats(month: YearMonth, sessions: List<WorkoutSession>, cardio: List<CardioSession>, weights: List<BodyWeightEntry>): MonthStats {
        val inMonth = { d: LocalDate -> YearMonth.from(d) == month }
        val w = weights.filter { inMonth(LocalDate.ofEpochDay(it.date)) }.sortedBy { it.date }
        return MonthStats(
            gymSessions = sessions.count { inMonth(Dates.toLocalDate(it.startedAt)) },
            cardioSessions = cardio.count { inMonth(LocalDate.ofEpochDay(it.date)) },
            weightChangeKg = if (w.size >= 2) w.last().weightKg - w.first().weightKg else null,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(c: AppContainer) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())

    val state: StateFlow<ProgressUiState> = c.profiles.currentProfile.filterNotNull().flatMapLatest { p ->
        combine(
            c.workouts.finishedSessions(p.id),
            c.track.cardio(p.id),
            c.weights.entries(p.id),
            c.workouts.exerciseSummaries(p.id),
            month,
        ) { sessions, cardio, weights, summaries, m ->
            val gymDays = sessions.map { Dates.toLocalDate(it.startedAt) }
            val cardioDays = cardio.map { LocalDate.ofEpochDay(it.date) }
            val counted = gymDays + if (p.cardioCountsTowardTarget) cardioDays else emptyList()
            ProgressUiState(
                loaded = true,
                profile = p,
                streak = StreakLogic.compute(counted, p.weeklyTarget, LocalDate.now()),
                month = m,
                gymDays = gymDays.toSet(),
                cardioDays = cardioDays.toSet(),
                stats = MonthLogic.stats(m, sessions, cardio, weights),
                sessions = sessions,
                cardio = cardio,
                weights = weights,
                exercises = summaries,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressUiState())

    fun shiftMonth(by: Long) { month.value = month.value.plusMonths(by).coerceAtMost(YearMonth.now()) }
}

private fun YearMonth.coerceAtMost(max: YearMonth) = if (isAfter(max)) max else this

data class SessionHistory(val sessionId: Long, val startedAt: Long, val sets: List<SetWithSession>) {
    val bestWeight: Double get() = sets.filter { it.setType != SetType.WARMUP }.maxOfOrNull { it.weightKg ?: 0.0 } ?: 0.0
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
            val sessions = rows.groupBy { it.sessionId }.map { (id, list) -> SessionHistory(id, list.first().startedAt, list) }
                .sortedByDescending { it.startedAt }
            val chart = sessions.groupBy { Dates.toLocalDate(it.startedAt).toEpochDay() }
                .map { (day, list) -> ChartPoint(day, list.maxOf { it.bestWeight }) }
                .sortedBy { it.x }
            val working = rows.filter { it.setType != SetType.WARMUP }
            HistoryUiState(
                exercise = ex,
                units = UnitSystem.from(p.unitSystem),
                sessions = sessions,
                chart = chart,
                bestWeight = working.maxOfOrNull { it.weightKg ?: 0.0 },
                bestSet = working.maxByOrNull { (it.weightKg ?: 0.0) * (it.reps ?: 0) },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())
}
