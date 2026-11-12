package com.gymplanner.app

import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.model.Units
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {
    @Test fun kgToLbAndBack() {
        val kg = Units.displayToKg(100.0, UnitSystem.IMPERIAL)
        assertEquals(45.359, kg, 0.001)
        assertEquals("100 lb", Units.formatWeight(kg, UnitSystem.IMPERIAL))
        assertEquals("45.4 kg", Units.formatWeight(kg, UnitSystem.METRIC))
    }

    @Test fun metricIsUnchanged() {
        assertEquals(82.5, Units.displayToKg(82.5, UnitSystem.METRIC), 0.0)
        assertEquals("82.5 kg", Units.formatWeight(82.5, UnitSystem.METRIC))
        assertEquals("80 kg", Units.formatWeight(80.0, UnitSystem.METRIC))
    }

    @Test fun height() {
        assertEquals("180 cm", Units.formatHeight(180.0, UnitSystem.METRIC))
        assertEquals("5′ 11″", Units.formatHeight(180.0, UnitSystem.IMPERIAL))
        assertEquals(177.8, Units.displayToCm(70.0, UnitSystem.IMPERIAL), 0.001)
    }

    @Test fun bmi() {
        assertEquals(24.69, Units.bmi(80.0, 180.0)!!, 0.01)
        assertEquals(null, Units.bmi(80.0, 0.0))
    }

    @Test fun volume() {
        assertEquals("1,000 kg", Units.formatVolume(1000.0, UnitSystem.METRIC))
        assertEquals("2,205 lb", Units.formatVolume(1000.0, UnitSystem.IMPERIAL))
    }

    @Test fun parseAcceptsComma() {
        assertEquals(82.5, Units.parse("82,5")!!, 0.0)
    }
}

class PlateCalculatorTest {
    private val calc = com.gymplanner.app.feature.today.PlateCalculator

    @Test fun kg100() {
        val r = calc.compute(100.0, 20.0, UnitSystem.METRIC)
        assertEquals(listOf(25.0 to 1, 15.0 to 1), r.perSide)
        assertEquals(100.0, r.achieved, 0.0)
    }

    @Test fun lb225() = assertEquals(listOf(45.0 to 2), calc.compute(225.0, 45.0, UnitSystem.IMPERIAL).perSide)

    @Test fun leftoverWhenNotLoadable() {
        val r = calc.compute(21.0, 20.0, UnitSystem.METRIC)
        assertEquals(emptyList<Pair<Double, Int>>(), r.perSide)
        assertEquals(1.0, r.leftover, 0.001)
    }

    @Test fun dropRounding() {
        assertEquals(45.0, calc.roundToLoadable(55.0 * 0.8, UnitSystem.METRIC), 0.0)
        assertEquals(40.0, calc.roundToLoadable(50.0 * 0.8, UnitSystem.METRIC), 0.0)
    }
}
