package com.gymplanner.app.core.data

import android.content.Context
import androidx.core.content.edit
import com.gymplanner.app.core.ui.Accent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


/** Small app-wide settings that don't belong in the database. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("gymplanner", Context.MODE_PRIVATE)

    private val _currentProfileId = MutableStateFlow(sp.getLong(KEY_PROFILE, 0L).takeIf { it > 0 })
    val currentProfileId: StateFlow<Long?> = _currentProfileId.asStateFlow()

    private val _accent = MutableStateFlow(Accent.from(sp.getString(KEY_ACCENT, null)))
    val accent: StateFlow<Accent> = _accent.asStateFlow()

    private val _defaultRest = MutableStateFlow(sp.getInt(KEY_REST, 90))
    val defaultRestSeconds: StateFlow<Int> = _defaultRest.asStateFlow()

    fun setCurrentProfile(id: Long?) {
        sp.edit { if (id == null) remove(KEY_PROFILE) else putLong(KEY_PROFILE, id) }
        _currentProfileId.value = id
    }

    fun setAccent(accent: Accent) {
        sp.edit { putString(KEY_ACCENT, accent.name) }
        _accent.value = accent
    }

    fun setDefaultRest(seconds: Int) {
        sp.edit { putInt(KEY_REST, seconds) }
        _defaultRest.value = seconds
    }

    private companion object {
        const val KEY_PROFILE = "current_profile"
        const val KEY_ACCENT = "accent"
        const val KEY_REST = "default_rest"
    }
}
