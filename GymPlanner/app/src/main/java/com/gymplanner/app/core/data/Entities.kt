package com.gymplanner.app.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// All weights in kg, heights in cm. Dates are epoch days (LocalDate.toEpochDay()),
// timestamps are epoch milliseconds.

@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val heightCm: Double,
    val unitSystem: String,
    val goal: String,
    val createdAt: Long,
    // v2 ↓ (defaults must match Migrations.MIGRATION_1_2)
    /** Gym sessions per week that count as "hit" for the streak. */
    @ColumnInfo(defaultValue = "4") val weeklyTarget: Int = 4,
    /** Morning weigh-in prompt time, minutes after midnight (420 = 07:00). */
    @ColumnInfo(defaultValue = "420") val weighInTime: Int = 420,
    /** Bitmask of weekdays for the prompt: bit 0 = Monday … bit 6 = Sunday. 127 = every day. */
    @ColumnInfo(defaultValue = "127") val weighInDays: Int = 127,
    @ColumnInfo(defaultValue = "1") val weighInEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "0") val cardioCountsTowardTarget: Boolean = false,
    /** Barbell weight for the plate calculator. */
    @ColumnInfo(defaultValue = "20.0") val barWeightKg: Double = 20.0,
)

@Entity(
    tableName = "body_weight",
    indices = [Index("profileId", "date")],
    foreignKeys = [ForeignKey(Profile::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
)
data class BodyWeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val date: Long,
    val weightKg: Double,
    val note: String = "",
)

/** Built-in exercises have profileId = null; custom ones belong to a profile. */
@Entity(tableName = "exercise", indices = [Index("profileId"), Index("name")])
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val isCustom: Boolean,
    val profileId: Long?,
)

@Entity(
    tableName = "plan",
    indices = [Index("profileId")],
    foreignKeys = [ForeignKey(Profile::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
)
data class Plan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,
    val isActive: Boolean,
    val daysPerWeek: Int,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "") val description: String = "",
)

@Entity(
    tableName = "plan_day",
    indices = [Index("planId")],
    foreignKeys = [ForeignKey(Plan::class, ["id"], ["planId"], onDelete = ForeignKey.CASCADE)],
)
data class PlanDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val name: String,
    val orderIndex: Int,
    /** 1 = Monday … 7 = Sunday, or null if not pinned. */
    val weekday: Int? = null,
    val note: String = "",
    /** "weekly" or "biweekly" (every 2 weeks). */
    @ColumnInfo(defaultValue = "weekly") val frequency: String = DayFrequency.WEEKLY,
)

object DayFrequency {
    const val WEEKLY = "weekly"
    const val BIWEEKLY = "biweekly"
}

@Entity(
    tableName = "plan_exercise",
    indices = [Index("planDayId"), Index("exerciseId")],
    foreignKeys = [
        ForeignKey(PlanDay::class, ["id"], ["planDayId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Exercise::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class PlanExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planDayId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val toFailure: Boolean,
    val restSeconds: Int,
    val note: String = "",
    val isOptional: Boolean = false,
    /** Reps are counted per arm / per leg (e.g. single-arm row). */
    val perSide: Boolean = false,
    /** Part of the warm-up block at the start of the day. */
    val isWarmup: Boolean = false,
    /** Exercises on a day with the same group number are done as a superset. */
    @ColumnInfo(defaultValue = "NULL") val supersetGroup: Int? = null,
    /** Drop sets after the last working set (0–3). */
    @ColumnInfo(defaultValue = "0") val dropSets: Int = 0,
)

/**
 * planDayId is a plain column (no foreign key) and dayName is copied in, so deleting
 * a plan never deletes workout history.
 */
@Entity(tableName = "workout_session", indices = [Index("profileId"), Index("planDayId")])
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val planDayId: Long?,
    val dayName: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val note: String = "",
    val isLight: Boolean = false,
)

@Entity(
    tableName = "set_log",
    indices = [Index("sessionId"), Index("exerciseId")],
    foreignKeys = [ForeignKey(WorkoutSession::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE)],
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val setNumber: Int,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val completed: Boolean = false,
    /** Position of the exercise within the session. */
    val exerciseOrder: Int = 0,
    /** Plan settings copied in when the session started, for display. */
    val planExerciseId: Long? = null,
    /** normal / warmup / failure / drop — see [SetType]. */
    @ColumnInfo(defaultValue = "normal") val setType: String = SetType.NORMAL,
    /** For a drop set: the main set it follows. */
    @ColumnInfo(defaultValue = "NULL") val parentSetId: Long? = null,
)

object SetType {
    const val NORMAL = "normal"
    const val WARMUP = "warmup"
    const val FAILURE = "failure"
    const val DROP = "drop"
}

// ---- v2 tables ----

@Entity(
    tableName = "body_measurement",
    indices = [Index("profileId", "date")],
    foreignKeys = [ForeignKey(Profile::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
)
data class BodyMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val date: Long,
    val waistCm: Double? = null,
    val armCm: Double? = null,
    val chestCm: Double? = null,
    val thighCm: Double? = null,
)

/** How to do an exercise: muscles for the body map, steps, mistakes, a video search. */
@Entity(
    tableName = "exercise_guide",
    foreignKeys = [ForeignKey(Exercise::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE)],
)
data class ExerciseGuide(
    @PrimaryKey val exerciseId: Long,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val mistakes: List<String> = emptyList(),
    val videoQuery: String = "",
)

@Entity(
    tableName = "cardio_session",
    indices = [Index("profileId", "date")],
    foreignKeys = [ForeignKey(Profile::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
)
data class CardioSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    /** Epoch day. */
    val date: Long,
    /** bike / elliptical / treadmill / stair / rower / walk / custom — see [CardioMachine]. */
    val machine: String,
    val customName: String = "",
    val minutes: Int,
    val kcal: Int,
    val distanceKm: Double? = null,
    val avgHeartRate: Int? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

enum class CardioMachine(val key: String, val label: String) {
    BIKE("bike", "Bike"),
    ELLIPTICAL("elliptical", "Elliptical"),
    TREADMILL("treadmill", "Treadmill"),
    STAIR("stair", "Stair climber"),
    ROWER("rower", "Rower"),
    WALK("walk", "Walk"),
    CUSTOM("custom", "Custom");

    companion object {
        fun from(key: String) = entries.firstOrNull { it.key == key } ?: CUSTOM
    }
}

fun CardioSession.displayName(): String =
    if (machine == CardioMachine.CUSTOM.key) customName.ifBlank { "Cardio" } else CardioMachine.from(machine).label
