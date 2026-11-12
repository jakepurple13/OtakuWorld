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
    fun segmentsOnlyForShortChapters() {
        assertTrue(usesSegments(24))
        assertFalse(usesSegments(MAX_SEGMENTS + 1))
        assertFalse(usesSegments(0))
    }

    @Test
    fun folioAlternatesCorners() {
        assertTrue(folioOnStart(0))
        assertFalse(folioOnStart(1))
    }
}
