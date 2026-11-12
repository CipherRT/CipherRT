package com.gymplanner.app.core.data

import androidx.room.withTransaction
import com.gymplanner.app.core.model.AppJson
import com.gymplanner.app.core.model.BackupFile
import com.gymplanner.app.core.model.BackupPlanJson
import com.gymplanner.app.core.model.CURRENT_FORMAT_VERSION
import com.gymplanner.app.core.model.CardioJson
import com.gymplanner.app.core.model.ExerciseJson
import com.gymplanner.app.core.model.GuideJson
import com.gymplanner.app.core.model.MeasurementJson
import com.gymplanner.app.core.model.ProfileJson
import com.gymplanner.app.core.model.SessionJson
import com.gymplanner.app.core.model.SetLogJson
import com.gymplanner.app.core.model.WeightJson

/**
 * Full private backup of one profile: profile and settings, weights, measurements,
 * cardio, plans, workouts and sets. Reads format 1 (v1) and 2.
 */
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
        val custom = db.exerciseDao().custom(profileId)

        val file = BackupFile(
            exportedAt = System.currentTimeMillis(),
            profile = ProfileJson(
                profile.name, profile.heightCm, profile.unitSystem, profile.goal, profile.createdAt,
                profile.weeklyTarget, profile.weighInTime, profile.weighInDays, profile.weighInEnabled,
                profile.cardioCountsTowardTarget, profile.barWeightKg,
            ),
            customExercises = custom.map { e ->
                val g = db.guideDao().get(e.id)
                ExerciseJson(e.name, e.muscleGroup, e.equipment, g?.let { GuideJson(it.primaryMuscles, it.secondaryMuscles, it.steps, it.mistakes, it.videoQuery) })
            },
            weights = db.weightDao().all(profileId).map { WeightJson(it.date, it.weightKg, it.note) },
            plans = db.planDao().plans(profileId).map { BackupPlanJson(it.isActive, plans.toJson(it.id)) },
            sessions = sessions.map { s ->
                val list = setsBySession[s.id].orEmpty().sortedWith(compareBy({ it.exerciseOrder }, { it.setNumber }, { it.parentSetId != null }, { it.id }))
                val index = list.withIndex().associate { (i, set) -> set.id to i }
                SessionJson(
                    dayName = s.dayName, startedAt = s.startedAt, finishedAt = s.finishedAt,
                    note = s.note, isLight = s.isLight,
                    sets = list.map {
                        SetLogJson(
                            names[it.exerciseId] ?: "Unknown", it.setNumber, it.weightKg, it.reps, it.completed, it.exerciseOrder,
                            setType = it.setType, parent = it.parentSetId?.let { p -> index[p] },
                        )
                    },
                )
            },
            cardio = db.trackDao().allCardio(profileId).map {
                CardioJson(it.date, it.machine, it.customName, it.minutes, it.kcal, it.distanceKm, it.avgHeartRate, it.note)
            },
            measurements = db.trackDao().allMeasurements(profileId).map { MeasurementJson(it.date, it.waistCm, it.armCm, it.chestCm, it.thighCm) },
        )
        return AppJson.encodeToString(BackupFile.serializer(), file)
    }

    /** Restores a backup as a new profile (never overwrites anything) and switches to it. */
    suspend fun restore(text: String): Long {
        val file = AppJson.decodeFromString<BackupFile>(text)
        require(file.type == "backup") { "This file isn't a backup. To add a shared plan, use Plan → Import." }
        require(file.formatVersion <= CURRENT_FORMAT_VERSION) { "This backup was made with a newer version of the app. Please update." }

        val profileId = db.withTransaction {
            val p = file.profile
            val id = db.profileDao().insert(
                Profile(
                    name = p.name, heightCm = p.heightCm, unitSystem = p.unitSystem, goal = p.goal, createdAt = p.createdAt,
                    weeklyTarget = p.weeklyTarget, weighInTime = p.weighInTime, weighInDays = p.weighInDays,
                    weighInEnabled = p.weighInEnabled, cardioCountsTowardTarget = p.cardioCountsTowardTarget, barWeightKg = p.barWeightKg,
                )
            )
            file.customExercises.forEach { e ->
                val exId = exercises.findOrCreate(id, e.name, e.muscleGroup, e.equipment)
                e.guide?.let { g -> db.guideDao().upsert(ExerciseGuide(exId, g.primaryMuscles, g.secondaryMuscles, g.steps, g.mistakes, g.videoQuery)) }
            }
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
                // One by one, so drop sets can point at their main set's new id.
                val newIds = mutableListOf<Long>()
                s.sets.forEach {
                    val exId = cache.getOrPut(it.exercise.lowercase()) { exercises.findOrCreate(id, it.exercise, "other", "") }
                    newIds += db.workoutDao().insertSet(
                        SetLog(
                            sessionId = sid, exerciseId = exId, setNumber = it.setNumber, weightKg = it.weightKg, reps = it.reps,
                            completed = it.completed, exerciseOrder = it.order, setType = it.setType,
                            parentSetId = it.parent?.let { p -> newIds.getOrNull(p) },
                        )
                    )
                }
            }
            db.trackDao().insertCardio(file.cardio.map {
                CardioSession(profileId = id, date = it.date, machine = it.machine, customName = it.customName, minutes = it.minutes,
                    kcal = it.kcal, distanceKm = it.distanceKm, avgHeartRate = it.avgHeartRate, note = it.note)
            })
            db.trackDao().insertMeasurements(file.measurements.map {
                BodyMeasurement(profileId = id, date = it.date, waistCm = it.waistCm, armCm = it.armCm, chestCm = it.chestCm, thighCm = it.thighCm)
            })
            id
        }
        prefs.setCurrentProfile(profileId)
        return profileId
    }
}
