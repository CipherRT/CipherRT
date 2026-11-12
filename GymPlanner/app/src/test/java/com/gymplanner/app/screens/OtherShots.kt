package com.gymplanner.app.screens

import com.gymplanner.app.ProfileMenuTargets
import com.gymplanner.app.feature.onboarding.OnboardingScreen
import com.gymplanner.app.feature.plan.ExerciseDemoScreen
import com.gymplanner.app.feature.plan.PlanScreen
import com.gymplanner.app.feature.settings.SettingsScreen
import kotlinx.coroutines.runBlocking
import org.junit.Test

class OnboardingShots : ScreenshotBase() {
    @Test fun welcome() {
        val app = Seed.app(); while (!app.ready.value) Thread.sleep(20)
        snap("onboarding") { OnboardingScreen(isFirstProfile = true, onDone = {}, onCancel = null) }
    }
}

class PlanShots : ScreenshotBase() {
    @Test fun plan() {
        Seed.basic()
        snap("plan") { PlanScreen(ProfileMenuTargets({}, {}), {}, {}) }
    }
}

class SettingsShots : ScreenshotBase() {
    @Test fun settings() {
        Seed.basic()
        snap("settings") { SettingsScreen({}, {}) }
    }
}

class DemoShots : ScreenshotBase() {
    @Test fun demo() {
        val c = Seed.basic()
        val id = runBlocking { c.db.exerciseDao().builtIns().first { it.name == "Wide-grip lat pulldown" }.id }
        snap("demo") { ExerciseDemoScreen(id) {} }
    }
}
