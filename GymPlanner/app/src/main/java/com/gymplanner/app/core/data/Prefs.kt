package com.gymplanner.app.core.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) { DARK("Dark"), LIGHT("Light"), SYSTEM("System") }

/** Small app-wide settings that don't belong in the database. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("gymplanner", Context.MODE_PRIVATE)

    private val _currentProfileId = MutableStateFlow(sp.getLong(KEY_PROFILE, 0L).takeIf { it > 0 })
    val currentProfileId: StateFlow<Long?> = _currentProfileId.asStateFlow()

    private val _theme = MutableStateFlow(
        ThemeMode.entries.firstOrNull { it.name == sp.getString(KEY_THEME, null) } ?: ThemeMode.DARK
    )
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _defaultRest = MutableStateFlow(sp.getInt(KEY_REST, 90))
    val defaultRestSeconds: StateFlow<Int> = _defaultRest.asStateFlow()

    fun setCurrentProfile(id: Long?) {
        sp.edit { if (id == null) remove(KEY_PROFILE) else putLong(KEY_PROFILE, id) }
        _currentProfileId.value = id
    }

    fun setTheme(mode: ThemeMode) {
        sp.edit { putString(KEY_THEME, mode.name) }
        _theme.value = mode
    }

    fun setDefaultRest(seconds: Int) {
        sp.edit { putInt(KEY_REST, seconds) }
        _defaultRest.value = seconds
    }

    private companion object {
        const val KEY_PROFILE = "current_profile"
        const val KEY_THEME = "theme"
        const val KEY_REST = "default_rest"
    }
}
