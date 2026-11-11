package com.programmersbox.manga.shared.reader

import io.kamel.core.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderMotionTest {

    @Test
    fun memoryHitStartsSettled() =
        assertEquals(RevealStart(alpha = 1f, scale = 1f), pageRevealStart(DataSource.Memory))

    @Test
    fun freshLoadStartsHiddenAndEnlarged() =
        assertEquals(RevealStart(alpha = 0f, scale = 1.04f), pageRevealStart(DataSource.Network))

    @Test
    fun positionCountsFromOldest() = assertEquals(43, chapterPosition(listIndex = 26, chapterCount = 69))

    @Test
    fun seriesProgressIsFraction() = assertEquals(43f / 69f, seriesProgress(26, 69), 0.0001f)

    @Test
    fun seriesProgressEmptyIsZero() = assertEquals(0f, seriesProgress(0, 0))
}
