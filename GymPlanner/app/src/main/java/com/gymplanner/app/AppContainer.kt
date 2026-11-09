package com.gymplanner.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.gymplanner.app.core.data.AppDatabase
import com.gymplanner.app.core.data.BackupRepository
import com.gymplanner.app.core.data.ExerciseRepository
import com.gymplanner.app.core.data.PlanRepository
import com.gymplanner.app.core.data.Prefs
import com.gymplanner.app.core.data.ProfileRepository
import com.gymplanner.app.core.data.WeightRepository
import com.gymplanner.app.core.data.WorkoutRepository

/** Simple hand-written dependency container — one instance for the whole app. */
class AppContainer(context: Context) {
    /** For short writes that should finish even if the screen that started them closes. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val prefs = Prefs(context)
    val db = AppDatabase.create(context)
    val profiles = ProfileRepository(db, prefs)
    val weights = WeightRepository(db)
    val exercises = ExerciseRepository(context, db)
    val plans = PlanRepository(context, db, exercises)
    val workouts = WorkoutRepository(db)
    val backup = BackupRepository(db, plans, exercises, prefs)
}
