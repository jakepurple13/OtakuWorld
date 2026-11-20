package com.programmersbox.desktop.motion

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.programmersbox.kmpuiviews.motion.MotionContract
import com.programmersbox.kmpuiviews.motion.MotionPhase
import com.programmersbox.kmpuiviews.motion.renderMotionFrame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NovelMotionsTest {
    private val catalog = novelMotionCatalog()

    @Test
    fun launchesObeyContract() = catalog.launch.forEach {
        assertEquals(emptyList(), MotionContract.violations(it, MotionPhase.Launch), it.key)
    }

    @Test
    fun quitsObeyContract() = catalog.quit.forEach {
        assertEquals(emptyList(), MotionContract.violations(it, MotionPhase.Quit), it.key)
    }

    /** Portrait and very tall hosts: the book must stay centred and still clear away at the end. */
    @Test
    fun tallWindowsObeyContract() = listOf(200 to 320, 200 to 500).forEach { (w, h) ->
        catalog.launch.forEach {
            assertEquals(emptyList(), MotionContract.violations(it, MotionPhase.Launch, w, h), "${it.key} ${w}x$h")
        }
        catalog.quit.forEach {
            assertEquals(emptyList(), MotionContract.violations(it, MotionPhase.Quit, w, h), "${it.key} ${w}x$h")
        }
    }

    @Test
    fun bookIsDefault() {
        assertEquals("NovelWorld", catalog.appName)
        assertEquals("book-opening", catalog.launch.first().key)
        assertEquals("book-closing", catalog.quit.first().key)
        assertEquals("Book opening", catalog.launch.first().title)
        assertEquals("Book closing", catalog.quit.first().title)
    }

    @Test
    fun durationsMatchMockup() {
        assertEquals(2400, BookOpening.durationMillis)
        assertEquals(2300, BookClosing.durationMillis)
    }

    /** Mid-run frames that show each scene's key moment actually draws (not only its start and end). */
    @Test
    fun keyMomentsDraw() {
        // 320×200 host: 1cqw = 3.2px; the book is 30cqw × 40cqw at (35cqw, 11.25cqw) = (112..208, 36..164).
        // Book opening at 450 ms: the closed book has risen in; above the title the front cover is plain green,
        // and the desk shows outside the book.
        val closed = renderMotionFrame(BookOpening, 450)
        assertEquals(BookCover, closed.pixel(160, 50))
        assertNotEquals(Color.Red, closed.pixel(40, 100))
        // Book opening at 1450 ms: the cover has swung open past 90° and the book has slid 15cqw right, so the
        // inside of the cover lies left of the spine (5..35cqw + 15cqw = 64..160px).
        val open = renderMotionFrame(BookOpening, 1450)
        assertEquals(BookCoverInside, open.pixel(100, 100))
        // Book closing at 1700 ms: the cover has swung shut and the book is back in the middle, in front of a
        // faded-out app.
        val shut = renderMotionFrame(BookClosing, 1700)
        assertEquals(BookCover, shut.pixel(160, 50))
        assertNotEquals(Color.Red, shut.pixel(40, 100))
        // Book closing at 1200 ms: halfway through the swing the cover is edge-on, so the pages show, here below
        // the text lines (the book has slid back 7.5cqw: pages at 136..232px).
        assertEquals(BookPages, renderMotionFrame(BookClosing, 1200).pixel(200, 145))
    }

    private fun ImageBitmap.pixel(x: Int, y: Int): Color {
        val buffer = IntArray(width * height)
        readPixels(buffer)
        return Color(buffer[y * width + x])
    }
}
