package com.gymplanner.app.feature.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymplanner.app.AppContainer
import com.gymplanner.app.core.data.Exercise
import com.gymplanner.app.core.data.Plan
import com.gymplanner.app.core.data.PlanDay
import com.gymplanner.app.core.data.PlanExercise
import com.gymplanner.app.core.data.TemplateInfo
import com.gymplanner.app.core.model.PlanFile
import com.gymplanner.app.core.model.PlanJson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DayCard(val day: PlanDay, val exerciseCount: Int, val muscles: List<String>)

data class PlanUiState(
    val loaded: Boolean = false,
    val profileId: Long = 0,
    val plan: Plan? = null,
    val days: List<DayCard> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModel(private val c: AppContainer) : ViewModel() {
    private val profile = c.profiles.currentProfile.filterNotNull()

    val state: StateFlow<PlanUiState> = profile.flatMapLatest { p ->
        c.plans.activePlan(p.id).flatMapLatest { plan ->
            if (plan == null) flowOf(PlanUiState(loaded = true, profileId = p.id))
            else combine(c.plans.days(plan.id), c.plans.planExercises(plan.id), c.exercises.library(p.id)) { days, items, lib ->
                val byId = lib.associateBy { it.id }
                val byDay = items.groupBy { it.planDayId }
                PlanUiState(
                    loaded = true, profileId = p.id, plan = plan,
                    days = days.map { d ->
                        val list = byDay[d.id].orEmpty()
                        DayCard(
                            d, list.size,
                            list.filter { !it.isWarmup }.mapNotNull { byId[it.exerciseId]?.muscleGroup }.distinct(),
                        )
                    },
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlanUiState())

    fun addDay(name: String) = state.value.plan?.let { p -> viewModelScope.launch { c.plans.addDay(p.id, name) } }
    fun reorder(days: List<PlanDay>) = viewModelScope.launch { c.plans.reorderDays(days) }
    fun createPlan(name: String) = viewModelScope.launch { c.plans.createEmpty(state.value.profileId, name, makeActive = true) }
    suspend fun exportActive(): PlanFile? = state.value.plan?.let { c.plans.exportFile(it.id) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlanListViewModel(private val c: AppContainer) : ViewModel() {
    private val profile = c.profiles.currentProfile.filterNotNull()
    val plans: StateFlow<List<Plan>> = profile.flatMapLatest { c.plans.plans(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val templates: List<TemplateInfo> = c.plans.templates()
    private var profileId = 0L

    init { viewModelScope.launch { profile.collect { profileId = it.id } } }

    fun setActive(plan: Plan) = viewModelScope.launch { c.plans.setActive(plan) }
    fun rename(plan: Plan, name: String) = viewModelScope.launch { c.plans.rename(plan, name) }
    fun duplicate(plan: Plan) = viewModelScope.launch { c.plans.duplicate(plan) }
    fun delete(plan: Plan) = viewModelScope.launch { c.plans.delete(plan) }
    fun createEmpty(name: String) = viewModelScope.launch { c.plans.createEmpty(profileId, name, makeActive = false) }
    fun addTemplate(t: TemplateInfo) = viewModelScope.launch { c.plans.importPlan(profileId, t.plan, makeActive = false) }
    suspend fun export(plan: Plan): PlanFile = c.plans.exportFile(plan.id)
}

data class DayItem(val pe: PlanExercise, val exercise: Exercise?)

@OptIn(ExperimentalCoroutinesApi::class)
class DayEditorViewModel(private val c: AppContainer, private val dayId: Long) : ViewModel() {
    val day: StateFlow<PlanDay?> = c.plans.day(dayId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val items: StateFlow<List<DayItem>> = c.profiles.currentProfile.filterNotNull().flatMapLatest { p ->
        combine(c.plans.dayExercises(dayId), c.exercises.library(p.id)) { list, lib ->
            val byId = lib.associateBy { it.id }
            list.map { DayItem(it, byId[it.exerciseId]) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun update(day: PlanDay) = viewModelScope.launch { c.plans.updateDay(day) }
    fun reorder(list: List<DayItem>) = viewModelScope.launch { c.plans.reorderExercises(list.map { it.pe }) }
    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        day.value?.let { c.plans.deleteDay(it) }
        onDone()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PickerViewModel(private val c: AppContainer, private val dayId: Long) : ViewModel() {
    private val profile = c.profiles.currentProfile.filterNotNull()
    val query = MutableStateFlow("")
    val group = MutableStateFlow<String?>(null)
    private var profileId = 0L

    init { viewModelScope.launch { profile.collect { profileId = it.id } } }

    val results: StateFlow<List<Exercise>> = combine(profile.flatMapLatest { c.exercises.library(it.id) }, query, group) { lib, q, g ->
        lib.filter { (g == null || it.muscleGroup == g) && (q.isBlank() || it.name.contains(q.trim(), ignoreCase = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(exercise: Exercise, onAdded: (Long) -> Unit) = viewModelScope.launch {
        onAdded(c.plans.addExercise(dayId, exercise.id, c.prefs.defaultRestSeconds.value))
    }

    fun createAndAdd(name: String, group: String, equipment: String, onAdded: (Long) -> Unit) = viewModelScope.launch {
        val id = c.exercises.addCustom(profileId, name, group, equipment)
        onAdded(c.plans.addExercise(dayId, id, c.prefs.defaultRestSeconds.value))
    }
}

class PlanExerciseViewModel(private val c: AppContainer, id: Long) : ViewModel() {
    val item: StateFlow<PlanExercise?> = c.plans.planExercise(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise: StateFlow<Exercise?> = _exercise

    init {
        viewModelScope.launch {
            item.filterNotNull().collect { if (_exercise.value == null) _exercise.value = c.exercises.get(it.exerciseId) }
        }
    }

    fun save(pe: PlanExercise) = viewModelScope.launch { c.plans.updateExercise(pe) }
    fun remove(onDone: () -> Unit) = viewModelScope.launch {
        item.value?.let { c.plans.removeExercise(it) }
        onDone()
    }
}

sealed interface ImportState {
    data object Loading : ImportState
    data class Ready(val plan: PlanJson) : ImportState
    data class Failed(val message: String) : ImportState
}

class ImportViewModel(private val c: AppContainer) : ViewModel() {
    val state = MutableStateFlow<ImportState>(ImportState.Loading)

    fun load(text: () -> String) = viewModelScope.launch {
        state.value = runCatching { ImportState.Ready(com.gymplanner.app.core.data.PlanRepository.parsePlanFile(text()).plan) }
            .getOrElse { ImportState.Failed(it.message ?: "This file couldn’t be read as a gym plan.") }
    }

    fun import(plan: PlanJson, makeActive: Boolean, onDone: () -> Unit) = viewModelScope.launch {
        val profileId = c.prefs.currentProfileId.value ?: return@launch
        c.plans.importPlan(profileId, plan, makeActive)
        onDone()
    }
}

