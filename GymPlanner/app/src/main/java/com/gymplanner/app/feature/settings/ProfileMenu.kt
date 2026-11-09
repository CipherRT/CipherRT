package com.gymplanner.app.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.ProfileMenuTargets
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.ProfileAvatar
import com.gymplanner.app.core.ui.appContainer

/** The avatar in the top-right of every tab: switch profile, add profile, settings. */
@Composable
fun ProfileMenuButton(targets: ProfileMenuTargets) {
    val container = appContainer()
    val profiles by container.profiles.profiles.collectAsStateWithLifecycle(emptyList())
    val currentId by container.prefs.currentProfileId.collectAsStateWithLifecycle()
    val current = profiles.firstOrNull { it.id == currentId }
    var open by remember { mutableStateOf(false) }

    Box {
        ProfileAvatar(name = current?.name ?: "?", onClick = { open = true })
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = AppTheme.colors.cardElevated,
        ) {
            profiles.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.name, style = AppTheme.type.body, color = AppTheme.colors.label) },
                    leadingIcon = { ProfileAvatar(p.name, size = 28.dp) },
                    trailingIcon = {
                        if (p.id == currentId) Icon(Icons.Rounded.Check, "Current profile", tint = AppTheme.colors.accent)
                    },
                    onClick = {
                        open = false
                        if (p.id != currentId) container.profiles.switchTo(p.id)
                    },
                )
            }
            HorizontalDivider(color = AppTheme.colors.separator)
            DropdownMenuItem(
                text = { Text("Add Profile", style = AppTheme.type.body, color = AppTheme.colors.label) },
                leadingIcon = { Icon(Icons.Rounded.PersonAdd, null, tint = AppTheme.colors.label, modifier = Modifier.size(22.dp)) },
                onClick = { open = false; targets.onAddProfile() },
            )
            DropdownMenuItem(
                text = { Text("Settings", style = AppTheme.type.body, color = AppTheme.colors.label) },
                leadingIcon = { Icon(Icons.Rounded.Settings, null, tint = AppTheme.colors.label, modifier = Modifier.size(22.dp)) },
                onClick = { open = false; targets.onSettings() },
            )
        }
    }
}
