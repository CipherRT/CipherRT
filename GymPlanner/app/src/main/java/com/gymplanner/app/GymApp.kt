package com.gymplanner.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import com.gymplanner.app.feature.weighin.WeighInScheduler

private data class WeighInKey(val id: Long, val on: Boolean, val time: Int, val days: Int)

class GymApp : Application() {
    lateinit var container: AppContainer
        private set

    /** Becomes true once the exercise library is seeded and the current profile is checked. */
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.exercises.syncLibrary(container.prefs.libraryVersion) { container.prefs.libraryVersion = it }
            container.profiles.validateCurrent()
            _ready.value = true
        }
        // Keep each profile's morning weigh-in alarm in step with its settings.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            var known = emptySet<Long>()
            container.profiles.profiles
                .map { list -> list.map { WeighInKey(it.id, it.weighInEnabled, it.weighInTime, it.weighInDays) } to list }
                .distinctUntilChangedBy { it.first }
                .collect { (_, list) ->
                    (known - list.map { it.id }.toSet()).forEach { WeighInScheduler.cancel(this@GymApp, it) }
                    list.forEach { WeighInScheduler.schedule(this@GymApp, it) }
                    known = list.map { it.id }.toSet()
                }
        }
    }
}
