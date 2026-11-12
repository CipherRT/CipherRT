package com.gymplanner.app.feature.weighin

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.gymplanner.app.core.ui.ConfirmDialog
import com.gymplanner.app.core.ui.appContainer

object WeighInPermission {
    /** Android 14+ lets people turn full-screen alerts off per app. */
    fun canFullScreen(context: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun openFullScreenSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= 34) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}

/**
 * Asks once (if the weigh-in is on) for notifications and, on Android 14+, full-screen alerts.
 * If either is refused, the weigh-in still arrives as a normal high-priority notification.
 */
@Composable
fun WeighInPermissionPrompt(weighInEnabled: Boolean) {
    val context = LocalContext.current
    val prefs = appContainer().prefs
    var askFullScreen by remember { mutableStateOf(false) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!WeighInPermission.canFullScreen(context)) askFullScreen = true
    }
    LaunchedEffect(weighInEnabled) {
        if (!weighInEnabled || prefs.fullScreenAsked) return@LaunchedEffect
        val needNotif = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        val needFullScreen = !WeighInPermission.canFullScreen(context)
        if (!needNotif && !needFullScreen) return@LaunchedEffect
        prefs.fullScreenAsked = true
        if (needNotif) notifications.launch(Manifest.permission.POST_NOTIFICATIONS) else askFullScreen = true
    }
    if (askFullScreen) {
        ConfirmDialog(
            title = "Show the morning weigh-in?",
            message = "To show the weigh-in over the lock screen like an alarm, allow full-screen alerts for Gym Planner on the next screen. Otherwise it arrives as a normal notification.",
            confirmText = "Allow",
            destructive = false,
            onDismiss = { askFullScreen = false },
            onConfirm = { askFullScreen = false; WeighInPermission.openFullScreenSettings(context) },
        )
    }
}
