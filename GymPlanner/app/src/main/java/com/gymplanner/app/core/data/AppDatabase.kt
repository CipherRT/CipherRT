package com.gymplanner.app.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        Profile::class, BodyWeightEntry::class, Exercise::class, Plan::class,
        PlanDay::class, PlanExercise::class, WorkoutSession::class, SetLog::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun weightDao(): WeightDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "gymplanner.db")
                .addMigrations(*Migrations.ALL)
                // No destructive fallback: an update must never wipe anyone's data.
                .build()
    }
}

/**
 * Add a Migration here every time the schema changes and bump `version` above.
 * Room checks them against the exported schemas in app/schemas/.
 *
 * Example for version 2:
 *   val MIGRATION_1_2 = object : Migration(1, 2) {
 *       override fun migrate(db: SupportSQLiteDatabase) {
 *           db.execSQL("ALTER TABLE profile ADD COLUMN avatarColor INTEGER NOT NULL DEFAULT 0")
 *       }
 *   }
 */
object Migrations {
    val ALL: Array<Migration> = arrayOf()
}
