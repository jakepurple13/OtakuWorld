package com.programmersbox.anime.shared.videoplayer

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayOptionsTest {
    @Test
    fun normalOpenPlaysAtOnce() {
        assertEquals(listOf(":no-sub-autodetect-file"), playOptions(startMs = 0, startPaused = false))
        assertEquals(listOf(":no-sub-autodetect-file", ":start-time=12.5"), playOptions(startMs = 12_500, startPaused = false))
    }

    @Test
    fun restoredOpenStartsPaused() {
        assertEquals(listOf(":no-sub-autodetect-file", ":start-paused"), playOptions(startMs = 0, startPaused = true))
        assertEquals(
            listOf(":no-sub-autodetect-file", ":start-time=3.0", ":start-paused"),
            playOptions(startMs = 3_000, startPaused = true),
        )
    }
}
