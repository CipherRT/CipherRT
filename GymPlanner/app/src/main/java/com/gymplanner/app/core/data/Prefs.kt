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

    /** Which version of the built-in exercise library has been loaded into the database. */
    var libraryVersion: Int
        get() = sp.getInt(KEY_LIBRARY, 0)
        set(value) = sp.edit { putInt(KEY_LIBRARY, value) }

    private val _notTodayDay = MutableStateFlow(sp.getLong(KEY_NOT_TODAY, -1L))
    /** Epoch day on which "Not today" was tapped (that day shows as an off day). */
    val notTodayDay: StateFlow<Long> = _notTodayDay.asStateFlow()

    fun setNotToday(epochDay: Long) {
        sp.edit { putLong(KEY_NOT_TODAY, epochDay) }
        _notTodayDay.value = epochDay
    }

    /** The one-time "allow full-screen weigh-in" question has been asked (Android 14+). */
    var fullScreenAsked: Boolean
        get() = sp.getBoolean(KEY_FS_ASKED, false)
        set(value) = sp.edit { putBoolean(KEY_FS_ASKED, value) }

    /** When a backup was last saved (0 = never). */
    var lastBackupAt: Long
        get() = sp.getLong(KEY_LAST_BACKUP, 0L)
        set(value) = sp.edit { putLong(KEY_LAST_BACKUP, value) }

    /** The version whose "make a backup" reminder has been shown. */
    var backupReminderVersion: Int
        get() = sp.getInt(KEY_BACKUP_REMINDER, 0)
        set(value) = sp.edit { putInt(KEY_BACKUP_REMINDER, value) }

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
        const val KEY_LIBRARY = "library_version"
        const val KEY_NOT_TODAY = "not_today"
        const val KEY_FS_ASKED = "fullscreen_asked"
        const val KEY_LAST_BACKUP = "last_backup"
        const val KEY_BACKUP_REMINDER = "backup_reminder"
        const val KEY_REST = "default_rest"
    }
}
