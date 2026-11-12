package com.gymplanner.app.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Database(
    entities = [
        Profile::class, BodyWeightEntry::class, Exercise::class, Plan::class,
        PlanDay::class, PlanExercise::class, WorkoutSession::class, SetLog::class,
        BodyMeasurement::class, ExerciseGuide::class, CardioSession::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun weightDao(): WeightDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun trackDao(): TrackDao
    abstract fun guideDao(): GuideDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "gymplanner.db")
                .addMigrations(*Migrations.ALL)
                // No destructive fallback: an update must never wipe anyone's data.
                .build()
    }
}

class Converters {
    private val listSerializer = ListSerializer(String.serializer())

    @TypeConverter fun fromList(list: List<String>): String = Json.encodeToString(listSerializer, list)
    @TypeConverter fun toList(text: String): List<String> =
        if (text.isBlank()) emptyList() else runCatching { Json.decodeFromString(listSerializer, text) }.getOrDefault(emptyList())
}

/**
 * Every schema change gets a Migration here (and `version` above goes up by one).
 * Room checks them against the exported schemas in app/schemas/, and
 * MigrationTest runs each one on a real v1 database. Never destructive.
 */
object Migrations {
    /** v1 → v2: only adds columns and tables; existing data is kept. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Profile settings
            db.execSQL("ALTER TABLE profile ADD COLUMN weeklyTarget INTEGER NOT NULL DEFAULT 4")
            db.execSQL("ALTER TABLE profile ADD COLUMN weighInTime INTEGER NOT NULL DEFAULT 420")
            db.execSQL("ALTER TABLE profile ADD COLUMN weighInDays INTEGER NOT NULL DEFAULT 127")
            db.execSQL("ALTER TABLE profile ADD COLUMN weighInEnabled INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE profile ADD COLUMN cardioCountsTowardTarget INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE profile ADD COLUMN barWeightKg REAL NOT NULL DEFAULT 20.0")
            // Plans
            db.execSQL("ALTER TABLE plan ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE plan_day ADD COLUMN frequency TEXT NOT NULL DEFAULT 'weekly'")
            db.execSQL("ALTER TABLE plan_exercise ADD COLUMN supersetGroup INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE plan_exercise ADD COLUMN dropSets INTEGER NOT NULL DEFAULT 0")
            // Sets
            db.execSQL("ALTER TABLE set_log ADD COLUMN setType TEXT NOT NULL DEFAULT 'normal'")
            db.execSQL("ALTER TABLE set_log ADD COLUMN parentSetId INTEGER DEFAULT NULL")
            // Sets logged for warm-up exercises in v1 become warm-up sets.
            db.execSQL(
                "UPDATE set_log SET setType = 'warmup' WHERE planExerciseId IN " +
                    "(SELECT id FROM plan_exercise WHERE isWarmup = 1)"
            )
            // Equipment names are lowercase from v2 on.
            db.execSQL("UPDATE exercise SET equipment = LOWER(equipment)")
            // New tables
            CREATE_TABLES_V2.forEach(db::execSQL)
        }
    }

    // Copied from app/schemas/.../2.json so the tables match exactly what Room expects.
    private val CREATE_TABLES_V2 = listOf(
        "CREATE TABLE IF NOT EXISTS `body_measurement` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `waistCm` REAL, `armCm` REAL, `chestCm` REAL, `thighCm` REAL, FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_body_measurement_profileId_date` ON `body_measurement` (`profileId`, `date`)",
        "CREATE TABLE IF NOT EXISTS `exercise_guide` (`exerciseId` INTEGER NOT NULL, `primaryMuscles` TEXT NOT NULL, `secondaryMuscles` TEXT NOT NULL, `steps` TEXT NOT NULL, `mistakes` TEXT NOT NULL, `videoQuery` TEXT NOT NULL, PRIMARY KEY(`exerciseId`), FOREIGN KEY(`exerciseId`) REFERENCES `exercise`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE TABLE IF NOT EXISTS `cardio_session` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `machine` TEXT NOT NULL, `customName` TEXT NOT NULL, `minutes` INTEGER NOT NULL, `kcal` INTEGER NOT NULL, `distanceKm` REAL, `avgHeartRate` INTEGER, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_cardio_session_profileId_date` ON `cardio_session` (`profileId`, `date`)",
    )

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
