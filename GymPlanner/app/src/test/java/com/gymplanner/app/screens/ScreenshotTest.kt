package com.gymplanner.app.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymplanner.app.core.ui.Accent
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.GymTheme
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.os.Looper
import java.io.File

/**
 * Renders screens to PNG files in build/screenshots so the design can be checked
 * without a phone. Only runs with -Pscreenshots (see app/build.gradle.kts).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
abstract class ScreenshotBase {
    protected fun snap(name: String, content: @Composable () -> Unit) {
        assumeTrue(System.getProperty("screenshots") == "true")
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            GymTheme(Accent.LIME) { Box(Modifier.fillMaxSize().background(AppTheme.colors.background)) { content() } }
        }
        repeat(40) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(100) }
        val view: View = activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        val out = File(System.getProperty("screenshotDir") ?: "build/screenshots").apply { mkdirs() }
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        controller.pause().stop().destroy()
    }
}
