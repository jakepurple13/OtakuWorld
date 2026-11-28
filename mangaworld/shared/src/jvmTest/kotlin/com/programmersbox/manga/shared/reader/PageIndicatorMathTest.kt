package com.programmersbox.manga.shared.reader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PageIndicatorMathTest {

    @Test
    fun displayPageIsOneBased() = assertEquals(3, displayPage(currentPage = 2, pages = 24))

    @Test
    fun displayPageCapsAtEndCard() = assertEquals(24, displayPage(currentPage = 24, pages = 24))

    @Test
    fun displayPageWithNoPagesIsOne() = assertEquals(1, displayPage(currentPage = 0, pages = 0))

    @Test
    fun railHasOneDotPerPageWhenShort() = assertEquals(24, railDotCount(24))

    @Test
    fun railBucketsLongChapters() = assertEquals(MAX_RAIL_DOTS, railDotCount(200))

    @Test
    fun railDotMapsPageToBucket() {
        assertEquals(0, railDotFor(page = 0, pages = 200))
        assertEquals(MAX_RAIL_DOTS - 1, railDotFor(page = 199, pages = 200))
        assertEquals(5, railDotFor(page = 5, pages = 24))
    }

    @Test
    fun railDotForEndCardStaysOnLastDot() = assertEquals(23, railDotFor(page = 24, pages = 24))

    @Test
    fun railFirstPageRoundTrips() {
        (0 until railDotCount(200)).forEach { dot ->
            assertEquals(dot, railDotFor(railFirstPage(dot, 200), 200))
        }
    }

    @Test
    fun segmentsForAnyLength() {
        assertTrue(usesSegments(1))
        assertTrue(usesSegments(500))
        assertFalse(usesSegments(0))
    }

    @Test
    fun oneSegmentPerPageUpToThirty() {
        assertEquals(1, pagesPerSegment(30))
        assertEquals(30, segmentCount(30))
    }

    @Test
    fun segmentCountCapsAtTwenty() {
        assertEquals(3, pagesPerSegment(60))
        assertEquals(20, segmentCount(60))
        assertTrue(segmentCount(250) <= MAX_GROUPED_SEGMENTS)
    }

    @Test
    fun segmentFillPartialInGroup() {
        // 60 pages, 3 per segment; index 4 = page 5 is the 2nd page of segment 1
        assertEquals(1f, segmentFill(0, 4, 60))
        assertEquals(2f / 3f, segmentFill(1, 4, 60), 0.001f)
        assertEquals(0f, segmentFill(2, 4, 60))
    }

    @Test
    fun segmentFillSinglePage() = assertEquals(1f, segmentFill(0, 0, 1))

    @Test
    fun segmentFillOnEndCard() = assertEquals(1f, segmentFill(segmentCount(24) - 1, 24, 24))

    @Test
    fun chipClampsAtEdges() {
        assertEquals(8f, clampedChipX(tip = 10f, chipWidth = 70f, width = 260f, margin = 8f))
        assertEquals(182f, clampedChipX(tip = 260f, chipWidth = 70f, width = 260f, margin = 8f))
        assertEquals(95f, clampedChipX(tip = 130f, chipWidth = 70f, width = 260f, margin = 8f))
    }

    @Test
    fun chipWiderThanScreenSitsAtMargin() = assertEquals(8f, clampedChipX(tip = 50f, chipWidth = 300f, width = 260f, margin = 8f))

    @Test
    fun ticksHiddenPastForty() {
        assertTrue(showScrubberTicks(40))
        assertFalse(showScrubberTicks(41))
        assertFalse(showScrubberTicks(1))
    }

    @Test
    fun folioAlternatesCorners() {
        assertTrue(folioOnStart(0))
        assertFalse(folioOnStart(1))
    }
}
