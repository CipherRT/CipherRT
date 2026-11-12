package com.gymplanner.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.IntentCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymplanner.app.core.ui.Accent
import com.gymplanner.app.core.ui.AppColors
import com.gymplanner.app.core.ui.GymTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** A plan file opened from another app, waiting to be shown in the import preview. */
    private val pendingImport = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as GymApp
        handleIntent(intent)
        val bg = AppColors(accent = Accent.LIME.color).background.toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(bg),
            navigationBarStyle = SystemBarStyle.dark(AppColors(accent = Accent.LIME.color).bottomBar.toArgb()),
        )
        window.setBackgroundDrawable(bg.toDrawable())

        setContent {
            val accent by app.container.prefs.accent.collectAsStateWithLifecycle()
            GymTheme(accent) {
                AppRoot(pendingImport = pendingImport)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        val bg = AppColors(accent = Accent.LIME.color).background.toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(bg),
            navigationBarStyle = SystemBarStyle.dark(AppColors(accent = Accent.LIME.color).bottomBar.toArgb()),
        )
        window.setBackgroundDrawable(bg.toDrawable())
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
