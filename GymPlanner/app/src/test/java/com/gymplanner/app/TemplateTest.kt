package com.gymplanner.app

import com.gymplanner.app.core.data.PlanRepository
import com.gymplanner.app.core.model.AppJson
import com.gymplanner.app.core.model.ExerciseLibraryFile
import com.gymplanner.app.core.model.PlanFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks the shipped JSON assets against the spec. */
class TemplateTest {
    private fun asset(name: String) = File("src/main/assets/templates/$name").readText()
    private val plan = PlanRepository.parsePlanFile(asset("5-day-split.gymplan.json")).plan
    private val library = AppJson.decodeFromString<ExerciseLibraryFile>(asset("exercises.json"))

    @Test fun fiveDays() {
        assertEquals(listOf("Back", "Chest", "Arms", "Back/Arms", "Legs"), plan.days.map { it.name })
        assertEquals("Every other week.", plan.days.last().note)
    }

    @Test fun everyDayStartsWithWarmUp() {
        plan.days.forEach { d ->
            val w = d.exercises.take(3)
            assertEquals(listOf("Shoulder warm-ups", "Reverse pec deck", "Machine shoulder press"), w.map { it.name })
            assertTrue(w.all { it.isWarmup })
            assertEquals(2 to 15, w[1].sets to w[1].repMin)
            assertEquals(2 to 12, w[2].sets to w[2].repMax)
        }
    }

    @Test fun backDayMatchesTable() {
        val back = plan.days.first()
        assertEquals("Last 1–2 reps of each set should be a real fight.", back.note)
        val main = back.exercises.filter { !it.isWarmup }
        data class Row(val name: String, val sets: Int, val reps: String, val rest: Int, val optional: Boolean)
        val expected = listOf(
            Row("Pull-ups", 3, "failure", 150, false),
            Row("Wide-grip lat pulldown", 3, "8-10", 90, false),
            Row("Close-grip lat pulldown", 3, "10-12", 90, false),
            Row("Machine lat pulldown", 2, "12-15", 90, true),
            Row("Cable wide-grip row", 3, "10-12", 90, false),
            Row("Single-arm row", 3, "10-10 side", 90, true),
            Row("Back extensions", 3, "12-15", 90, false),
        )
        val actual = main.map {
            val reps = if (it.toFailure) "failure" else "${it.repMin}-${it.repMax}" + if (it.perSide) " side" else ""
            Row(it.name, it.sets, reps, it.restSeconds, it.isOptional)
        }
        assertEquals(expected, actual)
        assertEquals("1-second squeeze at the back.", main.first { it.name == "Cable wide-grip row" }.note)
    }

    @Test fun otherDaysHaveFourToFiveExercises() {
        plan.days.drop(1).forEach { d -> assertTrue(d.name, d.exercises.count { !it.isWarmup } in 4..6) }
    }

    @Test fun libraryCoversTemplateAndSize() {
        assertTrue(library.exercises.size in 60..80)
        val names = library.exercises.map { it.name }.toSet()
        plan.days.flatMap { it.exercises }.forEach { assertTrue(it.name, it.name in names) }
        assertEquals(names.size, library.exercises.size) // no duplicates
        val groups = setOf("back", "chest", "shoulders", "biceps", "triceps", "legs", "core")
        assertTrue(library.exercises.all { it.muscleGroup in groups })
    }

    @Test fun planFileRoundTripHasNoPersonalData() {
        val text = AppJson.encodeToString(PlanFile.serializer(), PlanFile(plan = plan))
        assertTrue(text.contains("\"formatVersion\": 1"))
        listOf("weightKg", "heightCm", "profile", "reps\"").forEach { assertTrue(it, !text.contains(it)) }
        assertEquals(plan, PlanRepository.parsePlanFile(text).plan)
    }

    @Test(expected = IllegalArgumentException::class)
    fun newerFormatIsRejectedWithMessage() {
        PlanRepository.parsePlanFile("""{"formatVersion": 99, "type": "plan", "plan": {"name": "x"}}""")
    }
}
