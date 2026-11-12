package com.gymplanner.app.feature.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymplanner.app.AppContainer
import com.gymplanner.app.core.data.Exercise
import com.gymplanner.app.core.data.ExerciseMemory
import com.gymplanner.app.core.data.PlanExercise
import com.gymplanner.app.core.data.Profile
import com.gymplanner.app.core.data.SetLog
import com.gymplanner.app.core.data.SetType
import com.gymplanner.app.core.data.WorkoutSession
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.model.Units
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One set row. [placeholderWeightKg]/[placeholderReps] are the grey "last time" (or
 * drop −20 %) values used when the set is logged without typing anything.
 */
data class SetRow(
    val set: SetLog,
    val previous: SetLog?,
    val isPr: Boolean,
    val placeholderWeightKg: Double?,
    val placeholderReps: Int?,
) {
    val isDrop: Boolean get() = set.setType == SetType.DROP
    val isWarmup: Boolean get() = set.setType == SetType.WARMUP
    val weightKg: Double? get() = set.weightKg ?: placeholderWeightKg
    val reps: Int? get() = set.reps ?: placeholderReps
}

data class ExerciseBlock(
    val order: Int,
    val exercise: Exercise?,
    val plan: PlanExercise?,
    val rows: List<SetRow>,
) {
    val name: String get() = exercise?.name ?: "Exercise"
    val anyDone: Boolean get() = rows.any { it.set.completed }
    val allDone: Boolean get() = rows.isNotEmpty() && rows.all { it.set.completed }
    val isBarbell: Boolean get() = exercise?.equipment == "barbell"

    /** Main sets with their drop sets, in order: one "round" each. */
    val rounds: List<List<SetRow>>
        get() {
            val out = mutableListOf<MutableList<SetRow>>()
            rows.forEach { r -> if (!r.isDrop || out.isEmpty()) out += mutableListOf(r) else out.last() += r }
            return out
        }
}

/** One swipe page: a single exercise, or a superset of exercises done in turn. */
data class WorkoutPage(val blocks: List<ExerciseBlock>) {
    val isSuperset: Boolean get() = blocks.size > 1
    val allDone: Boolean get() = blocks.all { it.allDone }

    /** Sets in the order they're done: for a superset, set 1 of A, set 1 of B, set 2 of A … */
    val ordered: List<Pair<ExerciseBlock, SetRow>>
        get() {
            val rounds = blocks.map { it.rounds }
            val max = rounds.maxOfOrNull { it.size } ?: 0
            return (0 until max).flatMap { k -> blocks.indices.flatMap { b -> rounds[b].getOrNull(k).orEmpty().map { blocks[b] to it } } }
        }

    val active: Pair<ExerciseBlock, SetRow>? get() = ordered.firstOrNull { !it.second.set.completed }
}

data class WorkoutUiState(
    val loaded: Boolean = false,
    val session: WorkoutSession? = null,
    val profile: Profile? = null,
    val pages: List<WorkoutPage> = emptyList(),
) {
    val units: UnitSystem get() = UnitSystem.from(profile?.unitSystem)
    private val working get() = pages.flatMap { p -> p.blocks.flatMap { it.rows } }.filter { !it.isWarmup && !it.isDrop }
    val totalSets: Int get() = working.size
    val doneSets: Int get() = working.count { it.set.completed }
    val allRows: List<SetRow> get() = pages.flatMap { p -> p.blocks.flatMap { it.rows } }
}

class WorkoutViewModel(private val c: AppContainer, private val sessionId: Long) : ViewModel() {
    private val memory = MutableStateFlow<Map<Long, ExerciseMemory>>(emptyMap())
    private val planItems = MutableStateFlow<Map<Long, PlanExercise>>(emptyMap())
    private val exercises = MutableStateFlow<Map<Long, Exercise>>(emptyMap())
    private val loading = mutableSetOf<Long>()

