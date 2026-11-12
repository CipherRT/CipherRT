package com.gymplanner.app.core.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymplanner.app.core.model.Units

/**
 * Big-key number pad in a bottom sheet — made for sweaty hands mid-set.
 * The first key press replaces the pre-filled value, so a new number is quick to type,
 * while "Save" straight away keeps the pre-filled value (one tap).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberPadSheet(
    title: String,
    initialValue: Double?,
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    allowDecimal: Boolean = true,
    step: Double? = null,
    confirmText: String = "Save",
    minValue: Double = 0.0,
    maxValue: Double = 9999.0,
    allowZero: Boolean = false,
    extraContent: @Composable () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf(initialValue?.let { Units.formatNumber(it, 2) } ?: "") }
    var fresh by remember { mutableStateOf(true) }
    val view = LocalView.current

    fun press(key: String) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        val base = if (fresh) "" else text
        fresh = false
        text = when (key) {
            "⌫" -> base.dropLast(1)
            "." -> if (!allowDecimal || base.contains('.')) base else (if (base.isEmpty()) "0." else "$base.")
            else -> {
                val next = if (base == "0") key else base + key
                val decimals = next.substringAfter('.', "").length
                if (next.replace(".", "").length > 6 || decimals > 2) base else next
            }
        }
    }

    fun bump(delta: Double) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        val v = ((Units.parse(text) ?: 0.0) + delta).coerceIn(minValue, maxValue)
        text = Units.formatNumber(v, 2)
        fresh = false
    }

    val value = Units.parse(text)
    val valid = value != null && value >= minValue && value <= maxValue && (allowZero || value > 0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppTheme.colors.card,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            SheetHeader(title = title, onCancel = onDismiss)

            // The value display
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step != null) RoundKey("−") { bump(-step) } else Spacer(Modifier.width(56.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                    Text(
                        text.ifEmpty { "0" },
                        style = AppTheme.type.bigNumber.copy(fontSize = 60.sp),
                        color = if (text.isEmpty() || fresh) AppTheme.colors.secondaryLabel else AppTheme.colors.accent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { contentDescription = "$text $unit" },
                    )
                    if (unit.isNotEmpty()) {
                        Text(
                            " $unit",
                            style = AppTheme.type.title3,
                            color = AppTheme.colors.secondaryLabel,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                }
                if (step != null) RoundKey("+") { bump(step) } else Spacer(Modifier.width(56.dp))
            }

            extraContent()

            // Keys
            val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(if (allowDecimal) "." else "", "0", "⌫"))
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (key.isEmpty()) AppTheme.colors.card else AppTheme.colors.cardElevated)
                                    .clickable(enabled = key.isNotEmpty(), role = Role.Button) { press(key) }
                                    .semantics { contentDescription = if (key == "⌫") "Delete" else key },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (key == "⌫") {
                                    Icon(Icons.AutoMirrored.Rounded.Backspace, null, tint = AppTheme.colors.label)
                                } else {
                                    Text(key, style = AppTheme.type.title1.copy(fontWeight = FontWeight.SemiBold, fontSize = 26.sp), color = AppTheme.colors.label)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                text = confirmText,
                enabled = valid,
                onClick = { value?.let(onConfirm) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun RoundKey(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .width(56.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(AppTheme.colors.fill)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = AppTheme.type.title1.copy(fontWeight = FontWeight.Medium), color = AppTheme.colors.accent)
    }
}

/** "Cancel   Title   Done" header for sheets. */
@Composable
fun SheetHeader(
    title: String,
    onCancel: (() -> Unit)? = null,
    doneText: String? = null,
    doneEnabled: Boolean = true,
    onDone: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp)) {
        if (onCancel != null) {
            TextAction("Cancel", onCancel, modifier = Modifier.align(Alignment.CenterStart))
        }
        Text(title, style = AppTheme.type.headline, color = AppTheme.colors.label, modifier = Modifier.align(Alignment.Center))
        if (onDone != null && doneText != null) {
            TextAction(doneText, onDone, modifier = Modifier.align(Alignment.CenterEnd), enabled = doneEnabled, bold = true)
        }
    }
}
