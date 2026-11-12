package com.gymplanner.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymplanner.app.core.data.AppDatabase
import com.gymplanner.app.core.data.Exercise
import com.gymplanner.app.core.data.Plan
import com.gymplanner.app.core.data.PlanDay
import com.gymplanner.app.core.data.PlanExercise
import com.gymplanner.app.core.data.Profile
import com.gymplanner.app.core.data.SetLog
import com.gymplanner.app.core.data.SetType
import com.gymplanner.app.core.data.WorkoutRepository
import com.gymplanner.app.feature.today.ExerciseBlock
import com.gymplanner.app.feature.today.SetRow
import com.gymplanner.app.feature.today.WorkoutPage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WorkoutFlowTest {
    @Test
    fun dropSetsLogSeparatelyAndShowInHistory() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        val pid = db.profileDao().insert(Profile(name = "A", heightCm = 180.0, unitSystem = "METRIC", goal = "BULK", createdAt = 0))
        val ex = db.exerciseDao().insert(Exercise(name = "Pec deck", muscleGroup = "chest", equipment = "machine", isCustom = false, profileId = null))
        val warm = db.exerciseDao().insert(Exercise(name = "Pull-ups", muscleGroup = "back", equipment = "bodyweight", isCustom = false, profileId = null))
        val planId = db.planDao().insertPlan(Plan(profileId = pid, name = "P", isActive = true, daysPerWeek = 1, createdAt = 0))
        val dayId = db.planDao().insertDay(PlanDay(planId = planId, name = "Chest", orderIndex = 0))
        db.planDao().insertPlanExercise(PlanExercise(planDayId = dayId, exerciseId = warm, orderIndex = 0, sets = 1, repMin = 8, repMax = 12, toFailure = false, restSeconds = 60, isWarmup = true))
        db.planDao().insertPlanExercise(PlanExercise(planDayId = dayId, exerciseId = ex, orderIndex = 1, sets = 3, repMin = 8, repMax = 12, toFailure = true, restSeconds = 90, dropSets = 1))
        val repo = WorkoutRepository(db)

        val sid = repo.start(pid, db.planDao().getDay(dayId)!!, isLight = false)
        val sets = repo.getSets(sid)
        assertEquals(SetType.WARMUP, sets.first { it.exerciseOrder == 0 }.setType)
        val pec = sets.filter { it.exerciseOrder == 1 }
        assertEquals(listOf(SetType.FAILURE, SetType.FAILURE, SetType.FAILURE, SetType.DROP), pec.map { it.setType })
        val drop = pec.last()
        assertEquals(pec.first { it.setNumber == 3 && it.setType != SetType.DROP }.id, drop.parentSetId)

        // Log everything, then finish.
        sets.forEach { repo.updateSet(it.copy(weightKg = if (it.setType == SetType.DROP) 40.0 else 50.0, reps = 10, completed = true)) }
        repo.finish(sid)

        val history = db.workoutDao().observeExerciseHistory(pid, ex).first()
        assertEquals(4, history.size)
        assertEquals(1, history.count { it.setType == SetType.DROP && it.weightKg == 40.0 })
        // Warm-ups don't count toward weekly set totals.
        val week = db.workoutDao().observeCompletedSetsSince(pid, 0).first()
        assertTrue(week.none { it.setType == SetType.WARMUP })
        assertEquals(4, week.size)
        db.close()
    }

    @Test
    fun supersetAlternatesSetBySet() {
        fun row(id: Long, n: Int, done: Boolean = false) =
            SetRow(SetLog(id = id, sessionId = 1, exerciseId = 1, setNumber = n, completed = done), null, false, null, null)
        val a = ExerciseBlock(0, null, null, listOf(row(1, 1, done = true), row(2, 2), row(3, 3)))
        val b = ExerciseBlock(1, null, null, listOf(row(4, 1), row(5, 2), row(6, 3)))
        val page = WorkoutPage(listOf(a, b))
        assertEquals(listOf(1L, 4L, 2L, 5L, 3L, 6L), page.ordered.map { it.second.set.id })
        assertEquals(4L, page.active?.second?.set?.id)
    }
}
