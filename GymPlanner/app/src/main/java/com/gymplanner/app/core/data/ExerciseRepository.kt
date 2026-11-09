package com.gymplanner.app.core.data

import android.content.Context
import com.gymplanner.app.core.model.AppJson
import com.gymplanner.app.core.model.ExerciseLibraryFile
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val context: Context, db: AppDatabase) {
    private val dao = db.exerciseDao()

    fun library(profileId: Long): Flow<List<Exercise>> = dao.observeLibrary(profileId)

    suspend fun get(id: Long) = dao.get(id)
    suspend fun getAll(ids: List<Long>) = if (ids.isEmpty()) emptyList() else dao.getAll(ids)

    suspend fun addCustom(profileId: Long, name: String, muscleGroup: String, equipment: String): Long =
        dao.insert(
            Exercise(
                name = name.trim(),
                muscleGroup = muscleGroup,
                equipment = equipment.trim(),
                isCustom = true,
                profileId = profileId,
            )
        )

    /**
     * Finds an exercise visible to this profile by name (case-insensitive) or creates a
     * custom one. Used when importing plans so matching exercises are reused.
     */
    suspend fun findOrCreate(profileId: Long, name: String, muscleGroup: String, equipment: String): Long {
        val match = dao.library(profileId).firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
        return match?.id ?: addCustom(profileId, name, muscleGroup, equipment)
    }

    /** Loads the built-in library from assets the first time the app runs. */
    suspend fun seedIfNeeded() {
        if (dao.builtInCount() > 0) return
        val text = context.assets.open("templates/exercises.json").bufferedReader().use { it.readText() }
        val file = AppJson.decodeFromString<ExerciseLibraryFile>(text)
        dao.insertAll(
            file.exercises.map {
                Exercise(name = it.name, muscleGroup = it.muscleGroup, equipment = it.equipment, isCustom = false, profileId = null)
            }
        )
    }
}
