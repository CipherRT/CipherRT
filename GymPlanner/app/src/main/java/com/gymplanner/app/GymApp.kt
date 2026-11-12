package com.gymplanner.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
    }
}
