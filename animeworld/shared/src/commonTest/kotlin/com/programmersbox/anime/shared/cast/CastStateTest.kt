package com.programmersbox.anime.shared.cast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CastStateTest {

    private val tracks = listOf(
        CastTrack(1, "English", "en", "source"),
        CastTrack(2, "Spanish", "es", "source"),
    )

    private fun status() = CastEvent.Status(
        showTitle = "Show",
        episodeName = "Ep 1",
        posterUrl = "https://p/img.jpg",
        positionMs = 12_000,
        durationMs = 1_440_000,
        playing = true,
        tracks = tracks,
        activeTrackId = 2,
        volume = 0.4f,
        currentCue = "Hola",
    )

    @Test
    fun connectedSetsDeviceName() {
        val state = reduce(CastUiState(connection = CastConnection.Connecting), CastEvent.Connected("Living Room TV"))
        assertEquals(CastConnection.Connected, state.connection)
        assertEquals("Living Room TV", state.deviceName)
    }

    @Test
    fun statusUpdatesPositionAndTracks() {
        val connected = reduce(CastUiState(), CastEvent.Connected("TV"))
        val state = reduce(connected, status())
        assertEquals(CastConnection.Connected, state.connection)
        assertEquals("TV", state.deviceName)
        assertEquals("Show", state.showTitle)
        assertEquals("Ep 1", state.episodeName)
        assertEquals("https://p/img.jpg", state.posterUrl)
        assertEquals(12_000, state.positionMs)
        assertEquals(1_440_000, state.durationMs)
        assertTrue(state.playing)
        assertEquals(tracks, state.tracks)
        assertEquals(2L, state.activeTrackId)
        assertEquals(0.4f, state.volume)
        assertEquals("Hola", state.currentCue)
    }

    @Test
    fun disconnectClearsMedia() {
        val playing = reduce(reduce(CastUiState(), CastEvent.Connected("TV")), status())
        val state = reduce(playing, CastEvent.Disconnected)
        assertEquals(CastConnection.Disconnected, state.connection)
        assertNull(state.deviceName)
        assertNull(state.showTitle)
        assertNull(state.episodeName)
        assertNull(state.posterUrl)
        assertEquals(0, state.positionMs)
        assertEquals(0, state.durationMs)
        assertFalse(state.playing)
        assertEquals(emptyList(), state.tracks)
        assertNull(state.activeTrackId)
        assertNull(state.currentCue)
    }
}
