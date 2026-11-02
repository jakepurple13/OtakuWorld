package com.programmersbox.manga.shared.reader

import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.mangasettings.ReaderType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReaderWidthTest {

    @Test
    fun `side padding splits the unused width evenly`() {
        assertEquals(200f, readerSidePadding(1000.dp, 0.6f).value, 0.001f)
        assertEquals(0.dp, readerSidePadding(1000.dp, 1f))
    }

    @Test
    fun `side padding clamps fractions below the minimum`() {
        assertEquals(readerSidePadding(1000.dp, MIN_READER_WIDTH_FRACTION), readerSidePadding(1000.dp, 0.1f))
    }

    @Test
    fun `divider position mirrors around the center`() {
        assertEquals(200f, dividerPosition(0.6f, containerPx = 1000f, edgeInsetPx = 40f, isLeftDivider = true), 0.001f)
        assertEquals(800f, dividerPosition(0.6f, containerPx = 1000f, edgeInsetPx = 40f, isLeftDivider = false), 0.001f)
    }

    @Test
    fun `divider stays out of the edge inset at full width`() {
        assertEquals(40f, dividerPosition(1f, containerPx = 1000f, edgeInsetPx = 40f, isLeftDivider = true))
        assertEquals(960f, dividerPosition(1f, containerPx = 1000f, edgeInsetPx = 40f, isLeftDivider = false))
    }

    @Test
    fun `either divider gives the same fraction for mirrored positions`() {
        assertEquals(0.6f, fractionForDividerPosition(200f, containerPx = 1000f, edgeInsetPx = 40f), 0.0001f)
        assertEquals(0.6f, fractionForDividerPosition(800f, containerPx = 1000f, edgeInsetPx = 40f), 0.0001f)
    }

    @Test
    fun `dragging into the edge inset means full width`() {
        assertEquals(1f, fractionForDividerPosition(40f, containerPx = 1000f, edgeInsetPx = 40f))
        assertEquals(1f, fractionForDividerPosition(-50f, containerPx = 1000f, edgeInsetPx = 40f))
        assertEquals(1f, fractionForDividerPosition(990f, containerPx = 1000f, edgeInsetPx = 40f))
    }

    @Test
    fun `dragging past the center clamps to the minimum width`() {
        assertEquals(MIN_READER_WIDTH_FRACTION, fractionForDividerPosition(500f, containerPx = 1000f, edgeInsetPx = 40f))
        assertEquals(MIN_READER_WIDTH_FRACTION, fractionForDividerPosition(600f, containerPx = 1000f, edgeInsetPx = 40f))
    }

    @Test
    fun `fraction is full width before the container is measured`() {
        assertEquals(1f, fractionForDividerPosition(100f, containerPx = 0f, edgeInsetPx = 40f))
    }

    @Test
    fun `only the list reader supports width adjust`() {
        assertTrue(ReaderType.List.supportsWidthAdjust)
        assertFalse(ReaderType.Pager.supportsWidthAdjust)
        assertFalse(ReaderType.FlipPager.supportsWidthAdjust)
        assertFalse(ReaderType.CurlPager.supportsWidthAdjust)
    }
}
