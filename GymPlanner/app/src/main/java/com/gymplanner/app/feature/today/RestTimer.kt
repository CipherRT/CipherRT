package com.gymplanner.app.feature.today

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A running rest countdown. [endAt] is wall-clock millis. */
data class RestState(val endAt: Long, val totalSeconds: Int, val label: String) {
    fun remainingMs(now: Long = System.currentTimeMillis()) = (endAt - now).coerceAtLeast(0)
}

/**
 * The rest timer the workout screen shows. The actual countdown, notification and
 * vibration live in [RestTimerService] so they keep working with the screen off.
 */
object RestTimer {
    private val _state = MutableStateFlow<RestState?>(null)
    val state: StateFlow<RestState?> = _state.asStateFlow()

    fun start(context: Context, seconds: Int, label: String) {
        if (seconds <= 0) { stop(context); return }
        _state.value = RestState(System.currentTimeMillis() + seconds * 1000L, seconds, label)
        send(context, RestTimerService.ACTION_START)
    }

    fun add(context: Context, seconds: Int) {
        val s = _state.value ?: return
        val newEnd = s.endAt + seconds * 1000L
        if (newEnd <= System.currentTimeMillis()) { stop(context); return }
        _state.value = s.copy(endAt = newEnd, totalSeconds = (s.totalSeconds + seconds).coerceAtLeast(1))
        send(context, RestTimerService.ACTION_START)
    }

    fun stop(context: Context) {
        if (_state.value == null) return
        _state.value = null
        send(context, RestTimerService.ACTION_STOP)
    }

    /** Called by the service when time is up. */
    internal fun finished() { _state.value = null }

    private fun send(context: Context, action: String) {
        val intent = Intent(context, RestTimerService::class.java).setAction(action)
        if (action == RestTimerService.ACTION_START) ContextCompat.startForegroundService(context, intent)
        else context.startService(intent)
    }
}
