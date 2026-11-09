package com.gymplanner.app.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** iOS-style alert with a text field (e.g. "New Day", "Rename Plan"). */
@Composable
fun TextInputDialog(
    title: String,
    message: String? = null,
    initial: String = "",
    placeholder: String = "",
    confirmText: String = "Save",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.cardElevated,
        shape = RoundedCornerShape(16.dp),
        title = { Text(title, style = AppTheme.type.headline, color = AppTheme.colors.label, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column {
                if (message != null) {
                    Text(message, style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { if (it.text.length <= 40) value = it },
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppTheme.colors.card,
                        unfocusedContainerColor = AppTheme.colors.card,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.text.trim()) }, enabled = value.text.isNotBlank()) {
                Text(confirmText, style = AppTheme.type.body.copy(fontWeight = FontWeight.SemiBold), color = if (value.text.isNotBlank()) AppTheme.colors.accent else AppTheme.colors.tertiaryLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", style = AppTheme.type.body, color = AppTheme.colors.accent) }
        },
    )
}

/** Confirmation alert. Destructive actions are shown in red. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    destructive: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.cardElevated,
        shape = RoundedCornerShape(16.dp),
        title = { Text(title, style = AppTheme.type.headline, color = AppTheme.colors.label, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = { Text(message, style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, style = AppTheme.type.body.copy(fontWeight = FontWeight.SemiBold), color = if (destructive) AppTheme.colors.red else AppTheme.colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", style = AppTheme.type.body, color = AppTheme.colors.accent) }
        },
    )
}

/** iOS-style action sheet: a list of choices in a bottom sheet. */
data class SheetAction(val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(title: String?, actions: List<SheetAction>, onDismiss: () -> Unit) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.background,
        dragHandle = null,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Spacer(Modifier.height(12.dp))
            GroupedSection(header = title) {
                actions.forEachIndexed { i, a ->
                    ListRow(
                        a.label,
                        titleColor = if (a.destructive) AppTheme.colors.red else AppTheme.colors.accent,
                        showChevron = false,
                        onClick = { onDismiss(); a.onClick() },
                    )
                    if (i < actions.lastIndex) RowDivider()
                }
            }
            Spacer(Modifier.height(10.dp))
            GroupedSection {
                ListRow("Cancel", titleColor = AppTheme.colors.accent, showChevron = false, onClick = onDismiss)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
