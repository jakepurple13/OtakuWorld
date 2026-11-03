package com.programmersbox.supabaseintegration.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatDurationTest {
    @Test
    fun defaultSyncConfigValues() {
        assertEquals("5 min", formatDuration(5 * 60 * 1000L))
        assertEquals("1 s", formatDuration(1_000L))
        assertEquals("30 s", formatDuration(30_000L))
    }

    @Test
    fun valuesThatDoNotDivideEvenly() {
        assertEquals("90 s", formatDuration(90_000L))
        assertEquals("1500 ms", formatDuration(1_500L))
        assertEquals("0 ms", formatDuration(0L))
    }
}
