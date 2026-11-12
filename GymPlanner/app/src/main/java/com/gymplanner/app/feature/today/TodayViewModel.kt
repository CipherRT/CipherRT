package com.gymplanner.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymplanner.app.AppContainer
import com.gymplanner.app.core.data.BodyWeightEntry
import com.gymplanner.app.core.data.CardioSession
import com.gymplanner.app.core.data.Exercise
import com.gymplanner.app.core.data.Plan
import com.gymplanner.app.core.data.PlanDay
import com.gymplanner.app.core.data.PlanExercise
import com.gymplanner.app.core.data.Profile
import com.gymplanner.app.core.data.SetType
import com.gymplanner.app.core.data.WorkoutSession
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.ui.ChartPoint
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.feature.weight.WeightLogic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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

/** One dot in the week strip: Monday … Sunday. */
data class WeekDot(val date: LocalDate, val gym: Boolean, val cardio: Boolean) {
    val isToday: Boolean get() = date == LocalDate.now()
    val isFuture: Boolean get() = date.isAfter(LocalDate.now())
}

data class TodayUiState(
    val loaded: Boolean = false,
    val profile: Profile? = null,
    val plan: Plan? = null,
    val days: List<PlanDay> = emptyList(),
    /** The day the rotation suggests. */
    val nextDay: PlanDay? = null,
    /** The day shown (the suggestion, or one picked with "Different day"). */
    val day: PlanDay? = null,
    val exercises: List<Pair<PlanExercise, Exercise?>> = emptyList(),
    val unfinished: WorkoutSession? = null,
    /** Working sets ticked / planned in the open workout. */
    val openDone: Int = 0,
    val openTotal: Int = 0,
    /** Trained already today, or tapped "Not today" (and no workout is open). */
    val offDay: Boolean = false,
    val week: List<WeekDot> = emptyList(),
    val weekSessions: Int = 0,
    val streak: Streak = Streak(0, 0, 0),
    val latestWeight: BodyWeightEntry? = null,
    val todayWeight: BodyWeightEntry? = null,
    /** Today's (or latest) weight minus the 7-day average. */
    val vsAverage: Double? = null,
    val trend: List<ChartPoint> = emptyList(),
    val cardioKcalWeek: Int = 0,
) {
    val units: UnitSystem get() = UnitSystem.from(profile?.unitSystem)
}

private data class Base(
    val profile: Profile,
    val plan: Plan?,
    val days: List<PlanDay>,
    val finished: List<WorkoutSession>,
    val unfinished: WorkoutSession?,
)

