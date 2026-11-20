package com.programmersbox.desktop.motion

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.programmersbox.kmpuiviews.motion.DesktopMotion
import com.programmersbox.kmpuiviews.motion.MotionContract
import com.programmersbox.kmpuiviews.motion.MotionPhase
import com.programmersbox.kmpuiviews.motion.renderMotionFrame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MangaMotionsTest {
    private val catalog = mangaMotionCatalog()

    @Test
    fun launchesObeyContract() = catalog.launch.forEach {
        assertEquals(emptyList(), MotionContract.violations(it, MotionPhase.Launch), it.key)
    }

    @Test
    fun quitsObeyContract() = catalog.quit.forEach {
        assertEquals(emptyList(), MotionContract.violations(it, MotionPhase.Quit), it.key)
    }

    /** Portrait and very tall hosts: vertical moves must still clear the window. */
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
    fun sushiIsDefault() {
        assertEquals("sushi-unroll", catalog.launch.first().key)
        assertEquals("sushi-roll-up", catalog.quit.first().key)
    }

    @Test
    fun allSixPresent() {
        assertEquals(listOf("sushi-unroll", "ink-panels", "impact"), catalog.launch.map { it.key })
        assertEquals(listOf("sushi-roll-up", "screentone-fade", "the-end-panel"), catalog.quit.map { it.key })
        assertEquals(listOf("Sushi unroll", "Ink panels", "Impact"), catalog.launch.map { it.title })
        assertEquals(listOf("Sushi roll-up", "Screentone fade", "The End panel"), catalog.quit.map { it.title })
    }

    @Test
    fun durationsMatchMockup() {
        assertEquals(3200, SushiUnroll.durationMillis)
        assertEquals(3100, SushiRollUp.durationMillis)
        assertEquals(2600, InkPanels.durationMillis)
        assertEquals(2200, Impact.durationMillis)
        assertEquals(2400, ScreentoneFade.durationMillis)
        assertEquals(2300, TheEndPanel.durationMillis)
    }

    /** Mid-run frames that show each scene's key moment actually draws (not only its start and end). */
    @Test
    fun keyMomentsDraw() {
        // Ink panels: page is paper before any panel fades in.
        assertEquals(Paper, centre(InkPanels, 400))
        // Impact: the white flash peaks at 1650 ms.
        assertEquals(Color.White, centre(Impact, 1650))
        // Screentone fade: dots have grown into solid black and the app is gone (corner: おわり sits in the centre).
        val tone = renderMotionFrame(ScreentoneFade, 1320).pixel(5, 5)
        assertTrue(tone.alpha == 1f && maxOf(tone.red, tone.green, tone.blue) < .1f, "tone at 1320 ms is $tone, expected solid near-black")
        // The End panel: the shrunken app leaves paper around it.
        assertEquals(Paper, renderMotionFrame(TheEndPanel, 1200).pixel(5, 5))
    }

    private fun centre(motion: DesktopMotion, time: Int) = renderMotionFrame(motion, time).let { it.pixel(it.width / 2, it.height / 2) }

    private fun ImageBitmap.pixel(x: Int, y: Int): Color {
        val buffer = IntArray(width * height)
        readPixels(buffer)
        return Color(buffer[y * width + x])
    }
}
