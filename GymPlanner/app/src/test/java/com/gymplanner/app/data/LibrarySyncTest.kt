package com.gymplanner.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymplanner.app.core.data.AppDatabase
import com.gymplanner.app.core.data.Exercise
import com.gymplanner.app.core.data.ExerciseRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Upgrading from v1's 76-exercise library keeps every existing exercise id. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LibrarySyncTest {
    @Test
    fun syncKeepsIdsAndAddsGuides() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        val dao = db.exerciseDao()
        val thrustId = dao.insert(Exercise(name = "Hip thrust", muscleGroup = "legs", equipment = "barbell", isCustom = false, profileId = null))
        val customId = dao.insert(Exercise(name = "My move", muscleGroup = "core", equipment = "", isCustom = true, profileId = 1))

        var applied = 0
        ExerciseRepository(ctx, db).syncLibrary(0) { applied = it }

        assertEquals(2, applied)
        val thrust = dao.get(thrustId)!!
        assertEquals("glutes", thrust.muscleGroup)
        assertEquals(1, dao.builtIns().count { it.name == "Hip thrust" })
        assertTrue(dao.builtInCount() in 140..170)
        assertTrue(db.guideDao().get(thrustId)!!.steps.isNotEmpty())
        assertEquals("My move", dao.get(customId)!!.name)

        // Running again with the same version changes nothing.
        val before = dao.builtInCount()
        ExerciseRepository(ctx, db).syncLibrary(applied) { applied = it }
        assertEquals(before, dao.builtInCount())
        db.close()
    }
}
