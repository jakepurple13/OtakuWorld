package com.programmersbox.anime.shared.videoplayer

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerChromeFormatTest {
    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("0:00", formatPlayerTime(0))
        assertEquals("5:42", formatPlayerTime(342_999))
        assertEquals("23:48", formatPlayerTime(1_428_000))
    }

    @Test
    fun formatsHours() {
        assertEquals("1:02:03", formatPlayerTime(3_723_000))
    }

    @Test
    fun negativeTimeIsZero() {
        assertEquals("0:00", formatPlayerTime(-5_000))
    }

    @Test
    fun formatsSpeed() {
        assertEquals("1×", formatSpeed(1f))
        assertEquals("1.25×", formatSpeed(1.25f))
        assertEquals("0.5×", formatSpeed(0.5f))
        assertEquals("2×", formatSpeed(2f))
    }

    @Test
    fun classicIsTheDefaultStyle() {
        assertEquals(PlayerStyle.Classic, PlayerStyle.entries.first())
    }
}
