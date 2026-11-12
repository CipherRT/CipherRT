package com.gymplanner.app.screens

import androidx.test.core.app.ApplicationProvider
import com.gymplanner.app.AppContainer
import com.gymplanner.app.GymApp
import com.gymplanner.app.core.data.CardioSession
import com.gymplanner.app.core.data.PlanRepository
import com.gymplanner.app.core.data.SetLog
import com.gymplanner.app.core.data.WorkoutSession
import com.gymplanner.app.core.model.Goal
import com.gymplanner.app.core.model.UnitSystem
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId

/** Realistic sample data for screenshots: profile, the 4-Day Split, weights, workouts, cardio. */
object Seed {
    fun app(): GymApp = ApplicationProvider.getApplicationContext()

    fun basic(trainedToday: Boolean = false): AppContainer = runBlocking {
        val app = app()
        val c = app.container
        while (!app.ready.value) Thread.sleep(20)
        val pid = c.profiles.create("Alex", 180.0, 78.4, UnitSystem.METRIC, Goal.BULK, makeCurrent = false)
        val text = app.assets.open("templates/default.gymplan.json").bufferedReader().readText()
        c.plans.importPlan(pid, PlanRepository.parsePlanFile(text).plan, makeActive = true)
        val today = LocalDate.now()
        listOf(78.9, 78.6, 78.7, 78.3, 78.4, 78.1, 78.2, 78.0, 77.9, 78.1, 77.8).forEachIndexed { i, w ->
            c.weights.add(pid, today.minusDays(10L - i).toEpochDay(), w)
        }
        val plan = c.db.planDao().plans(pid).first()
        val days = c.plans.getDays(plan.id)
        // Two weeks of training: Back, Chest, Shoulders & Arms, Leg, Back, Chest
        val schedule = listOf(13L to 0, 11L to 1, 9L to 2, 8L to 3, 4L to 0, 2L to 1) + if (trainedToday) listOf(0L to 2) else emptyList()
        schedule.forEach { (ago, idx) ->
            val day = days[idx]
            val start = today.minusDays(ago).atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val sid = c.db.workoutDao().insertSession(
                WorkoutSession(profileId = pid, planDayId = day.id, dayName = day.name, startedAt = start, finishedAt = start + 55 * 60_000)
            )
            val items = c.plans.getDayExercises(day.id)
            c.db.workoutDao().insertSets(items.flatMapIndexed { order, pe ->
                (1..pe.sets).map { n ->
                    SetLog(sessionId = sid, exerciseId = pe.exerciseId, setNumber = n, weightKg = 50.0 + order * 5 - n * 2.5 + (13 - ago) * 0.5,
                        reps = 8 + (n % 3), completed = true, exerciseOrder = order, planExerciseId = pe.id)
                }
            })
        }
        c.track.addCardio(CardioSession(profileId = pid, date = today.minusDays(3).toEpochDay(), machine = "bike", minutes = 25, kcal = 210))
        c.track.addCardio(CardioSession(profileId = pid, date = today.minusDays(1).toEpochDay(), machine = "treadmill", minutes = 30, kcal = 260))
        c.profiles.switchTo(pid)
        c
    }
}
