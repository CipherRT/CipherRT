package com.gymplanner.app.feature.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymplanner.app.AppContainer
import com.gymplanner.app.core.data.BodyWeightEntry
import com.gymplanner.app.core.data.Profile
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.model.Units
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

data class WeightUiState(
    val profile: Profile? = null,
    val entries: List<BodyWeightEntry> = emptyList(),
    val range: WeightRange = WeightRange.M3,
    val points: List<ChartPoint> = emptyList(),
    val average: List<ChartPoint> = emptyList(),
    val latest: BodyWeightEntry? = null,
    val changeSinceStart: Double? = null,
    val change30: Double? = null,
    val bmi: Double? = null,
) {
    val units: UnitSystem get() = UnitSystem.from(profile?.unitSystem)
}

@OptIn(ExperimentalCoroutinesApi::class)
class WeightViewModel(private val c: AppContainer) : ViewModel() {
    private val range = MutableStateFlow(WeightRange.M3)
    private val profile = c.profiles.currentProfile.filterNotNull()

    val state: StateFlow<WeightUiState> = combine(
        profile,
        profile.flatMapLatest { c.weights.entries(it.id) },
        range,
    ) { p, entries, r ->
        val today = Dates.today()
        val daily = WeightLogic.dailyPoints(entries)
        val avg = WeightLogic.sevenDayAverage(daily)
        val latest = entries.firstOrNull()
        WeightUiState(
            profile = p,
            entries = entries,
            range = r,
            points = WeightLogic.filter(daily, r, today),
            average = WeightLogic.filter(avg, r, today),
            latest = latest,
            changeSinceStart = if (daily.size > 1) daily.last().y - daily.first().y else null,
            change30 = WeightLogic.changeOver(daily, 30, today),
            bmi = latest?.let { Units.bmi(it.weightKg, p.heightCm) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeightUiState())

    fun setRange(r: WeightRange) { range.value = r }

    fun delete(entry: BodyWeightEntry) = viewModelScope.launch { c.weights.delete(entry) }
}
