package com.programmersbox.anime.shared.videoplayer

import kotlin.test.Test
import kotlin.test.assertEquals

class BufferPercentTest {
    @Test
    fun nothingBuffered() = assertEquals(0, bufferPercent(0, 0))

    @Test
    fun halfOfTarget() = assertEquals(50, bufferPercent(2_500, 0))

    @Test
    fun measuredAheadOfPlayhead() = assertEquals(40, bufferPercent(12_000, 10_000))

    @Test
    fun clampedToHundred() = assertEquals(100, bufferPercent(20_000, 0))

    @Test
    fun bufferBehindPlayheadClampsToZero() = assertEquals(0, bufferPercent(500, 1_000))
}
