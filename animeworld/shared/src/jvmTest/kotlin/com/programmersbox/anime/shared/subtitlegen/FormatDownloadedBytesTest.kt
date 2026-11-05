package com.programmersbox.anime.shared.subtitlegen

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatDownloadedBytesTest {
    @Test
    fun formats() {
        assertEquals("0 MB", formatDownloadedBytes(0))
        assertEquals("466 MB", formatDownloadedBytes(466_000_000))
        assertEquals("1.5 GB", formatDownloadedBytes(1_530_000_000))
    }
}
