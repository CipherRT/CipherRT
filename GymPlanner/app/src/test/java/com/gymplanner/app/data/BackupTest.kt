package com.gymplanner.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymplanner.app.core.data.BodyMeasurement
import com.gymplanner.app.core.data.SetType
import com.gymplanner.app.screens.Seed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class BackupTest {
    @Test
    fun v2BackupRoundTrip() = runBlocking {
        val c = Seed.basic()
        val pid = c.prefs.currentProfileId.value!!
        c.profiles.update(c.profiles.get(pid)!!.copy(weeklyTarget = 3, weighInTime = 390))
        c.track.addMeasurement(BodyMeasurement(profileId = pid, date = 20000, waistCm = 82.0))
        // A finished session with a drop set
        val day = c.plans.getDays(c.db.planDao().plans(pid).first().id).first()
        val sid = c.workouts.start(pid, day, isLight = false)
        val main = c.workouts.getSets(sid).first { it.exerciseOrder == 1 }
        c.workouts.addDropSet(main)
        c.workouts.getSets(sid).forEach { c.workouts.updateSet(it.copy(weightKg = 40.0, reps = 8, completed = true)) }
        c.workouts.finish(sid)

        val text = c.backup.export(pid)
        assertTrue(text.contains("\"formatVersion\": 2"))
        val newId = c.backup.restore(text)
        assertNotEquals(pid, newId)

        val p = c.profiles.get(newId)!!
        assertEquals(3, p.weeklyTarget); assertEquals(390, p.weighInTime)
        assertEquals(c.weights.entries(pid).first().size, c.weights.entries(newId).first().size)
        assertEquals(2, c.track.cardio(newId).first().size)
        assertEquals(82.0, c.track.measurements(newId).first().single().waistCm!!, 0.0)
        val oldSessions = c.workouts.finishedSessions(pid).first()
        val newSessions = c.workouts.finishedSessions(newId).first()
        assertEquals(oldSessions.size, newSessions.size)
        val restoredDropSession = newSessions.first { it.startedAt == oldSessions.maxOf { s -> s.startedAt } }
        val sets = c.workouts.getSets(restoredDropSession.id)
        val drop = sets.single { it.setType == SetType.DROP }
        assertEquals(sets.first { it.exerciseOrder == 1 && it.setType != SetType.DROP && it.setNumber == main.setNumber }.id, drop.parentSetId)
        assertEquals(c.plans.getDays(c.db.planDao().plans(newId).first().id).map { it.frequency },
            c.plans.getDays(c.db.planDao().plans(pid).first().id).map { it.frequency })
    }

    @Test
    fun v1BackupStillRestores() = runBlocking {
        val c = Seed.basic()
        val v1 = """
            {"formatVersion": 1, "type": "backup", "exportedAt": 1,
             "profile": {"name": "Old", "heightCm": 170.0, "unitSystem": "IMPERIAL", "goal": "CUT", "createdAt": 1},
             "weights": [{"date": 20000, "weightKg": 70.0}],
             "plans": [], "sessions": [{"dayName": "Back", "startedAt": 100, "finishedAt": 200,
               "sets": [{"exercise": "Pull-ups", "setNumber": 1, "weightKg": 0.0, "reps": 10, "completed": true}]}]}
        """.trimIndent()
        val id = c.backup.restore(v1)
        val p = c.profiles.get(id)!!
        assertEquals("Old", p.name); assertEquals(4, p.weeklyTarget)
        assertEquals(1, c.workouts.finishedSessions(id).first().size)
        assertEquals(SetType.NORMAL, c.workouts.getSets(c.workouts.finishedSessions(id).first().single().id).single().setType)
    }
}
