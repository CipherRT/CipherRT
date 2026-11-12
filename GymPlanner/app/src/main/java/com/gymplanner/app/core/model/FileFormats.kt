package com.gymplanner.app.core.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON formats shared between phones. Every file carries `formatVersion` so a
 * future version of the app can still read files made by this one.
 * Exercises are referenced by name, so plans work on any phone.
 */
/** v2 adds day frequency, supersets, drop sets and plan descriptions. Files of version 1 still load. */
const val CURRENT_FORMAT_VERSION = 2

@OptIn(ExperimentalSerializationApi::class)
val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
    prettyPrintIndent = "  "
    explicitNulls = false
}

@Serializable
data class GuideJson(
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val mistakes: List<String> = emptyList(),
    val videoQuery: String = "",
)

@Serializable
data class ExerciseJson(
    val name: String,
    val muscleGroup: String = "other",
    val equipment: String = "",
    val guide: GuideJson? = null,
)

@Serializable
data class ExerciseLibraryFile(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    /** Bumped whenever the built-in library changes, so existing installs pick it up. */
    val libraryVersion: Int = 1,
    val exercises: List<ExerciseJson>,
)

@Serializable
data class PlanExerciseJson(
    val name: String,
    val muscleGroup: String = "other",
    val equipment: String = "",
    val sets: Int = 3,
    val repMin: Int = 8,
    val repMax: Int = 12,
    val toFailure: Boolean = false,
    val perSide: Boolean = false,
    val restSeconds: Int = 90,
    val note: String = "",
    val isOptional: Boolean = false,
    val isWarmup: Boolean = false,
    val supersetGroup: Int? = null,
    val dropSets: Int = 0,
)

@Serializable
data class PlanDayJson(
    val name: String,
    val note: String = "",
    val weekday: Int? = null,
    /** "weekly" or "biweekly". */
    val frequency: String = "weekly",
    val exercises: List<PlanExerciseJson> = emptyList(),
)

@Serializable
data class PlanJson(
    val name: String,
    val description: String = "",
    val days: List<PlanDayJson> = emptyList(),
)

/** A `.gymplan.json` file: a plan with no weights, logs or personal info. */
@Serializable
data class PlanFile(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val type: String = "plan",
    val plan: PlanJson,
)

// ---- Full backup (private; for moving your own data to a new phone) ----

@Serializable
data class ProfileJson(
    val name: String,
    val heightCm: Double,
    val unitSystem: String,
    val goal: String,
    val createdAt: Long,
)

@Serializable
data class WeightJson(val date: Long, val weightKg: Double, val note: String = "")

@Serializable
data class SetLogJson(
    val exercise: String,
    val setNumber: Int,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val completed: Boolean = false,
    val order: Int = 0,
)

@Serializable
data class SessionJson(
    val dayName: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val note: String = "",
    val isLight: Boolean = false,
    val sets: List<SetLogJson> = emptyList(),
)

@Serializable
data class BackupPlanJson(val isActive: Boolean, val plan: PlanJson)

@Serializable
data class BackupFile(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val type: String = "backup",
    val exportedAt: Long,
    val profile: ProfileJson,
    val customExercises: List<ExerciseJson> = emptyList(),
    val weights: List<WeightJson> = emptyList(),
    val plans: List<BackupPlanJson> = emptyList(),
    val sessions: List<SessionJson> = emptyList(),
)