private data class Extras(
    val weights: List<BodyWeightEntry>,
    val cardio: List<CardioSession>,
    val notToday: Long,
    val openSets: Pair<Int, Int>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val c: AppContainer) : ViewModel() {
    private val chosenDayId = MutableStateFlow<Long?>(null)
    private val trainAnyway = MutableStateFlow(false)
    private val profile = c.profiles.currentProfile.filterNotNull()

    private val base: Flow<Base> = profile.flatMapLatest { p ->
        val planFlow = c.plans.activePlan(p.id)
        val daysFlow = planFlow.flatMapLatest { plan -> if (plan == null) flowOf(emptyList()) else c.plans.days(plan.id) }
        combine(planFlow, daysFlow, c.workouts.finishedSessions(p.id), c.workouts.unfinished(p.id)) { plan, days, finished, open ->
            Base(p, plan, days, finished, open)
        }
    }

    private val extras: Flow<Extras> = profile.flatMapLatest { p ->
        val openSets = c.workouts.unfinished(p.id).flatMapLatest { s ->
            if (s == null) flowOf(0 to 0) else c.workouts.sets(s.id).map { sets ->
                val working = sets.filter { it.setType != SetType.WARMUP && it.setType != SetType.DROP }
                working.count { it.completed } to working.size
            }
        }
        combine(
            c.weights.entries(p.id),
            c.track.cardioSince(p.id, StreakLogic.weekStart(LocalDate.now()).minusWeeks(1).toEpochDay()),
            c.prefs.notTodayDay,
            openSets,
        ) { w, cardio, notToday, open -> Extras(w, cardio, notToday, open) }
    }

    val state: StateFlow<TodayUiState> = combine(base, extras, chosenDayId, trainAnyway) { b, x, chosen, anyway ->
        Triple(b, x, chosen to anyway)
    }.flatMapLatest { (b, x, choice) ->
        val (chosen, anyway) = choice
        val today = LocalDate.now()
        val history = b.finished.map { DoneWorkout(it.planDayId, it.dayName, Dates.toLocalDate(it.startedAt), it.startedAt) }
        val next = TodayLogic.nextDay(b.days, history, today)
        val day = b.days.firstOrNull { it.id == chosen } ?: next
        val trainedToday = history.any { it.date == today }
        val offDay = b.unfinished == null && !anyway && chosen == null &&
            (trainedToday || x.notToday == today.toEpochDay())

        val gymDates = history.map { it.date }
        val cardioDates = x.cardio.map { LocalDate.ofEpochDay(it.date) }
        val monday = StreakLogic.weekStart(today)
        val week = (0..6).map { i ->
            val d = monday.plusDays(i.toLong())
            WeekDot(d, gym = d in gymDates, cardio = d in cardioDates)
        }
        val countsCardio = b.profile.cardioCountsTowardTarget
        // Streak needs all cardio days, not just recent ones, when cardio counts.
        val streakFlow = if (countsCardio) c.track.cardio(b.profile.id).map { all -> all.map { LocalDate.ofEpochDay(it.date) } }
        else flowOf(emptyList())

        val daily = WeightLogic.dailyPoints(x.weights)
        val avg = WeightLogic.sevenDayAverage(daily)
        val latest = x.weights.firstOrNull()
        val todayWeight = latest?.takeIf { it.date == today.toEpochDay() }

        val exercisesFlow = if (day == null) flowOf(emptyList()) else combine(
            c.plans.dayExercises(day.id), c.exercises.library(b.profile.id),
        ) { list, lib ->
            val byId = lib.associateBy { it.id }
            list.map { it to byId[it.exerciseId] }
        }
        combine(exercisesFlow, streakFlow) { exercises, allCardioDates ->
            val sessionDates = gymDates + if (countsCardio) allCardioDates else emptyList()
            val streak = StreakLogic.compute(sessionDates, b.profile.weeklyTarget, today)
            TodayUiState(
                loaded = true,
                profile = b.profile,
                plan = b.plan,
                days = b.days,
                nextDay = next,
                day = day,
                exercises = exercises,
                unfinished = b.unfinished,
                openDone = x.openSets.first,
                openTotal = x.openSets.second,
                offDay = offDay,
                week = week,
                weekSessions = streak.thisWeek,
                streak = streak,
                latestWeight = latest,
                todayWeight = todayWeight,
                vsAverage = latest?.let { l -> avg.lastOrNull()?.let { l.weightKg - it.y } },
                trend = daily.filter { it.x > today.toEpochDay() - 14 },
                cardioKcalWeek = x.cardio.filter { !LocalDate.ofEpochDay(it.date).isBefore(monday) }.sumOf { it.kcal },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    fun chooseDay(id: Long) { chosenDayId.value = id; trainAnyway.value = true }

    fun notToday() = c.prefs.setNotToday(LocalDate.now().toEpochDay())

    fun start(isLight: Boolean, day: PlanDay? = state.value.day, onStarted: (Long) -> Unit) {
        val p = state.value.profile ?: return
        val d = day ?: return
        viewModelScope.launch {
            val id = c.workouts.start(p.id, d, isLight)
            chosenDayId.value = null
            trainAnyway.value = false
            onStarted(id)
        }
    }
}
