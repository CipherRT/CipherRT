package com.gymplanner.app.screens

import com.gymplanner.app.ProfileMenuTargets
import com.gymplanner.app.feature.track.CardioEntryScreen
import com.gymplanner.app.feature.track.TrackScreen
import com.gymplanner.app.feature.track.TrackSection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TrackShots : ScreenshotBase() {
    @Test fun cardioEntry() {
        val c = Seed.basic()
        val id = runBlocking { c.track.cardio(c.prefs.currentProfileId.value!!).first().first { it.machine == "bike" }.id }
        snap("cardio_entry") { CardioEntryScreen(id) {} }
    }

    @Test fun cardioTab() {
        Seed.basic()
        snap("track_cardio") { TrackScreen(ProfileMenuTargets({}, {}), {}, TrackSection.CARDIO) }
    }

    @Test fun weightTab() {
        Seed.basic()
        snap("track_weight") { TrackScreen(ProfileMenuTargets({}, {}), {}, TrackSection.WEIGHT) }
    }
}
