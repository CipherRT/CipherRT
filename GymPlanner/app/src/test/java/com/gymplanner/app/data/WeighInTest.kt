package com.gymplanner.app.data

import android.app.NotificationManager
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymplanner.app.GymApp
import com.gymplanner.app.core.model.Goal
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.feature.weighin.WeighInReceiver
import com.gymplanner.app.feature.weighin.WeighInScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WeighInTest {
    private val app: GymApp = ApplicationProvider.getApplicationContext()
    private val nm = app.getSystemService(NotificationManager::class.java)

    private fun fire(profileId: Long) {
        app.sendBroadcast(
            Intent(app, WeighInReceiver::class.java).setAction(WeighInScheduler.ACTION_PROMPT).putExtra(WeighInScheduler.EXTRA_PROFILE, profileId)
        )
        repeat(30) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(30) }
    }

    @Test
    fun promptOnlyWhenNotWeighedInToday() {
        while (!app.ready.value) Thread.sleep(20)
        val c = app.container
        // Creating a profile logs today's weight, so no prompt.
        val pid = runBlocking { c.profiles.create("Sam", 175.0, 70.0, UnitSystem.METRIC, Goal.CUT) }
        fire(pid)
        assertEquals(0, shadowOf(nm).allNotifications.size)

        // Remove today's entry: now the full-screen prompt is posted.
        runBlocking { c.weights.latest(pid)?.let { c.weights.delete(it) } }
        fire(pid)
        val n = shadowOf(nm).allNotifications.single()
        assertNotNull(n.fullScreenIntent)
        assertTrue(n.extras.getString("android.title")!!.contains("Sam"))

        // Tomorrow's alarm is set.
        assertTrue(shadowOf(app.getSystemService(android.app.AlarmManager::class.java)).scheduledAlarms.isNotEmpty())
    }
}
