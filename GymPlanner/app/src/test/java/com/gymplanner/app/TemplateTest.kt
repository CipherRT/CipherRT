package com.gymplanner.app

import com.gymplanner.app.core.data.PlanRepository
import com.gymplanner.app.core.model.AppJson
import com.gymplanner.app.core.model.ExerciseLibraryFile
import com.gymplanner.app.core.model.PlanFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks the shipped JSON assets against the v2 spec. */
class TemplateTest {
    private fun asset(name: String) = File("src/main/assets/templates/$name").readText()
    private val file = PlanRepository.parsePlanFile(asset("default.gymplan.json"))
    private val plan = file.plan
    private val library = AppJson.decodeFromString<ExerciseLibraryFile>(asset("exercises.json"))

    private data class Row(val name: String, val sets: Int, val reps: String, val rest: Int, val flags: Set<String>, val note: String = "")

    private fun rows(day: String) = plan.days.first { it.name == day }.exercises.map {
        Row(
            it.name, it.sets, "${it.repMin}-${it.repMax}", it.restSeconds,
            buildSet {
                if (it.isWarmup) add("warmup"); if (it.toFailure) add("failure")
                if (it.isOptional) add("optional"); if (it.perSide) add("side")
            },
            it.note,
        )
    }

    @Test fun fourDaysFormat2() {
        assertEquals(2, file.formatVersion)
        assertEquals("4-Day Split", plan.name)
        assertEquals(listOf("Back", "Chest", "Shoulders & Arms", "Leg"), plan.days.map { it.name })
        assertEquals(listOf("weekly", "weekly", "weekly", "biweekly"), plan.days.map { it.frequency })
    }

    @Test fun backDay() = assertEquals(
        listOf(
            Row("Pull-ups", 1, "8-12", 60, setOf("warmup")),
            Row("Wide-grip lat pulldown", 3, "6-8", 60, setOf()),
            Row("Close-grip lat pulldown", 3, "6-8", 60, setOf()),
            Row("Cable wide-grip row", 3, "8-12", 90, setOf("failure")),
            Row("Back extensions", 2, "8-12", 90, setOf("failure")),
            Row("EZ-bar curl", 3, "8-12", 90, setOf("failure")),
        ),
        rows("Back"),
    )

    @Test fun chestDay() = assertEquals(
        listOf(
            Row("Cable lateral raise", 3, "8-12", 90, setOf("side")),
            Row("Barbell bench press", 4, "3-12", 90, setOf("optional")),
            Row("Incline barbell bench press", 3, "8-12", 90, setOf("failure")),
            Row("Pec deck", 3, "8-12", 90, setOf("failure")),
            Row("Decline chest fly (machine)", 3, "8-12", 90, setOf("failure")),
            Row("Tricep bar pushdown", 3, "8-12", 90, setOf()),
            Row("Overhead cable triceps extension", 3, "8-12", 90, setOf()),
        ),
        rows("Chest"),
    )

    @Test fun shouldersAndArmsDay() = assertEquals(
        listOf(
            Row("Cable lateral raise", 3, "8-12", 60, setOf("side")),
            Row("Machine lateral raise", 3, "8-12", 90, setOf("optional")),
            Row("Reverse pec deck", 3, "8-12", 60, setOf()),
            Row("Machine shoulder press", 3, "8-12", 90, setOf("failure")),
            Row("Incline dumbbell curl", 3, "8-12", 90, setOf("failure")),
            Row("Tricep bar pushdown", 3, "8-12", 90, setOf("failure")),
            Row("Cable curl", 3, "8-12", 90, setOf("failure")),
            Row("Overhead cable triceps extension", 3, "8-12", 90, setOf()),
            Row("Cable crunch", 3, "8-12", 90, setOf()),
            Row("Reverse dragon flag", 3, "3-5", 90, setOf(), "Slow on the descent"),
        ),
        rows("Shoulders & Arms"),
    )

    @Test fun legDay() = assertEquals(
        listOf(
            Row("Leg extension", 3, "8-12", 90, setOf("failure")),
            Row("Seated leg curl", 3, "8-12", 90, setOf("failure")),
            Row("Leg press", 3, "8-12", 90, setOf()),
            Row("Hack squat", 3, "8-12", 90, setOf()),
            Row("Hip abductor", 3, "8-12", 90, setOf()),
            Row("Hip adductor", 3, "8-12", 90, setOf()),
            Row("Seated calf raise", 3, "8-12", 90, setOf("failure")),
            Row("Hip thrust", 2, "8-12", 90, setOf("failure")),
        ),
        rows("Leg"),
    )

    @Test fun reverseDragonFlagCleanedUp() {
        val e = plan.days.flatMap { it.exercises }.first { it.name == "Reverse dragon flag" }
        assertEquals("bodyweight", e.equipment)
        assertTrue(plan.days.flatMap { it.exercises }.all { it.equipment == it.equipment.lowercase() })
    }

    @Test fun libraryCoversTemplateWithGuides() {
        assertTrue(library.exercises.size in 140..170)
        val byName = library.exercises.associateBy { it.name }
        assertEquals(byName.size, library.exercises.size) // no duplicates
        plan.days.flatMap { it.exercises }.forEach { ex ->
            val g = byName[ex.name]?.guide
            assertTrue(ex.name, g != null && g.primaryMuscles.isNotEmpty() && g.steps.size in 3..5 && g.mistakes.size in 1..3)
        }
        val groups = setOf("back", "chest", "shoulders", "biceps", "triceps", "forearms", "legs", "glutes", "calves", "core")
        assertTrue(library.exercises.all { it.muscleGroup in groups })
        assertEquals(groups, library.exercises.map { it.muscleGroup }.toSet())
    }

    @Test fun planFileRoundTripHasNoPersonalData() {
        val text = AppJson.encodeToString(PlanFile.serializer(), PlanFile(plan = plan))
        assertTrue(text.contains("\"formatVersion\": 2"))
        listOf("weightKg", "heightCm", "profile", "reps\"").forEach { assertTrue(it, !text.contains(it)) }
        assertEquals(plan, PlanRepository.parsePlanFile(text).plan)
    }

    /** A plan shared from v1 (format 1, no frequency/superset/drop fields) still loads. */
    @Test fun format1FileStillImports() {
        val v1 = """
            {"formatVersion": 1, "type": "plan", "plan": {"name": "Old", "days": [
              {"name": "Back", "note": "", "weekday": null, "exercises": [
                {"name": "Pull-ups", "muscleGroup": "back", "equipment": "Bodyweight", "sets": 3, "repMin": 0, "repMax": 0,
                 "toFailure": true, "perSide": false, "restSeconds": 150, "note": "", "isOptional": false, "isWarmup": false}
              ]}
            ]}}
        """.trimIndent()
        val p = PlanRepository.parsePlanFile(v1).plan
        assertEquals("weekly", p.days[0].frequency)
        assertEquals(0, p.days[0].exercises[0].dropSets)
        assertEquals(null, p.days[0].exercises[0].supersetGroup)
    }

    @Test(expected = IllegalArgumentException::class)
    fun newerFormatIsRejectedWithMessage() {
        PlanRepository.parsePlanFile("""{"formatVersion": 99, "type": "plan", "plan": {"name": "x"}}""")
    }
}
