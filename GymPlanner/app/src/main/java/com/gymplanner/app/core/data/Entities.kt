package com.gymplanner.app.core.data

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
)

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
)
