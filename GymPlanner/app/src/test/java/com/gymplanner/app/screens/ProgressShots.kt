package com.gymplanner.app.screens

import com.gymplanner.app.ProfileMenuTargets
import com.gymplanner.app.feature.progress.ProgressScreen
import org.junit.Test

class ProgressShots : ScreenshotBase() {
    @Test fun progress() {
        Seed.basic(trainedToday = true)
        snap("progress") { ProgressScreen(ProfileMenuTargets({}, {})) {} }
    }
}
