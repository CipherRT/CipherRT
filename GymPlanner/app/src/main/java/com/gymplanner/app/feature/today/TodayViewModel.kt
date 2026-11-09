package com.gymplanner.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymplanner.app.AppContainer
import com.gymplanner.app.core.data.BodyWeightEntry
import com.gymplanner.app.core.data.Exercise
import com.gymplanner.app.core.data.Plan
import com.gymplanner.app.core.data.PlanDay
import com.gymplanner.app.core.data.PlanExercise
import com.gymplanner.app.core.data.Profile
import com.gymplanner.app.core.data.WorkoutSession
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.ui.Dates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    val loaded: Boolean = false,
    val profile: Profile? = null,
    val plan: Plan? = null,
    val days: List<PlanDay> = emptyList(),
    val day: PlanDay? = null,
    val isSuggested: Boolean = true,
    val exercises: List<Pair<PlanExercise, Exercise?>> = emptyList(),
    val unfinished: WorkoutSession? = null,
    val trainedToday: Boolean = false,
    val latestWeight: BodyWeightEntry? = null,
) {
    val units: UnitSystem get() = UnitSystem.from(profile?.unitSystem)
}

private data class Base(
    val profile: Profile,
    val plan: Plan?,
    val days: List<PlanDay>,
    val last: WorkoutSession?,
    val unfinished: WorkoutSession?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val c: AppContainer) : ViewModel() {
    private val chosenDayId = MutableStateFlow<Long?>(null)
    private val profile = c.profiles.currentProfile.filterNotNull()

    private val base = profile.flatMapLatest { p ->
        val planFlow = c.plans.activePlan(p.id)
        val daysFlow = planFlow.flatMapLatest { plan -> if (plan == null) flowOf(emptyList()) else c.plans.days(plan.id) }
        combine(planFlow, daysFlow, c.workouts.lastFinished(p.id), c.workouts.unfinished(p.id)) { plan, days, last, unfinished ->
            Base(p, plan, days, last, unfinished)
        }
    }

    val state: StateFlow<TodayUiState> = combine(base, chosenDayId) { b, chosen -> b to chosen }
        .flatMapLatest { (b, chosen) ->
            val suggested = TodayLogic.pickDay(b.days, LocalDate.now().dayOfWeek.value, b.last?.planDayId)
            val day = b.days.firstOrNull { it.id == chosen } ?: suggested
            val exercisesFlow = if (day == null) flowOf(emptyList()) else combine(
                c.plans.dayExercises(day.id), c.exercises.library(b.profile.id),
            ) { list, lib ->
                val byId = lib.associateBy { it.id }
                list.map { it to byId[it.exerciseId] }
            }
            combine(exercisesFlow, c.weights.entries(b.profile.id).map { it.firstOrNull() }) { exercises, weight ->
                TodayUiState(
                    loaded = true,
                    profile = b.profile,
                    plan = b.plan,
                    days = b.days,
                    day = day,
                    isSuggested = day?.id == suggested?.id,
                    exercises = exercises,
                    unfinished = b.unfinished,
                    trainedToday = b.last?.finishedAt?.let { Dates.toLocalDate(it) == LocalDate.now() } == true,
                    latestWeight = weight,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    fun chooseDay(id: Long) { chosenDayId.value = id }

    fun start(isLight: Boolean, onStarted: (Long) -> Unit) {
        val s = state.value
        val p = s.profile ?: return
        val day = s.day ?: return
        viewModelScope.launch {
            val id = c.workouts.start(p.id, day, isLight)
            chosenDayId.value = null
            onStarted(id)
        }
    }
}
