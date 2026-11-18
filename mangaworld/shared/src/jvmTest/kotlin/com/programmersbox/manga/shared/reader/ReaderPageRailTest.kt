package com.programmersbox.manga.shared.reader

import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderPageRailTest {

    @Test
    fun `thumb size follows the visible share of the items`() {
        val thumb = railThumb(count = 10, position = 0f, visible = 2f, trackPx = 500f, minThumbPx = 20f)
        assertEquals(100f, thumb.sizePx, 0.01f)
        assertEquals(0f, thumb.offsetPx, 0.01f)
    }

    @Test
    fun `thumb sits at the end of the track at the last position`() {
        val thumb = railThumb(count = 10, position = 8f, visible = 2f, trackPx = 500f, minThumbPx = 20f)
        assertEquals(400f, thumb.offsetPx, 0.01f)
    }

    @Test
    fun `thumb never gets smaller than the minimum`() {
        val thumb = railThumb(count = 1000, position = 500f, visible = 1f, trackPx = 500f, minThumbPx = 20f)
        assertEquals(20f, thumb.sizePx, 0.01f)
        assertEquals(240f, thumb.offsetPx, 0.5f)
    }

    @Test
    fun `thumb clamps positions past the end`() {
        val thumb = railThumb(count = 10, position = 50f, visible = 2f, trackPx = 500f, minThumbPx = 20f)
        assertEquals(400f, thumb.offsetPx, 0.01f)
    }

    @Test
    fun `dragging the thumb maps back to a position`() {
        assertEquals(0f, railPositionForThumb(thumbOffsetPx = -10f, count = 10, visible = 2f, trackPx = 500f, thumbPx = 100f))
        assertEquals(4f, railPositionForThumb(thumbOffsetPx = 200f, count = 10, visible = 2f, trackPx = 500f, thumbPx = 100f), 0.01f)
        assertEquals(8f, railPositionForThumb(thumbOffsetPx = 900f, count = 10, visible = 2f, trackPx = 500f, thumbPx = 100f), 0.01f)
    }

    @Test
    fun `clicking the track picks the item under the pointer`() {
        assertEquals(0, railIndexAt(yPx = 0f, trackPx = 500f, count = 10))
        assertEquals(3, railIndexAt(yPx = 175f, trackPx = 500f, count = 10))
        assertEquals(9, railIndexAt(yPx = 600f, trackPx = 500f, count = 10))
    }
}
