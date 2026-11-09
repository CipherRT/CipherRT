package com.gymplanner.app.feature.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymplanner.app.AppContainer
import com.gymplanner.app.core.data.Exercise
import com.gymplanner.app.core.data.ExerciseMemory
import com.gymplanner.app.core.data.PlanExercise
import com.gymplanner.app.core.data.SetLog
import com.gymplanner.app.core.data.WorkoutSession
import com.gymplanner.app.core.model.UnitSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SetRow(val set: SetLog, val previous: SetLog?, val isPr: Boolean)

data class ExerciseBlock(
    val order: Int,
    val exercise: Exercise?,
    val plan: PlanExercise?,
    val rows: List<SetRow>,
) {
    val name: String get() = exercise?.name ?: "Exercise"
    val anyDone: Boolean get() = rows.any { it.set.completed }
}

data class WorkoutUiState(
    val loaded: Boolean = false,
    val session: WorkoutSession? = null,
    val units: UnitSystem = UnitSystem.METRIC,
    val blocks: List<ExerciseBlock> = emptyList(),
) {
    val totalSets: Int get() = blocks.sumOf { it.rows.size }
    val doneSets: Int get() = blocks.sumOf { b -> b.rows.count { it.set.completed } }
}

class WorkoutViewModel(private val c: AppContainer, private val sessionId: Long) : ViewModel() {
    private val memory = MutableStateFlow<Map<Long, ExerciseMemory>>(emptyMap())
    private val planItems = MutableStateFlow<Map<Long, PlanExercise>>(emptyMap())
    private val exercises = MutableStateFlow<Map<Long, Exercise>>(emptyMap())
    private var loading = mutableSetOf<Long>()

    val state: StateFlow<WorkoutUiState> = combine(
        c.workouts.session(sessionId), c.workouts.sets(sessionId), c.profiles.currentProfile,
        combine(memory, planItems, exercises) { m, p, e -> Triple(m, p, e) },
    ) { session, sets, profile, (mem, plans, exs) ->
        if (session != null) ensureLoaded(session.profileId, sets)
        val blocks = sets.groupBy { it.exerciseOrder }.toSortedMap().map { (order, list) ->
            val first = list.first()
            val m = mem[first.exerciseId]
            val best = m?.bestScore?.takeIf { it > 0 }
            ExerciseBlock(
                order = order,
                exercise = exs[first.exerciseId],
                plan = first.planExerciseId?.let { plans[it] },
                rows = list.sortedBy { it.setNumber }.map { s ->
                    val prev = m?.lastSets?.let { last -> last.firstOrNull { it.setNumber == s.setNumber } ?: last.lastOrNull() }
                    val score = (s.weightKg ?: 0.0) * (s.reps ?: 0)
                    SetRow(s, prev, isPr = s.completed && best != null && score > best)
                },
            )
        }
        WorkoutUiState(loaded = true, session = session, units = UnitSystem.from(profile?.unitSystem), blocks = blocks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkoutUiState())

    /** Loads last-session numbers, plan settings and names for any exercise we haven't seen yet. */
    private fun ensureLoaded(profileId: Long, sets: List<SetLog>) {
        val newExercises = sets.map { it.exerciseId }.distinct().filter { it !in loading }
        val newPlans = sets.mapNotNull { it.planExerciseId }.distinct().filter { it !in planItems.value }
        if (newExercises.isEmpty() && newPlans.isEmpty()) return
        loading.addAll(newExercises)
        viewModelScope.launch {
            if (newPlans.isNotEmpty()) planItems.value += c.plans.getPlanExercises(newPlans).associateBy { it.id }
            if (newExercises.isNotEmpty()) {
                exercises.value += c.exercises.getAll(newExercises).associateBy { it.id }
                memory.value += newExercises.associateWith { c.workouts.memory(profileId, it, sessionId) }
            }
        }
    }

    sealed interface TickResult {
        data object Done : TickResult
        data class NeedReps(val set: SetLog) : TickResult
    }

    /** Ticks a set. Empty fields are filled from last session (the grey numbers). */
    fun toggle(context: Context, block: ExerciseBlock, row: SetRow): TickResult {
        val s = row.set
        if (s.completed) {
            viewModelScope.launch { c.workouts.updateSet(s.copy(completed = false)) }
            return TickResult.Done
        }
        val above = block.rows.filter { it.set.setNumber < s.setNumber }.lastOrNull()?.set
        val weight = s.weightKg ?: row.previous?.weightKg ?: above?.weightKg ?: 0.0
        val reps = s.reps ?: row.previous?.reps ?: above?.reps
            ?: block.plan?.takeIf { !it.toFailure }?.repMin
            ?: return TickResult.NeedReps(s.copy(weightKg = weight))
        complete(context, block, s.copy(weightKg = weight, reps = reps))
        return TickResult.Done
    }

    fun complete(context: Context, block: ExerciseBlock, set: SetLog) {
        viewModelScope.launch { c.workouts.updateSet(set.copy(completed = true)) }
        val rest = block.plan?.restSeconds ?: c.prefs.defaultRestSeconds.value
        val nextLabel = nextUp(block, set)
        RestTimer.start(context, rest, nextLabel)
    }

    private fun nextUp(block: ExerciseBlock, set: SetLog): String {
        val remainingHere = block.rows.count { !it.set.completed && it.set.id != set.id }
        if (remainingHere > 0) return "Next: ${block.name}, set ${set.setNumber + 1}"
        val next = state.value.blocks.firstOrNull { b -> b.order > block.order && b.rows.any { !it.set.completed } }
        return next?.let { "Next: ${it.name}" } ?: "Last set done — finish when you’re ready"
    }

    fun setWeight(set: SetLog, kg: Double) = viewModelScope.launch { c.workouts.updateSet(set.copy(weightKg = kg)) }
    fun setReps(set: SetLog, reps: Int) = viewModelScope.launch { c.workouts.updateSet(set.copy(reps = reps)) }
    fun addSet(block: ExerciseBlock) = block.rows.lastOrNull()?.let { viewModelScope.launch { c.workouts.addSet(it.set) } }
    fun removeLastSet(block: ExerciseBlock) = block.rows.lastOrNull()?.let { viewModelScope.launch { c.workouts.removeSet(it.set) } }
    fun skip(block: ExerciseBlock) = viewModelScope.launch { c.workouts.skipExercise(sessionId, block.order) }

    fun finish(context: Context, onDone: () -> Unit) = viewModelScope.launch {
        RestTimer.stop(context)
        c.workouts.finish(sessionId)
        onDone()
    }

    fun discard(context: Context, onDone: () -> Unit) = viewModelScope.launch {
        RestTimer.stop(context)
        c.workouts.discard(sessionId)
        onDone()
    }
}
