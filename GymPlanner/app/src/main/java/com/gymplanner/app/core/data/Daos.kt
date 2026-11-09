package com.gymplanner.app.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile ORDER BY createdAt")
    fun observeAll(): Flow<List<Profile>>

    @Query("SELECT * FROM profile WHERE id = :id")
    fun observe(id: Long): Flow<Profile?>

    @Query("SELECT * FROM profile WHERE id = :id")
    suspend fun get(id: Long): Profile?

    @Query("SELECT * FROM profile ORDER BY createdAt LIMIT 1")
    suspend fun first(): Profile?

    @Insert suspend fun insert(profile: Profile): Long
    @Update suspend fun update(profile: Profile)
    @Delete suspend fun delete(profile: Profile)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM body_weight WHERE profileId = :profileId ORDER BY date DESC, id DESC")
    fun observe(profileId: Long): Flow<List<BodyWeightEntry>>

    @Query("SELECT * FROM body_weight WHERE profileId = :profileId ORDER BY date")
    suspend fun all(profileId: Long): List<BodyWeightEntry>

    @Query("SELECT * FROM body_weight WHERE profileId = :profileId ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun latest(profileId: Long): BodyWeightEntry?

    @Query("SELECT * FROM body_weight WHERE id = :id")
    suspend fun get(id: Long): BodyWeightEntry?

    @Insert suspend fun insert(entry: BodyWeightEntry): Long
    @Insert suspend fun insertAll(entries: List<BodyWeightEntry>)
    @Update suspend fun update(entry: BodyWeightEntry)
    @Delete suspend fun delete(entry: BodyWeightEntry)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise WHERE profileId IS NULL OR profileId = :profileId ORDER BY name COLLATE NOCASE")
    fun observeLibrary(profileId: Long): Flow<List<Exercise>>

    @Query("SELECT * FROM exercise WHERE profileId IS NULL OR profileId = :profileId")
    suspend fun library(profileId: Long): List<Exercise>

    @Query("SELECT * FROM exercise WHERE profileId = :profileId AND isCustom = 1")
    suspend fun custom(profileId: Long): List<Exercise>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun get(id: Long): Exercise?

    @Query("SELECT * FROM exercise WHERE id IN (:ids)")
    suspend fun getAll(ids: List<Long>): List<Exercise>

    @Query("SELECT COUNT(*) FROM exercise WHERE isCustom = 0")
    suspend fun builtInCount(): Int

    @Insert suspend fun insert(exercise: Exercise): Long
    @Insert suspend fun insertAll(exercises: List<Exercise>)

    @Query("DELETE FROM exercise WHERE profileId = :profileId AND isCustom = 1")
    suspend fun deleteCustom(profileId: Long)
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plan WHERE profileId = :profileId ORDER BY isActive DESC, createdAt DESC")
    fun observePlans(profileId: Long): Flow<List<Plan>>

    @Query("SELECT * FROM plan WHERE profileId = :profileId AND isActive = 1 LIMIT 1")
    fun observeActive(profileId: Long): Flow<Plan?>

    @Query("SELECT * FROM plan WHERE profileId = :profileId")
    suspend fun plans(profileId: Long): List<Plan>

    @Query("SELECT * FROM plan WHERE id = :id")
    fun observePlan(id: Long): Flow<Plan?>

    @Query("SELECT * FROM plan WHERE id = :id")
    suspend fun getPlan(id: Long): Plan?

    @Insert suspend fun insertPlan(plan: Plan): Long
    @Update suspend fun updatePlan(plan: Plan)
    @Delete suspend fun deletePlan(plan: Plan)

    @Query("UPDATE plan SET isActive = (id = :planId) WHERE profileId = :profileId")
    suspend fun setActive(profileId: Long, planId: Long)

    // Days
    @Query("SELECT * FROM plan_day WHERE planId = :planId ORDER BY orderIndex")
    fun observeDays(planId: Long): Flow<List<PlanDay>>

    @Query("SELECT * FROM plan_day WHERE planId = :planId ORDER BY orderIndex")
    suspend fun days(planId: Long): List<PlanDay>

    @Query("SELECT * FROM plan_day WHERE id = :id")
    fun observeDay(id: Long): Flow<PlanDay?>

    @Query("SELECT * FROM plan_day WHERE id = :id")
    suspend fun getDay(id: Long): PlanDay?

    @Insert suspend fun insertDay(day: PlanDay): Long
    @Update suspend fun updateDay(day: PlanDay)
    @Update suspend fun updateDays(days: List<PlanDay>)
    @Delete suspend fun deleteDay(day: PlanDay)

    @Query("SELECT COUNT(*) FROM plan_day WHERE planId = :planId")
    suspend fun dayCount(planId: Long): Int

    // Exercises in a day
    @Query("SELECT * FROM plan_exercise WHERE planDayId = :dayId ORDER BY orderIndex")
    fun observeDayExercises(dayId: Long): Flow<List<PlanExercise>>

    @Query("SELECT * FROM plan_exercise WHERE planDayId = :dayId ORDER BY orderIndex")
    suspend fun dayExercises(dayId: Long): List<PlanExercise>

    @Query(
        "SELECT pe.* FROM plan_exercise pe JOIN plan_day d ON pe.planDayId = d.id " +
            "WHERE d.planId = :planId ORDER BY d.orderIndex, pe.orderIndex"
    )
    fun observePlanExercises(planId: Long): Flow<List<PlanExercise>>

    @Query("SELECT * FROM plan_exercise WHERE id = :id")
    suspend fun getPlanExercise(id: Long): PlanExercise?

    @Query("SELECT * FROM plan_exercise WHERE id IN (:ids)")
    suspend fun getPlanExercises(ids: List<Long>): List<PlanExercise>

    @Query("SELECT * FROM plan_exercise WHERE id = :id")
    fun observePlanExercise(id: Long): Flow<PlanExercise?>

    @Insert suspend fun insertPlanExercise(pe: PlanExercise): Long
    @Update suspend fun updatePlanExercise(pe: PlanExercise)
    @Update suspend fun updatePlanExercises(list: List<PlanExercise>)
    @Delete suspend fun deletePlanExercise(pe: PlanExercise)
}

