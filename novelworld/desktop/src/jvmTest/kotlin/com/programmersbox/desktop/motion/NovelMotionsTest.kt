package com.programmersbox.desktop.motion

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.programmersbox.kmpuiviews.motion.MotionContract
import com.programmersbox.kmpuiviews.motion.MotionPhase
import com.programmersbox.kmpuiviews.motion.renderMotionFrame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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
    fun allSixPresent() {
        assertEquals(listOf("book-opening", "typewriter", "ink-drop"), catalog.launch.map { it.key })
        assertEquals(listOf("book-closing", "the-end", "lamp-out"), catalog.quit.map { it.key })
        assertEquals(listOf("Book opening", "Typewriter", "Ink drop"), catalog.launch.map { it.title })
        assertEquals(listOf("Book closing", "The End", "Lamp out"), catalog.quit.map { it.title })
    }

    @Test
    fun durationsMatchMockup() {
        assertEquals(2400, BookOpening.durationMillis)
        assertEquals(2300, BookClosing.durationMillis)
        assertEquals(2600, Typewriter.durationMillis)
        assertEquals(1900, InkDrop.durationMillis)
        assertEquals(2400, TheEnd.durationMillis)
        assertEquals(2200, LampOut.durationMillis)
    }

    /** Mid-run frames that show each scene's key moment actually draws (not only its start and end). */
    @Test
    fun keyMomentsDraw() {
        // 320×200 host: 1cqw = 3.2px; the book is 30cqw × 40cqw at (35cqw, 11.25cqw) = (112..208, 36..164).
        // Book opening at 450 ms: the closed book has risen in; above the title the front cover is plain green,
        // and the desk shows outside the book.
        val closed = renderMotionFrame(BookOpening, 450)
        assertEquals(BookCover, closed.pixel(160, 50))
        assertDesk(closed.pixel(40, 100))
        // Book opening at 1450 ms: the cover has swung open past 90° and the book has slid 15cqw right, so the
        // inside of the cover lies left of the spine (5..35cqw + 15cqw = 64..160px).
        val open = renderMotionFrame(BookOpening, 1450)
        assertEquals(BookCoverInside, open.pixel(100, 100))
        // Book closing at 1700 ms: the cover has swung shut and the book is back in the middle, in front of a
        // faded-out app.
        val shut = renderMotionFrame(BookClosing, 1700)
        assertEquals(BookCover, shut.pixel(160, 50))
        assertDesk(shut.pixel(40, 100))
        // Book closing at 1200 ms: halfway through the swing the cover is edge-on, so the pages show, here below
        // the text lines (the book has slid back 7.5cqw: pages at 136..232px).
        assertEquals(BookPages, renderMotionFrame(BookClosing, 1200).pixel(200, 145))
    }

    /** The typed lines grow by whole characters on the mockup's `steps(n)` timing. */
    @Test
    fun typewriterTypesStepByStep() {
        // 320×200: `.tw .l1` sits at (12cqw, 20cqw) = (38.4, 64) and is 7cqw × 1.1 = 24.6px tall. At 600 ms only
        // floor(10 × 400 / 900) = 4 of its 10 characters are typed, so nothing inks the right half of the line.
        val early = renderMotionFrame(Typewriter, 600)
        assertEquals(NovelPaperCream, early.pixel(300, 5))
        assertEquals(0, early.inkPixels(130..300, 66..86))
        // At 1100 ms all of "NovelWorld" is typed.
        assertTrue(renderMotionFrame(Typewriter, 1100).inkPixels(130..300, 66..86) > 0)
        // At 2300 ms the sheet has fed halfway up and out, uncovering the bottom of the app.
        val feeding = renderMotionFrame(Typewriter, 2300)
        assertEquals(Color.Red, feeding.pixel(160, 190))
        assertEquals(NovelPaperCream, feeding.pixel(300, 50))
    }

    /** Mid-spread the ink stain covers the paper while the app opens through a smaller circle at its centre. */
    @Test
    fun inkDropSpreads() {
        val spread = renderMotionFrame(InkDrop, 1300)
        assertEquals(Color.Red, spread.pixel(160, 100))
        assertEquals(NovelInk, spread.pixel(10, 10))
        // Before the drop lands the title sits on ruled paper.
        assertEquals(NovelPaperCream, renderMotionFrame(InkDrop, 500).pixel(300, 5))
    }

    /** "The End." types out over the paper (8 steps from 650 ms). */
    @Test
    fun theEndTypesOut() {
        // `.end` sits at top 40% = 80px; its 7cqw text line is 24.6px tall.
        val before = renderMotionFrame(TheEnd, 640)
        assertEquals(NovelPaperCream, before.pixel(300, 5))
        assertEquals(0, before.inkPixels(0..319, 82..102))
        assertTrue(renderMotionFrame(TheEnd, 1300).inkPixels(0..319, 82..102) > 0)
    }

    /** Darkness has closed in on the corners while the centre still shows the app warmed to sepia; then the flame. */
    @Test
    fun lampOutDarkens() {
        val dim = renderMotionFrame(LampOut, 1000)
        assertEquals(NovelLampDark, dim.pixel(5, 5))
        val warmed = dim.pixel(160, 150)
        assertNotEquals(Color.Red, warmed)
        assertEquals(1f, warmed.alpha)
        assertTrue(warmed.green > .05f, "sepia lifts green: $warmed")
        // At 1320 ms the flame is fully lit: its hot core is pale yellow at the gradient centre.
        val core = renderMotionFrame(LampOut, 1320).pixel(160, 110)
        assertTrue(core.red > .9f && core.green > .8f && core.blue < core.red, "flame core: $core")
    }

    /** The desk draws there: opaque and not the app. */
    private fun assertDesk(pixel: Color) {
        assertNotEquals(Color.Red, pixel)
        assertTrue(pixel.alpha > 0f, "desk is drawn: $pixel")
    }

    /** Pixels in the area dark enough to be the ink (`#2a2219`), not paper or ruling. */
    private fun ImageBitmap.inkPixels(xs: IntRange, ys: IntRange): Int {
        val buffer = IntArray(width * height)
        readPixels(buffer)
        var count = 0
        for (y in ys) for (x in xs) {
            val c = Color(buffer[y * width + x])
            if (c.alpha > .5f && c.red + c.green + c.blue < 1.2f) count++
        }
        return count
    }

    private fun ImageBitmap.pixel(x: Int, y: Int): Color {
        val buffer = IntArray(width * height)
        readPixels(buffer)
        return Color(buffer[y * width + x])
    }
}
