package com.gymplanner.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.IntentCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.core.ui.DarkAppColors
import com.gymplanner.app.core.ui.GymTheme
import com.gymplanner.app.core.ui.LightAppColors
import com.gymplanner.app.core.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** A plan file opened from another app, waiting to be shown in the import preview. */
    private val pendingImport = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as GymApp
        handleIntent(intent)

        setContent {
            val themeMode by app.container.prefs.theme.collectAsStateWithLifecycle()
            val dark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
            LaunchedEffect(dark) {
                val bg = (if (dark) DarkAppColors else LightAppColors).background.toArgb()
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(bg) else SystemBarStyle.light(bg, bg),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(bg) else SystemBarStyle.light(bg, bg),
                )
                window.setBackgroundDrawable(bg.toDrawable())
            }
            GymTheme(themeMode) {
                AppRoot(pendingImport = pendingImport)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }
        if (uri != null) pendingImport.value = uri
    }
}
