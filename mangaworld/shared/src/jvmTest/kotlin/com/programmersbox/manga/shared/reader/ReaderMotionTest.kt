package com.programmersbox.manga.shared.reader

import com.programmersbox.datastore.mangasettings.ReaderType
import io.kamel.core.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun progressIsFraction() = assertEquals(0.5f, chapterProgress(currentPage = 11, pages = 24))

    @Test
    fun progressClamps() {
        assertEquals(1f, chapterProgress(30, 24))
        assertEquals(0f, chapterProgress(0, 0))
    }

    @Test
    fun onlyListAndPagerMorph() {
        assertTrue(morphsPages(ReaderType.List, ReaderType.Pager))
        assertTrue(morphsPages(ReaderType.Pager, ReaderType.List))
        assertFalse(morphsPages(ReaderType.List, ReaderType.FlipPager))
        assertFalse(morphsPages(ReaderType.CurlPager, ReaderType.Pager))
    }
}
