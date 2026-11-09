package com.gymplanner.app.core.data

import com.gymplanner.app.core.model.Goal
import com.gymplanner.app.core.model.UnitSystem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

class ProfileRepository(private val db: AppDatabase, private val prefs: Prefs) {
    private val dao = db.profileDao()

    val profiles: Flow<List<Profile>> = dao.observeAll()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProfile: Flow<Profile?> = prefs.currentProfileId.flatMapLatest { id ->
        if (id == null) flowOf(null) else dao.observe(id)
    }

    suspend fun get(id: Long) = dao.get(id)

    /** Creates a profile, saves the first weight entry and (optionally) makes it the current profile. */
    suspend fun create(
        name: String,
        heightCm: Double,
        weightKg: Double,
        units: UnitSystem,
        goal: Goal,
        makeCurrent: Boolean = true,
    ): Long {
        val id = dao.insert(
            Profile(
                name = name.trim(),
                heightCm = heightCm,
                unitSystem = units.name,
                goal = goal.name,
                createdAt = System.currentTimeMillis(),
            )
        )
        if (weightKg > 0) {
            db.weightDao().insert(
                BodyWeightEntry(profileId = id, date = LocalDate.now().toEpochDay(), weightKg = weightKg)
            )
        }
        if (makeCurrent) prefs.setCurrentProfile(id)
        return id
    }

    suspend fun update(profile: Profile) = dao.update(profile)

    fun switchTo(id: Long) = prefs.setCurrentProfile(id)

    /** Deletes a profile and everything that belongs to it. */
    suspend fun delete(profile: Profile) {
        val workoutDao = db.workoutDao()
        workoutDao.allSessions(profile.id).forEach { workoutDao.deleteSession(it) }
        dao.delete(profile) // weights and plans cascade
        db.exerciseDao().deleteCustom(profile.id)
        if (prefs.currentProfileId.value == profile.id) {
            prefs.setCurrentProfile(dao.first()?.id)
        }
    }

    /** On start-up, make sure the remembered profile still exists. */
    suspend fun validateCurrent() {
        val id = prefs.currentProfileId.value
        if (id == null || dao.get(id) == null) prefs.setCurrentProfile(dao.first()?.id)
    }
}
