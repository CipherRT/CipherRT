package com.gymplanner.app.feature.weighin

import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.GymApp
import com.gymplanner.app.core.data.Profile
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.model.Units
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.core.ui.GymTheme
import com.gymplanner.app.core.ui.PrimaryButton
import com.gymplanner.app.core.ui.cardSurface
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Alarm-style morning weigh-in, shown over the lock screen. */
class WeighInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        enableEdgeToEdge()
        val app = application as GymApp
        val c = app.container
        val profileId = intent.getLongExtra(WeighInScheduler.EXTRA_PROFILE, c.prefs.currentProfileId.value ?: -1)

        lifecycleScope.launch {
            val profile = c.profiles.get(profileId)
            val latest = c.weights.latest(profileId)
            val preview = intent.getBooleanExtra(WeighInScheduler.EXTRA_PREVIEW, false)
            if (profile == null || (!preview && latest?.date == Dates.today())) { close(profileId); return@launch }
            setContent {
                val accent by c.prefs.accent.collectAsStateWithLifecycle()
                GymTheme(accent) {
                    WeighInScreen(
                        profile = profile,
                        hintKg = latest?.weightKg,
                        onSave = { kg ->
                            lifecycleScope.launch {
                                c.weights.add(profileId, Dates.today(), kg)
                                close(profileId)
                            }
                        },
                        onSnooze = {
                            WeighInScheduler.snooze(this@WeighInActivity, profileId)
                            close(profileId, cancelSnooze = false)
                        },
                        onSkip = { close(profileId) },
                    )
                }
            }
        }
    }

    private fun close(profileId: Long, cancelSnooze: Boolean = true) {
        WeighInReceiver.dismiss(this, profileId)
        if (cancelSnooze) WeighInScheduler.cancelSnooze(this, profileId)
        finish()
    }
}

@Composable
fun WeighInScreen(profile: Profile, hintKg: Double?, onSave: (Double) -> Unit, onSnooze: () -> Unit, onSkip: () -> Unit) {
    val units = UnitSystem.from(profile.unitSystem)
    val hint = hintKg?.let { Units.formatNumber(Units.kgToDisplay(it, units)) }
    var text by remember { mutableStateOf("") }
    val view = LocalView.current
    val value = Units.parse(text.ifEmpty { hint ?: "" })
    val valid = value != null && value > 0 && value < (if (units == UnitSystem.METRIC) 400.0 else 880.0)

    fun press(k: String) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        text = when (k) {
            "⌫" -> text.dropLast(1)
            "." -> if (text.contains('.')) text else if (text.isEmpty()) "0." else "$text."
            else -> {
                val next = if (text == "0") k else text + k
                if (next.replace(".", "").length > 5 || next.substringAfter('.', "").length > 2) text else next
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C0D))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                LocalTime.now().format(DateTimeFormatter.ofPattern("H:mm")),
                style = AppTheme.type.bigNumber.copy(fontSize = 56.sp, fontWeight = FontWeight.SemiBold), color = AppTheme.colors.label,
            )
            val hour = LocalTime.now().hour
            val greeting = when { hour < 12 -> "Good morning"; hour < 18 -> "Good afternoon"; else -> "Good evening" }
            Text("$greeting, ${profile.name}. Step on the scale.", style = AppTheme.type.callout, color = AppTheme.colors.secondaryLabel)
        }
        Column(
            Modifier.fillMaxWidth().cardSurface(RoundedCornerShape(24.dp), Color(0xFF17191C)).padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("TODAY’S WEIGHT", style = AppTheme.type.sectionHeader, color = AppTheme.colors.secondaryLabel)
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.semantics { contentDescription = "Today's weight ${text.ifEmpty { hint ?: "" }} ${units.weightUnit}" }) {
                Text(
                    text.ifEmpty { hint ?: "0" },
                    style = AppTheme.type.bigNumber.copy(fontSize = 64.sp),
                    color = if (text.isEmpty()) AppTheme.colors.accent.copy(alpha = 0.45f) else AppTheme.colors.accent,
                )
                Text(" ${units.weightUnit}", style = AppTheme.type.title3, color = AppTheme.colors.secondaryLabel, modifier = Modifier.padding(bottom = 10.dp))
            }
            hint?.let { Text("Last time $it ${units.weightUnit}", style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "⌫")).forEach { row ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { k ->
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .heightIn(min = 56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppTheme.colors.card)
                                .clickable(role = Role.Button) { press(k) }
                                .semantics { contentDescription = when (k) { "⌫" -> "Delete"; "." -> "Decimal point"; else -> k } },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (k == "⌫") Icon(Icons.AutoMirrored.Rounded.Backspace, null, tint = AppTheme.colors.label)
                            else Text(k, style = AppTheme.type.number.copy(fontSize = 26.sp, fontWeight = FontWeight.SemiBold), color = AppTheme.colors.label)
                        }
                    }
                }
            }
        }
        PrimaryButton("Save weight", enabled = valid, onClick = { value?.let { onSave(Units.displayToKg(it, units)) } })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Outlined("Snooze 10 min", AppTheme.colors.label, Modifier.weight(1f), onSnooze)
            Outlined("Skip today", AppTheme.colors.secondaryLabel, Modifier.weight(1f), onSkip)
        }
    }
}

@Composable
private fun Outlined(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp).cardSurface(RoundedCornerShape(14.dp), Color.Transparent).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = AppTheme.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = color) }
}
