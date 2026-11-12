package com.gymplanner.app.core.model

import java.util.Locale
import kotlin.math.roundToInt

enum class UnitSystem(val label: String) {
    METRIC("kg · cm"),
    IMPERIAL("lb · in");

    val weightUnit: String get() = if (this == METRIC) "kg" else "lb"

    companion object {
        fun from(value: String?): UnitSystem = entries.firstOrNull { it.name == value } ?: METRIC
    }
}

enum class Goal(val label: String, val blurb: String) {
    BULK("Build muscle", "Gain size and strength"),
    CUT("Lose fat", "Get leaner, keep muscle"),
    MAINTAIN("Stay fit", "Keep what you have");

    companion object {
        fun from(value: String?): Goal = entries.firstOrNull { it.name == value } ?: MAINTAIN
    }
}

/**
 * Everything is stored in kg / cm. These helpers convert for display and input,
 * so switching units never changes the saved data.
 */
object Units {
    const val LB_PER_KG = 2.2046226218
    const val CM_PER_IN = 2.54

    fun kgToDisplay(kg: Double, units: UnitSystem): Double =
        if (units == UnitSystem.METRIC) kg else kg * LB_PER_KG

    fun displayToKg(value: Double, units: UnitSystem): Double =
        if (units == UnitSystem.METRIC) value else value / LB_PER_KG

    fun cmToDisplay(cm: Double, units: UnitSystem): Double =
        if (units == UnitSystem.METRIC) cm else cm / CM_PER_IN

    fun displayToCm(value: Double, units: UnitSystem): Double =
        if (units == UnitSystem.METRIC) value else value * CM_PER_IN

    /** "82.5" — trims a trailing ".0". */
    fun formatNumber(value: Double, decimals: Int = 1): String {
        val s = String.format(Locale.US, "%.${decimals}f", value)
        return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
    }

    fun formatWeight(kg: Double, units: UnitSystem, withUnit: Boolean = true): String {
        val n = formatNumber(kgToDisplay(kg, units))
        return if (withUnit) "$n ${units.weightUnit}" else n
    }

    fun formatHeight(cm: Double, units: UnitSystem): String =
        if (units == UnitSystem.METRIC) {
            "${cm.roundToInt()} cm"
        } else {
            val totalIn = (cm / CM_PER_IN).roundToInt()
            "${totalIn / 12}′ ${totalIn % 12}″"
        }

    /** Volume (weight × reps) in the user's unit, rounded to whole numbers. */
    fun formatVolume(kg: Double, units: UnitSystem): String {
        val v = kgToDisplay(kg, units).roundToInt()
        return String.format(Locale.US, "%,d %s", v, units.weightUnit)
    }

    fun bmi(weightKg: Double, heightCm: Double): Double? {
        if (heightCm <= 0 || weightKg <= 0) return null
        val m = heightCm / 100.0
        return weightKg / (m * m)
    }

    fun parse(text: String): Double? = text.replace(',', '.').toDoubleOrNull()
}

object Weekdays {
    private val short = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val long = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    /** 1 = Monday … 7 = Sunday (java.time.DayOfWeek values). */
    fun short(day: Int): String = short.getOrElse(day - 1) { "" }
    fun long(day: Int): String = long.getOrElse(day - 1) { "" }
}

object MuscleGroups {
    val all = listOf("back", "chest", "shoulders", "biceps", "triceps", "forearms", "legs", "glutes", "calves", "core")
    fun label(group: String): String = group.replaceFirstChar { it.uppercase() }
}

/** Muscles the body map can highlight, with the names people use. */
object Muscles {
    val labels = linkedMapOf(
        "chest" to "Chest", "front_delts" to "Front delts", "side_delts" to "Side delts", "rear_delts" to "Rear delts",
        "traps" to "Traps", "lats" to "Lats", "upper_back" to "Upper back", "lower_back" to "Lower back",
        "biceps" to "Biceps", "triceps" to "Triceps", "forearms" to "Forearms", "abs" to "Abs", "obliques" to "Obliques",
        "hip_flexors" to "Hip flexors", "glutes" to "Glutes", "quads" to "Quads", "hamstrings" to "Hamstrings",
        "adductors" to "Inner thighs", "abductors" to "Outer hips", "calves" to "Calves",
    )
    fun label(key: String): String = labels[key] ?: key.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