    val state: StateFlow<WorkoutUiState> = combine(
        c.workouts.session(sessionId), c.workouts.sets(sessionId), c.profiles.currentProfile,
        combine(memory, planItems, exercises) { m, p, e -> Triple(m, p, e) },
    ) { session, sets, profile, (mem, plans, exs) ->
        if (session != null) ensureLoaded(session.profileId, sets)
        val units = UnitSystem.from(profile?.unitSystem)
        val blocks = sets.groupBy { it.exerciseOrder }.toSortedMap().map { (order, list) ->
            buildBlock(order, list, mem[list.first().exerciseId], plans[list.first().planExerciseId], exs[list.first().exerciseId], units)
        }
        // Consecutive exercises with the same superset group share a page.
        val pages = mutableListOf<MutableList<ExerciseBlock>>()
        blocks.forEach { b ->
            val group = b.plan?.supersetGroup
            val last = pages.lastOrNull()?.lastOrNull()
            if (group != null && last?.plan?.supersetGroup == group) pages.last() += b else pages += mutableListOf(b)
        }
        WorkoutUiState(loaded = true, session = session, profile = profile, pages = pages.map { WorkoutPage(it) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkoutUiState())

    private fun buildBlock(order: Int, list: List<SetLog>, m: ExerciseMemory?, plan: PlanExercise?, ex: Exercise?, units: UnitSystem): ExerciseBlock {
        val best = m?.bestScore?.takeIf { it > 0 }
        val lastMains = m?.lastSets.orEmpty().filter { it.setType != SetType.DROP }
        val lastDrops = m?.lastSets.orEmpty().filter { it.setType == SetType.DROP }
        val rows = mutableListOf<SetRow>()
        var dropIndex = 0
        list.forEach { s ->
            val isDrop = s.setType == SetType.DROP
            if (!isDrop) dropIndex = 0
            val previous = if (isDrop) {
                lastDrops.filter { it.setNumber == s.setNumber }.getOrNull(dropIndex++)
            } else lastMains.firstOrNull { it.setNumber == s.setNumber } ?: lastMains.lastOrNull()
            val above = rows.lastOrNull()
            val mainAbove = rows.lastOrNull { !it.isDrop }
            val weight = s.weightKg ?: previous?.weightKg ?: when {
                isDrop -> above?.weightKg?.let { kg ->
                    Units.displayToKg(PlateCalculator.roundToLoadable(Units.kgToDisplay(kg, units) * 0.8, units), units)
                }
                else -> mainAbove?.weightKg
            }
            val reps = s.reps ?: previous?.reps ?: (if (isDrop) null else mainAbove?.reps)
                ?: plan?.repMin?.takeIf { it > 0 && !isDrop }
            val score = (s.weightKg ?: 0.0) * (s.reps ?: 0)
            rows += SetRow(s, previous, s.completed && s.setType != SetType.WARMUP && best != null && score > best, weight, reps)
        }
        return ExerciseBlock(order, ex, plan, rows)
    }

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

    sealed interface LogResult {
        data object Done : LogResult
        data class NeedReps(val block: ExerciseBlock, val row: SetRow) : LogResult
    }

    /** Ticks or un-ticks a set from the table. */
    fun toggle(context: Context, page: WorkoutPage, block: ExerciseBlock, row: SetRow): LogResult {
        if (row.set.completed) {
            viewModelScope.launch { c.workouts.updateSet(row.set.copy(completed = false)) }
            return LogResult.Done
        }
        return log(context, page, block, row)
    }

    /** Logs a set with its typed or grey values, then starts the rest timer (after the pair, in a superset). */
    fun log(context: Context, page: WorkoutPage, block: ExerciseBlock, row: SetRow, repsOverride: Int? = null): LogResult {
        val reps = repsOverride ?: row.reps ?: return LogResult.NeedReps(block, row)
        val done = row.set.copy(weightKg = row.weightKg ?: 0.0, reps = reps, completed = true)
        viewModelScope.launch { c.workouts.updateSet(done) }

        val order = page.ordered
        val i = order.indexOfFirst { it.second.set.id == row.set.id }
        val next = order.drop(i + 1).firstOrNull { !it.second.set.completed }
        val midSuperset = page.isSuperset && next != null && page.blocks.indexOf(next.first) > page.blocks.indexOf(block)
        val nextDrop = next?.second?.isDrop == true && next.first == block
        if (!midSuperset && !nextDrop) {
            val pages = state.value.pages
            val nextPage = pages.drop(pages.indexOf(page) + 1).firstOrNull { !it.allDone }
            val label = when {
                next != null -> "Next: ${next.first.name}, set ${next.second.set.setNumber}"
                nextPage != null -> "Next: ${nextPage.blocks.joinToString(" + ") { it.name }}"
                else -> null
            }
            if (label != null) {
                val rest = page.blocks.first().plan?.restSeconds ?: c.prefs.defaultRestSeconds.value
                RestTimer.start(context, rest, label)
            } else RestTimer.stop(context)
        }
        return LogResult.Done
    }

    fun adjustWeight(row: SetRow, deltaDisplay: Double, units: UnitSystem) {
        val base = row.weightKg?.let { Units.kgToDisplay(it, units) } ?: 0.0
        val next = (Math.round((base + deltaDisplay) * 100) / 100.0).coerceAtLeast(0.0)
        setWeight(row.set, Units.displayToKg(next, units))
    }

    fun adjustReps(row: SetRow, delta: Int) = setReps(row.set, ((row.reps ?: 0) + delta).coerceAtLeast(0))

    fun setWeight(set: SetLog, kg: Double) = viewModelScope.launch { c.workouts.updateSet(set.copy(weightKg = kg)) }
    fun setReps(set: SetLog, reps: Int) = viewModelScope.launch { c.workouts.updateSet(set.copy(reps = reps)) }
    fun addSet(block: ExerciseBlock) = block.rows.lastOrNull()?.let { viewModelScope.launch { c.workouts.addSet(it.set) } }
    fun addDrop(block: ExerciseBlock, under: SetRow? = null) {
        val parent = under?.set ?: block.rows.lastOrNull { !it.isDrop && !it.isWarmup }?.set ?: return
        viewModelScope.launch { c.workouts.addDropSet(parent) }
    }
    fun removeSet(row: SetRow) = viewModelScope.launch { c.workouts.removeSet(row.set) }
    fun skip(block: ExerciseBlock) = viewModelScope.launch { c.workouts.skipExercise(sessionId, block.order) }

    fun setBarWeight(kg: Double) {
        val p = state.value.profile ?: return
        viewModelScope.launch { c.profiles.update(p.copy(barWeightKg = kg)) }
    }

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
