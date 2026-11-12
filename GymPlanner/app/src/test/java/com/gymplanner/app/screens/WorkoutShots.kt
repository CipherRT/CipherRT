package com.gymplanner.app.screens

import com.gymplanner.app.feature.today.WorkoutScreen
import kotlinx.coroutines.runBlocking
import org.junit.Test

class WorkoutShots : ScreenshotBase() {
    @Test fun workout() {
        val c = Seed.basic()
        val sid = runBlocking {
            val pid = c.prefs.currentProfileId.value!!
            val plan = c.db.planDao().plans(pid).first()
            val back = c.plans.getDays(plan.id).first()
            val id = c.workouts.start(pid, back, isLight = false)
            val sets = c.workouts.getSets(id)
            sets.filter { it.exerciseOrder == 0 }.forEach { c.workouts.updateSet(it.copy(weightKg = 0.0, reps = 10, completed = true)) }
            sets.first { it.exerciseOrder == 1 && it.setNumber == 1 }.let { c.workouts.updateSet(it.copy(weightKg = 55.0, reps = 8, completed = true)) }
            sets.first { it.exerciseOrder == 1 && it.setNumber == 2 }.let { c.workouts.updateSet(it.copy(weightKg = 55.0)) }
            c.workouts.addDropSet(sets.first { it.exerciseOrder == 1 && it.setNumber == 3 })
            id
        }
        snap("workout") { WorkoutScreen(sid, {}, {}, {}) }
    }
}
