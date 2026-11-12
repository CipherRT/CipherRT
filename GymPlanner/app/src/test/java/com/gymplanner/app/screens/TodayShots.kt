package com.gymplanner.app.screens

import com.gymplanner.app.ProfileMenuTargets
import com.gymplanner.app.feature.today.TodayScreen
import org.junit.Test
import java.time.LocalDate

class TodayShots : ScreenshotBase() {
    private val menu = ProfileMenuTargets({}, {})

    @Test fun gymDay() {
        Seed.basic()
        snap("today_gym") { TodayScreen(menu, {}, {}, {}, {}) }
    }

    @Test fun offDay() {
        val c = Seed.basic()
        c.prefs.setNotToday(LocalDate.now().toEpochDay())
        snap("today_off") { TodayScreen(menu, {}, {}, {}, {}) }
    }
}
