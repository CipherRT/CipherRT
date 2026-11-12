package com.gymplanner.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.Badge
import com.gymplanner.app.core.ui.BodyMap
import com.gymplanner.app.core.ui.Card
import com.gymplanner.app.core.ui.CheckCircle
import com.gymplanner.app.core.ui.LargeTitle
import com.gymplanner.app.core.ui.PrimaryButton
import com.gymplanner.app.core.ui.SegmentedControl
import com.gymplanner.app.core.ui.TintedButton
import org.junit.Test

class ComponentShots : ScreenshotBase() {
    @Test fun components() = snap("components") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LargeTitle("Back", eyebrow = "Thu · Nov 12 · Next in rotation")
            Card {
                Text("Card title", style = AppTheme.type.headline, color = AppTheme.colors.label)
                BodyMap(setOf("lats"), setOf("rear_delts", "biceps"))
            }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Start workout", onClick = {})
                TintedButton("Train anyway: start Chest", onClick = {})
                SegmentedControl(listOf("Weight", "Cardio", "Body"), "Cardio", {})
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge("Warm-up"); Badge("PR", AppTheme.colors.accent); Badge("DROP", AppTheme.colors.warning)
                    CheckCircle(true, {}); CheckCircle(false, {})
                }
            }
        }
    }
}
