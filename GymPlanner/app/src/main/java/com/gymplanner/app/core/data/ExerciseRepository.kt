package com.gymplanner.app.core.data

import android.content.Context
import androidx.room.withTransaction
import com.gymplanner.app.core.model.AppJson
import com.gymplanner.app.core.model.ExerciseLibraryFile
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val context: Context, private val db: AppDatabase) {
    private val dao = db.exerciseDao()
    private val guides = db.guideDao()

    fun library(profileId: Long): Flow<List<Exercise>> = dao.observeLibrary(profileId)
    fun guide(exerciseId: Long): Flow<ExerciseGuide?> = guides.observe(exerciseId)

    suspend fun get(id: Long) = dao.get(id)
    suspend fun getAll(ids: List<Long>) = if (ids.isEmpty()) emptyList() else dao.getAll(ids)

    suspend fun saveGuide(guide: ExerciseGuide) = guides.upsert(guide)

    suspend fun addCustom(profileId: Long, name: String, muscleGroup: String, equipment: String): Long =
        dao.insert(
            Exercise(
                name = name.trim(),
                muscleGroup = muscleGroup,
                equipment = equipment.trim().lowercase(),
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

    /**
     * Loads the built-in library and guides from assets/templates/exercises.json.
     * Runs on a fresh install, and again whenever the file's libraryVersion goes up:
     * existing built-ins are updated by name (their ids, and so every plan and log
     * that uses them, stay the same), new ones are added. Custom exercises are untouched.
     */
    suspend fun syncLibrary(appliedVersion: Int, onApplied: (Int) -> Unit) {
        val text = context.assets.open("templates/exercises.json").bufferedReader().use { it.readText() }
        val file = AppJson.decodeFromString<ExerciseLibraryFile>(text)
        if (file.libraryVersion <= appliedVersion && dao.builtInCount() > 0) return
        db.withTransaction {
            val existing = dao.builtIns().associateBy { it.name.lowercase() }.toMutableMap()
            file.exercises.forEach { e ->
                val current = existing[e.name.lowercase()]
                val id = if (current == null) {
                    dao.insert(Exercise(name = e.name, muscleGroup = e.muscleGroup, equipment = e.equipment, isCustom = false, profileId = null))
                } else {
                    if (current.muscleGroup != e.muscleGroup || current.equipment != e.equipment) {
                        dao.update(current.copy(muscleGroup = e.muscleGroup, equipment = e.equipment))
                    }
                    current.id
                }
                e.guide?.let { g ->
                    guides.upsert(ExerciseGuide(id, g.primaryMuscles, g.secondaryMuscles, g.steps, g.mistakes, g.videoQuery))
                }
            }
        }
        onApplied(file.libraryVersion)
    }
}
