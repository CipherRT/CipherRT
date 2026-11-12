package com.gymplanner.app.feature.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymplanner.app.AppContainer
import com.gymplanner.app.core.data.BodyMeasurement
import com.gymplanner.app.core.data.CardioSession
import com.gymplanner.app.core.data.Profile
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.feature.today.StreakLogic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Shared between the Track tab's sections so switching keeps your place. */
enum class TrackSection(val label: String) { WEIGHT("Weight"), CARDIO("Cardio"), BODY("Body") }

data class CardioUiState(
    val profile: Profile? = null,
    val sessions: List<CardioSession> = emptyList(),
    /** kcal per day, Monday … Sunday of this week. */
    val week: List<Int> = List(7) { 0 },
    val weekKcal: Int = 0,
    val weekSessions: Int = 0,
)

object CardioLogic {
    fun week(sessions: List<CardioSession>, today: LocalDate): List<Int> {
        val monday = StreakLogic.weekStart(today)
        return (0..6).map { i -> sessions.filter { it.date == monday.plusDays(i.toLong()).toEpochDay() }.sumOf { it.kcal } }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CardioViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<CardioUiState> = c.profiles.currentProfile.filterNotNull().flatMapLatest { p ->
        c.track.cardio(p.id).map { list ->
            val today = LocalDate.now()
            val monday = StreakLogic.weekStart(today).toEpochDay()
            val thisWeek = list.filter { it.date >= monday }
            CardioUiState(p, list, CardioLogic.week(list, today), thisWeek.sumOf { it.kcal }, thisWeek.size)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CardioUiState())

    fun save(session: CardioSession, onDone: () -> Unit) = viewModelScope.launch {
        if (session.id == 0L) c.track.addCardio(session) else c.track.updateCardio(session)
        onDone()
    }

    suspend fun load(id: Long): CardioSession? = c.track.getCardio(id)

    fun delete(session: CardioSession, onDone: () -> Unit) = viewModelScope.launch {
        c.track.deleteCardio(session); onDone()
    }
}

enum class Measure(val label: String) { WAIST("Waist"), ARM("Arm"), CHEST("Chest"), THIGH("Thigh") }

fun BodyMeasurement.value(m: Measure): Double? = when (m) {
    Measure.WAIST -> waistCm; Measure.ARM -> armCm; Measure.CHEST -> chestCm; Measure.THIGH -> thighCm
}

data class BodyUiState(
    val profile: Profile? = null,
    val entries: List<BodyMeasurement> = emptyList(),
    val selected: Measure = Measure.WAIST,
) {
    val units: UnitSystem get() = UnitSystem.from(profile?.unitSystem)
    /** Latest value of each measurement, with its change since the first entry. */
    fun latest(m: Measure): Pair<Double, Double?>? {
        val withValue = entries.filter { it.value(m) != null }
        val last = withValue.firstOrNull()?.value(m) ?: return null
        val first = withValue.lastOrNull()?.value(m)
        return last to if (withValue.size > 1 && first != null) last - first else null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BodyViewModel(private val c: AppContainer) : ViewModel() {
    private val selected = MutableStateFlow(Measure.WAIST)
    val state: StateFlow<BodyUiState> = c.profiles.currentProfile.filterNotNull().flatMapLatest { p ->
        combine(c.track.measurements(p.id), selected) { list, m -> BodyUiState(p, list, m) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BodyUiState())

    fun select(m: Measure) { selected.value = m }

    fun save(m: BodyMeasurement) = viewModelScope.launch {
        if (m.id == 0L) c.track.addMeasurement(m) else c.track.updateMeasurement(m)
    }

    fun delete(m: BodyMeasurement) = viewModelScope.launch { c.track.deleteMeasurement(m) }
}