data class SetWithSession(
    val sessionId: Long,
    val startedAt: Long,
    val setNumber: Int,
    val weightKg: Double?,
    val reps: Int?,
)

data class ExerciseSummary(
    val exerciseId: Long,
    val name: String,
    val muscleGroup: String,
    val sessions: Int,
    val lastDone: Long,
)

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_session WHERE profileId = :profileId AND finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeUnfinished(profileId: Long): Flow<WorkoutSession?>

    @Query("SELECT * FROM workout_session WHERE profileId = :profileId AND finishedAt IS NOT NULL ORDER BY finishedAt DESC LIMIT 1")
    fun observeLastFinished(profileId: Long): Flow<WorkoutSession?>

    @Query("SELECT * FROM workout_session WHERE profileId = :profileId AND finishedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeFinished(profileId: Long): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_session WHERE profileId = :profileId ORDER BY startedAt")
    suspend fun allSessions(profileId: Long): List<WorkoutSession>

    @Query("SELECT * FROM workout_session WHERE id = :id")
    fun observeSession(id: Long): Flow<WorkoutSession?>

    @Query("SELECT * FROM workout_session WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSession?

    @Insert suspend fun insertSession(session: WorkoutSession): Long
    @Update suspend fun updateSession(session: WorkoutSession)
    @Delete suspend fun deleteSession(session: WorkoutSession)

    @Query("SELECT * FROM set_log WHERE sessionId = :sessionId ORDER BY exerciseOrder, setNumber")
    fun observeSets(sessionId: Long): Flow<List<SetLog>>

    @Query("SELECT * FROM set_log WHERE sessionId = :sessionId ORDER BY exerciseOrder, setNumber")
    suspend fun sets(sessionId: Long): List<SetLog>

    @Query("SELECT * FROM set_log WHERE sessionId IN (:sessionIds)")
    suspend fun setsForSessions(sessionIds: List<Long>): List<SetLog>

    @Insert suspend fun insertSets(sets: List<SetLog>)
    @Insert suspend fun insertSet(set: SetLog): Long
    @Update suspend fun updateSet(set: SetLog)
    @Delete suspend fun deleteSet(set: SetLog)

    /** Sets from the most recent finished session that included this exercise. */
    @Query(
        """
        SELECT sl.* FROM set_log sl
        WHERE sl.exerciseId = :exerciseId AND sl.sessionId = (
            SELECT s.id FROM workout_session s JOIN set_log x ON x.sessionId = s.id
            WHERE s.profileId = :profileId AND s.finishedAt IS NOT NULL AND s.id != :excludeSessionId
              AND x.exerciseId = :exerciseId AND x.completed = 1
            ORDER BY s.startedAt DESC LIMIT 1
        )
        ORDER BY sl.setNumber
        """
    )
    suspend fun lastSetsFor(profileId: Long, exerciseId: Long, excludeSessionId: Long): List<SetLog>

    /** Best weight × reps for an exercise, from finished sessions only. */
    @Query(
        """
        SELECT MAX(sl.weightKg * sl.reps) FROM set_log sl JOIN workout_session s ON sl.sessionId = s.id
        WHERE s.profileId = :profileId AND sl.exerciseId = :exerciseId AND sl.completed = 1
          AND s.finishedAt IS NOT NULL AND s.id != :excludeSessionId
        """
    )
    suspend fun bestScore(profileId: Long, exerciseId: Long, excludeSessionId: Long): Double?

    @Query(
        """
        SELECT s.id AS sessionId, s.startedAt AS startedAt, sl.setNumber AS setNumber,
               sl.weightKg AS weightKg, sl.reps AS reps
        FROM set_log sl JOIN workout_session s ON sl.sessionId = s.id
        WHERE s.profileId = :profileId AND sl.exerciseId = :exerciseId AND sl.completed = 1
          AND s.finishedAt IS NOT NULL
        ORDER BY s.startedAt DESC, sl.setNumber
        """
    )
    fun observeExerciseHistory(profileId: Long, exerciseId: Long): Flow<List<SetWithSession>>

    @Query(
        """
        SELECT e.id AS exerciseId, e.name AS name, e.muscleGroup AS muscleGroup,
               COUNT(DISTINCT s.id) AS sessions, MAX(s.startedAt) AS lastDone
        FROM set_log sl
        JOIN workout_session s ON sl.sessionId = s.id
        JOIN exercise e ON e.id = sl.exerciseId
        WHERE s.profileId = :profileId AND sl.completed = 1 AND s.finishedAt IS NOT NULL
        GROUP BY e.id ORDER BY lastDone DESC
        """
    )
    fun observeExerciseSummaries(profileId: Long): Flow<List<ExerciseSummary>>

    @Query(
        """
        SELECT sl.* FROM set_log sl JOIN workout_session s ON sl.sessionId = s.id
        WHERE s.profileId = :profileId AND s.finishedAt IS NOT NULL AND sl.completed = 1
          AND s.startedAt >= :fromMillis
        """
    )
    fun observeCompletedSetsSince(profileId: Long, fromMillis: Long): Flow<List<SetLog>>

    @Transaction
    suspend fun replaceSessionSets(sessionId: Long, sets: List<SetLog>) {
        sets(sessionId).forEach { deleteSet(it) }
        insertSets(sets)
    }
}
