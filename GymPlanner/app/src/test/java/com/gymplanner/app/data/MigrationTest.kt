package com.gymplanner.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gymplanner.app.core.data.AppDatabase
import com.gymplanner.app.core.data.Migrations
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Builds a real v1 database (exactly as v1 of the app left it), runs the v1 → v2
 * migration, lets Room validate the result against the v2 schema, and checks that
 * every row survived.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)


    @Test
    fun migrate1To2_keepsAllData() {
        helper.createDatabase(dbName, 1).apply {
            fun row(table: String, vararg v: Pair<String, Any?>) {
                val cv = ContentValues()
                v.forEach { (k, value) ->
                    when (value) {
                        null -> cv.putNull(k)
                        is Long -> cv.put(k, value)
                        is Int -> cv.put(k, value)
                        is Double -> cv.put(k, value)
                        is Boolean -> cv.put(k, if (value) 1 else 0)
                        is String -> cv.put(k, value)
                    }
                }
                insert(table, SQLiteDatabase.CONFLICT_ABORT, cv)
            }
            row("profile", "id" to 1L, "name" to "Mulka", "heightCm" to 180.0, "unitSystem" to "METRIC", "goal" to "BULK", "createdAt" to 1L)
            row("body_weight", "id" to 1L, "profileId" to 1L, "date" to 20000L, "weightKg" to 78.0, "note" to "")
            row("exercise", "id" to 1L, "name" to "Pull-ups", "muscleGroup" to "back", "equipment" to "Bodyweight", "isCustom" to false, "profileId" to null)
            row("exercise", "id" to 2L, "name" to "My curl", "muscleGroup" to "biceps", "equipment" to "Cable", "isCustom" to true, "profileId" to 1L)
            row("plan", "id" to 1L, "profileId" to 1L, "name" to "My Plan", "isActive" to true, "daysPerWeek" to 1, "createdAt" to 1L)
            row("plan_day", "id" to 1L, "planId" to 1L, "name" to "Back", "orderIndex" to 0, "weekday" to null, "note" to "Fight")
            row(
                "plan_exercise", "id" to 1L, "planDayId" to 1L, "exerciseId" to 1L, "orderIndex" to 0, "sets" to 1, "repMin" to 8,
                "repMax" to 12, "toFailure" to false, "restSeconds" to 60, "note" to "", "isOptional" to false, "perSide" to false, "isWarmup" to true,
            )
            row(
                "plan_exercise", "id" to 2L, "planDayId" to 1L, "exerciseId" to 2L, "orderIndex" to 1, "sets" to 3, "repMin" to 8,
                "repMax" to 12, "toFailure" to true, "restSeconds" to 90, "note" to "", "isOptional" to false, "perSide" to false, "isWarmup" to false,
            )
            row("workout_session", "id" to 1L, "profileId" to 1L, "planDayId" to 1L, "dayName" to "Back", "startedAt" to 100L, "finishedAt" to 200L, "note" to "", "isLight" to false)
            row("set_log", "id" to 1L, "sessionId" to 1L, "exerciseId" to 1L, "setNumber" to 1, "weightKg" to 0.0, "reps" to 10, "completed" to true, "exerciseOrder" to 0, "planExerciseId" to 1L)
            row("set_log", "id" to 2L, "sessionId" to 1L, "exerciseId" to 2L, "setNumber" to 1, "weightKg" to 20.0, "reps" to 9, "completed" to true, "exerciseOrder" to 1, "planExerciseId" to 2L)
            close()
        }

        // Runs the migration and validates the schema against 2.json.
        val db = helper.runMigrationsAndValidate(dbName, 2, true, *Migrations.ALL)

        fun count(table: String) = db.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }
        listOf("profile" to 1, "body_weight" to 1, "exercise" to 2, "plan" to 1, "plan_day" to 1, "plan_exercise" to 2, "workout_session" to 1, "set_log" to 2)
            .forEach { (t, n) -> assertEquals(t, n, count(t)) }
        listOf("cardio_session", "body_measurement", "exercise_guide").forEach { assertEquals(it, 0, count(it)) }

        db.query("SELECT weeklyTarget, weighInTime, weighInDays, weighInEnabled, barWeightKg FROM profile").use {
            it.moveToFirst()
            assertEquals(4, it.getInt(0)); assertEquals(420, it.getInt(1)); assertEquals(127, it.getInt(2))
            assertEquals(1, it.getInt(3)); assertEquals(20.0, it.getDouble(4), 0.0)
        }
        db.query("SELECT frequency FROM plan_day").use { it.moveToFirst(); assertEquals("weekly", it.getString(0)) }
        db.query("SELECT setType FROM set_log ORDER BY id").use {
            it.moveToFirst(); assertEquals("warmup", it.getString(0))
            it.moveToNext(); assertEquals("normal", it.getString(0))
        }
        db.query("SELECT equipment FROM exercise ORDER BY id").use {
            it.moveToFirst(); assertEquals("bodyweight", it.getString(0))
            it.moveToNext(); assertEquals("cable", it.getString(0))
        }
        db.close()
    }

    /** Opening through the real app builder (with our migrations) works and reads old rows. */
    @Test
    fun appBuilderOpensMigratedDatabase() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO profile (id, name, heightCm, unitSystem, goal, createdAt) VALUES (1, 'A', 170, 'METRIC', 'CUT', 1)")
            close()
        }
        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, dbName)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()
        val p = runBlocking { db.profileDao().get(1) }
        assertTrue(p != null && p.name == "A" && p.weeklyTarget == 4 && p.weighInEnabled)
        db.close()
    }
}

