package com.gymplanner.app.screens

import com.gymplanner.app.core.data.Profile
import com.gymplanner.app.feature.weighin.WeighInScreen
import org.junit.Test

class WeighInShots : ScreenshotBase() {
    @Test fun weighIn() = snap("weigh_in") {
        WeighInScreen(Profile(id = 1, name = "Alex", heightCm = 180.0, unitSystem = "METRIC", goal = "BULK", createdAt = 0), 78.0, {}, {}, {})
    }
}
