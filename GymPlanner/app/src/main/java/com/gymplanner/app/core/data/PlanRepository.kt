package com.gymplanner.app.core.data

import android.content.Context
import androidx.room.withTransaction
import com.gymplanner.app.core.model.AppJson
import com.gymplanner.app.core.model.PlanDayJson
import com.gymplanner.app.core.model.PlanExerciseJson
import com.gymplanner.app.core.model.PlanFile
import com.gymplanner.app.core.model.PlanJson
import kotlinx.coroutines.flow.Flow

data class TemplateInfo(val fileName: String, val plan: PlanJson)

class PlanRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val exercises: ExerciseRepository,
) {
    private val dao = db.planDao()

    fun plans(profileId: Long): Flow<List<Plan>> = dao.observePlans(profileId)
    fun activePlan(profileId: Long): Flow<Plan?> = dao.observeActive(profileId)
    fun plan(id: Long): Flow<Plan?> = dao.observePlan(id)
    fun days(planId: Long): Flow<List<PlanDay>> = dao.observeDays(planId)
    fun day(id: Long): Flow<PlanDay?> = dao.observeDay(id)
    fun dayExercises(dayId: Long): Flow<List<PlanExercise>> = dao.observeDayExercises(dayId)
    fun planExercises(planId: Long): Flow<List<PlanExercise>> = dao.observePlanExercises(planId)
    fun planExercise(id: Long): Flow<PlanExercise?> = dao.observePlanExercise(id)

    suspend fun getDay(id: Long) = dao.getDay(id)
    suspend fun getDays(planId: Long) = dao.days(planId)
    suspend fun getDayExercises(dayId: Long) = dao.dayExercises(dayId)
    suspend fun getPlanExercises(ids: List<Long>) = if (ids.isEmpty()) emptyList() else dao.getPlanExercises(ids)

    // ---- Plans ----

    suspend fun createEmpty(profileId: Long, name: String, makeActive: Boolean): Long = db.withTransaction {
        val makeItActive = makeActive || dao.plans(profileId).isEmpty()
        val id = dao.insertPlan(
            Plan(profileId = profileId, name = name.trim(), isActive = false, daysPerWeek = 0, createdAt = System.currentTimeMillis())
        )
        if (makeItActive) dao.setActive(profileId, id)
        id
    }

    suspend fun rename(plan: Plan, name: String) = dao.updatePlan(plan.copy(name = name.trim()))

    suspend fun setActive(plan: Plan) = dao.setActive(plan.profileId, plan.id)

    /** Workout history is kept: sessions only reference the day id loosely. */
    suspend fun delete(plan: Plan) = db.withTransaction {
        dao.deletePlan(plan)
        if (plan.isActive) {
            dao.plans(plan.profileId).maxByOrNull { it.createdAt }?.let { dao.setActive(plan.profileId, it.id) }
        }
    }

    suspend fun duplicate(plan: Plan): Long {
        val json = toJson(plan.id, plan.name)
        return importPlan(plan.profileId, json.copy(name = "${plan.name} copy"), makeActive = false)
    }

    private suspend fun refreshDayCount(planId: Long) {
        val plan = dao.getPlan(planId) ?: return
        dao.updatePlan(plan.copy(daysPerWeek = dao.dayCount(planId)))
    }

    // ---- Days ----

    suspend fun addDay(planId: Long, name: String): Long {
        val order = dao.days(planId).size
        val id = dao.insertDay(PlanDay(planId = planId, name = name.trim(), orderIndex = order))
        refreshDayCount(planId)
        return id
    }

    suspend fun updateDay(day: PlanDay) = dao.updateDay(day)

    suspend fun deleteDay(day: PlanDay) {
        dao.deleteDay(day)
        dao.updateDays(dao.days(day.planId).mapIndexed { i, d -> d.copy(orderIndex = i) })
        refreshDayCount(day.planId)
    }

    suspend fun reorderDays(ordered: List<PlanDay>) =
        dao.updateDays(ordered.mapIndexed { i, d -> d.copy(orderIndex = i) })

    // ---- Exercises in a day ----

    suspend fun addExercise(dayId: Long, exerciseId: Long, defaultRest: Int): Long {
        val list = dao.dayExercises(dayId)
        return dao.insertPlanExercise(
            PlanExercise(
                planDayId = dayId, exerciseId = exerciseId, orderIndex = list.size,
                sets = 3, repMin = 8, repMax = 12, toFailure = false, restSeconds = defaultRest,
            )
        )
    }

    suspend fun updateExercise(pe: PlanExercise) = dao.updatePlanExercise(pe)

    suspend fun removeExercise(pe: PlanExercise) {
        dao.deletePlanExercise(pe)
        dao.updatePlanExercises(dao.dayExercises(pe.planDayId).mapIndexed { i, e -> e.copy(orderIndex = i) })
    }

    suspend fun reorderExercises(ordered: List<PlanExercise>) =
        dao.updatePlanExercises(ordered.mapIndexed { i, e -> e.copy(orderIndex = i) })

    // ---- Import / export / templates ----

    /** Builds the shareable JSON for a plan: settings only, no weights or logs. */
    suspend fun toJson(planId: Long, name: String? = null): PlanJson {
        val plan = dao.getPlan(planId) ?: error("Plan not found")
        val days = dao.days(planId).map { day ->
            val items = dao.dayExercises(day.id)
            val exMap = db.exerciseDao().getAll(items.map { it.exerciseId }).associateBy { it.id }
            PlanDayJson(
                name = day.name,
                note = day.note,
                weekday = day.weekday,
                exercises = items.mapNotNull { pe ->
                    val ex = exMap[pe.exerciseId] ?: return@mapNotNull null
                    PlanExerciseJson(
                        name = ex.name, muscleGroup = ex.muscleGroup, equipment = ex.equipment,
                        sets = pe.sets, repMin = pe.repMin, repMax = pe.repMax, toFailure = pe.toFailure,
                        perSide = pe.perSide, restSeconds = pe.restSeconds, note = pe.note,
                        isOptional = pe.isOptional, isWarmup = pe.isWarmup,
                    )
                },
            )
        }
        return PlanJson(name = name ?: plan.name, days = days)
    }

    suspend fun exportFile(planId: Long): PlanFile = PlanFile(plan = toJson(planId))

    /** Adds a plan from JSON. Matching exercises are reused by name, others are created. */
    suspend fun importPlan(profileId: Long, plan: PlanJson, makeActive: Boolean): Long = db.withTransaction {
        val planId = createEmpty(profileId, plan.name.ifBlank { "Imported plan" }, makeActive)
        val cache = mutableMapOf<String, Long>()
        plan.days.forEachIndexed { dayIndex, day ->
            val dayId = dao.insertDay(
                PlanDay(
                    planId = planId,
                    name = day.name.ifBlank { "Day ${dayIndex + 1}" },
                    orderIndex = dayIndex,
                    weekday = day.weekday?.takeIf { it in 1..7 },
                    note = day.note,
                )
            )
            day.exercises.forEachIndexed { i, e ->
                val exId = cache.getOrPut(e.name.lowercase()) {
                    exercises.findOrCreate(profileId, e.name, e.muscleGroup, e.equipment)
                }
                dao.insertPlanExercise(
                    PlanExercise(
                        planDayId = dayId, exerciseId = exId, orderIndex = i,
                        sets = e.sets.coerceIn(1, 20), repMin = e.repMin.coerceAtLeast(0),
                        repMax = e.repMax.coerceAtLeast(e.repMin), toFailure = e.toFailure,
                        restSeconds = e.restSeconds.coerceIn(0, 900), note = e.note,
                        isOptional = e.isOptional, perSide = e.perSide, isWarmup = e.isWarmup,
                    )
                )
            }
        }
        refreshDayCount(planId)
        planId
    }

    // Built-in templates live in assets/templates as .gymplan.json files: add a file to add a template.
    fun templates(): List<TemplateInfo> {
        val files = context.assets.list("templates").orEmpty().filter { it.endsWith(".gymplan.json") }.sorted()
        return files.mapNotNull { f ->
            runCatching {
                val text = context.assets.open("templates/$f").bufferedReader().use { it.readText() }
                TemplateInfo(f, parsePlanFile(text).plan)
            }.getOrNull()
        }
    }

    companion object {
        fun parsePlanFile(text: String): PlanFile {
            val file = AppJson.decodeFromString<PlanFile>(text)
            require(file.type == "plan") { "This file isn't a plan." }
            require(file.formatVersion <= com.gymplanner.app.core.model.CURRENT_FORMAT_VERSION) {
                "This plan was made with a newer version of the app. Please update."
            }
            return file
        }
    }
}
