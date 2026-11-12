package com.gymplanner.app.core.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

/** What was done last time for one exercise, plus the best weight × reps ever. */
data class ExerciseMemory(val lastSets: List<SetLog>, val bestScore: Double?)

class WorkoutRepository(private val db: AppDatabase) {
    private val dao = db.workoutDao()
    private val planDao = db.planDao()

    fun unfinished(profileId: Long): Flow<WorkoutSession?> = dao.observeUnfinished(profileId)
    fun lastFinished(profileId: Long): Flow<WorkoutSession?> = dao.observeLastFinished(profileId)
    fun finishedSessions(profileId: Long): Flow<List<WorkoutSession>> = dao.observeFinished(profileId)
    fun session(id: Long): Flow<WorkoutSession?> = dao.observeSession(id)
    fun sets(sessionId: Long): Flow<List<SetLog>> = dao.observeSets(sessionId)
    fun exerciseHistory(profileId: Long, exerciseId: Long) = dao.observeExerciseHistory(profileId, exerciseId)
    fun exerciseSummaries(profileId: Long) = dao.observeExerciseSummaries(profileId)

    fun completedSetsThisWeek(profileId: Long): Flow<List<SetLog>> {
        val monday = LocalDate.now().let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }
        val from = monday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return dao.observeCompletedSetsSince(profileId, from)
    }

    /**
     * Starts a session for a plan day. On a light day, optional exercises are left out
     * and every remaining exercise gets one set fewer (minimum one).
     */
    suspend fun start(profileId: Long, day: PlanDay, isLight: Boolean): Long = db.withTransaction {
        val sessionId = dao.insertSession(
            WorkoutSession(
                profileId = profileId, planDayId = day.id, dayName = day.name,
                startedAt = System.currentTimeMillis(), isLight = isLight,
            )
        )
        val items = planDao.dayExercises(day.id).filter { !(isLight && it.isOptional) }
        val sets = items.flatMapIndexed { order, pe ->
            val count = if (isLight && !pe.isWarmup) (pe.sets - 1).coerceAtLeast(1) else pe.sets
            (1..count).map { n ->
                SetLog(
                    sessionId = sessionId, exerciseId = pe.exerciseId, setNumber = n, exerciseOrder = order, planExerciseId = pe.id,
                    setType = when {
                        pe.isWarmup -> SetType.WARMUP
                        pe.toFailure -> SetType.FAILURE
                        else -> SetType.NORMAL
                    },
                )
            }
        }
        dao.insertSets(sets)
        sessionId
    }

    suspend fun memory(profileId: Long, exerciseId: Long, currentSessionId: Long) = ExerciseMemory(
        lastSets = dao.lastSetsFor(profileId, exerciseId, currentSessionId),
        bestScore = dao.bestScore(profileId, exerciseId, currentSessionId),
    )

    suspend fun updateSet(set: SetLog) = dao.updateSet(set)

    suspend fun addSet(after: SetLog) {
        val existing = dao.sets(after.sessionId).filter { it.exerciseOrder == after.exerciseOrder }
        dao.insertSet(
            after.copy(id = 0, setNumber = (existing.maxOfOrNull { it.setNumber } ?: 0) + 1, completed = false)
        )
    }

    suspend fun removeSet(set: SetLog) = dao.deleteSet(set)

    /** "Skip" an exercise for this session. */
    suspend fun skipExercise(sessionId: Long, exerciseOrder: Int) = db.withTransaction {
        dao.sets(sessionId).filter { it.exerciseOrder == exerciseOrder }.forEach { dao.deleteSet(it) }
    }

    suspend fun updateSession(session: WorkoutSession) = dao.updateSession(session)

    /** Saves the session. Sets that were never ticked are dropped from history. */
    suspend fun finish(sessionId: Long) = db.withTransaction {
        val session = dao.getSession(sessionId) ?: return@withTransaction
        dao.sets(sessionId).filter { !it.completed }.forEach { dao.deleteSet(it) }
        dao.updateSession(session.copy(finishedAt = System.currentTimeMillis()))
    }

    suspend fun discard(sessionId: Long) {
        dao.getSession(sessionId)?.let { dao.deleteSession(it) }
    }

    suspend fun getSession(id: Long) = dao.getSession(id)
    suspend fun getSets(sessionId: Long) = dao.sets(sessionId)
}
