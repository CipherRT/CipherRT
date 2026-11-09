package com.gymplanner.app.core.data

import androidx.room.withTransaction
import com.gymplanner.app.core.model.AppJson
import com.gymplanner.app.core.model.BackupFile
import com.gymplanner.app.core.model.BackupPlanJson
import com.gymplanner.app.core.model.CURRENT_FORMAT_VERSION
import com.gymplanner.app.core.model.ExerciseJson
import com.gymplanner.app.core.model.ProfileJson
import com.gymplanner.app.core.model.SessionJson
import com.gymplanner.app.core.model.SetLogJson
import com.gymplanner.app.core.model.WeightJson

/** Full private backup of one profile: profile, weights, plans and workout history. */
class BackupRepository(
    private val db: AppDatabase,
    private val plans: PlanRepository,
    private val exercises: ExerciseRepository,
    private val prefs: Prefs,
) {
    suspend fun export(profileId: Long): String {
        val profile = db.profileDao().get(profileId) ?: error("Profile not found")
        val workoutDao = db.workoutDao()
        val sessions = workoutDao.allSessions(profileId)
        val allSets = if (sessions.isEmpty()) emptyList() else workoutDao.setsForSessions(sessions.map { it.id })
        val names = db.exerciseDao().library(profileId).associate { it.id to it.name }.toMutableMap()
        // History can reference exercises from other places; look up anything missing.
        allSets.map { it.exerciseId }.distinct().filter { it !in names }.forEach { id ->
            db.exerciseDao().get(id)?.let { names[id] = it.name }
        }
        val setsBySession = allSets.groupBy { it.sessionId }

        val file = BackupFile(
            exportedAt = System.currentTimeMillis(),
            profile = ProfileJson(profile.name, profile.heightCm, profile.unitSystem, profile.goal, profile.createdAt),
            customExercises = db.exerciseDao().custom(profileId).map { ExerciseJson(it.name, it.muscleGroup, it.equipment) },
            weights = db.weightDao().all(profileId).map { WeightJson(it.date, it.weightKg, it.note) },
            plans = db.planDao().plans(profileId).map { BackupPlanJson(it.isActive, plans.toJson(it.id)) },
            sessions = sessions.map { s ->
                SessionJson(
                    dayName = s.dayName, startedAt = s.startedAt, finishedAt = s.finishedAt,
                    note = s.note, isLight = s.isLight,
                    sets = setsBySession[s.id].orEmpty().map {
                        SetLogJson(names[it.exerciseId] ?: "Unknown", it.setNumber, it.weightKg, it.reps, it.completed, it.exerciseOrder)
                    },
                )
            },
        )
        return AppJson.encodeToString(BackupFile.serializer(), file)
    }

    /** Restores a backup as a new profile (never overwrites anything) and switches to it. */
    suspend fun restore(text: String): Long {
        val file = AppJson.decodeFromString<BackupFile>(text)
        require(file.type == "backup") { "This file isn't a backup. To add a shared plan, use Plan → Import." }
        require(file.formatVersion <= CURRENT_FORMAT_VERSION) { "This backup was made with a newer version of the app." }

        val profileId = db.withTransaction {
            val p = file.profile
            val id = db.profileDao().insert(
                Profile(name = p.name, heightCm = p.heightCm, unitSystem = p.unitSystem, goal = p.goal, createdAt = p.createdAt)
            )
            file.customExercises.forEach { exercises.findOrCreate(id, it.name, it.muscleGroup, it.equipment) }
            db.weightDao().insertAll(file.weights.map { BodyWeightEntry(profileId = id, date = it.date, weightKg = it.weightKg, note = it.note) })
            file.plans.sortedBy { it.isActive }.forEach { plans.importPlan(id, it.plan, makeActive = it.isActive) }
            val cache = mutableMapOf<String, Long>()
            file.sessions.forEach { s ->
                val sid = db.workoutDao().insertSession(
                    WorkoutSession(
                        profileId = id, planDayId = null, dayName = s.dayName, startedAt = s.startedAt,
                        finishedAt = s.finishedAt ?: s.startedAt, note = s.note, isLight = s.isLight,
                    )
                )
                db.workoutDao().insertSets(
                    s.sets.map {
                        val exId = cache.getOrPut(it.exercise.lowercase()) { exercises.findOrCreate(id, it.exercise, "other", "") }
                        SetLog(sessionId = sid, exerciseId = exId, setNumber = it.setNumber, weightKg = it.weightKg, reps = it.reps, completed = it.completed, exerciseOrder = it.order)
                    }
                )
            }
            id
        }
        prefs.setCurrentProfile(profileId)
        return profileId
    }
}
