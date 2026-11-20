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

class AnimeMotionsTest {
    private val catalog = animeMotionCatalog()

    @Test
    fun launchesObeyContract() = catalog.launch.forEach {
        assertEquals(emptyList(), MotionContract.violations(it, MotionPhase.Launch), it.key)
    }

    @Test
    fun quitsObeyContract() = catalog.quit.forEach {
        assertEquals(emptyList(), MotionContract.violations(it, MotionPhase.Quit), it.key)
    }

    /** Portrait and very tall hosts: the TV and the push-in must still cover the window at the end. */
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
    fun crtIsDefault() {
        assertEquals("AnimeWorld", catalog.appName)
        assertEquals("crt-power-on", catalog.launch.first().key)
        assertEquals("crt-power-off", catalog.quit.first().key)
        assertEquals("CRT power-on", catalog.launch.first().title)
        assertEquals("CRT power-off", catalog.quit.first().title)
    }

    @Test
    fun allSixPresent() {
        assertEquals(listOf("crt-power-on", "play-button", "eyecatch"), catalog.launch.map { it.key })
        assertEquals(listOf("crt-power-off", "next-episode", "iris-out"), catalog.quit.map { it.key })
        assertEquals(listOf("CRT power-on", "Play button", "Eyecatch"), catalog.launch.map { it.title })
        assertEquals(listOf("CRT power-off", "Next episode", "Iris out"), catalog.quit.map { it.title })
    }

    @Test
    fun durationsMatchMockup() {
        assertEquals(2300, CrtPowerOn.durationMillis)
        assertEquals(1500, CrtPowerOff.durationMillis)
        assertEquals(1900, PlayButton.durationMillis)
        assertEquals(1900, Eyecatch.durationMillis)
        assertEquals(2300, NextEpisode.durationMillis)
        assertEquals(1700, IrisOut.durationMillis)
    }

    /** Mid-run frames that show each scene's key moment actually draws (not only its start and end). */
    @Test
    fun keyMomentsDraw() {
        // Power-on: the bright dot peaks at 380 ms in the middle of the dark screen.
        assertEquals(Color.White, centre(CrtPowerOn, 380))
        // Power-off: the picture has collapsed and the dot peaks at 500 ms.
        assertEquals(Color.White, centre(CrtPowerOff, 500))
        // Power-on: before the click, the room is dark around the TV.
        assertEquals(AnimeRoom, renderMotionFrame(CrtPowerOn, 0).pixel(2, 2))

        // 320×200 host: 1cqw = 3.2px.
        // Play button: at 600 ms the button has popped in; above the white triangle it is solid accent, and the
        // triangle sits on the centre. The app has not started opening.
        val play = renderMotionFrame(PlayButton, 600)
        assertEquals(AnimeAccent, play.pixel(160, 100 - 16))
        assertEquals(Color.White, play.pixel(160, 100))
        // Eyecatch: at 800 ms both slabs are home: accent across the top, paper across the bottom.
        val eyecatch = renderMotionFrame(Eyecatch, 800)
        assertEquals(AnimeAccent, eyecatch.pixel(160, 20))
        assertEquals(AnimeEyecatchPaper, eyecatch.pixel(160, 190))
        // Next episode: at 1200 ms the banner is in (inside its bottom padding, left of the text) and the app
        // is a sepia still, no longer pure red.
        val next = renderMotionFrame(NextEpisode, 1200)
        assertEquals(AnimeAccent, next.pixel(4, 171))
        val still = next.pixel(160, 100)
        assertTrue(still.red > still.green && still.green > still.blue && still.green > .2f, "still at 1200 ms is $still, expected sepia")
        // Iris out: at 800 ms the iris holds as a small spot: app in the centre, black towards the corners.
        val iris = renderMotionFrame(IrisOut, 800)
        assertEquals(Color.Red, iris.pixel(160, 100))
        assertEquals(Color.Black, iris.pixel(80, 50))
    }

    private fun centre(motion: DesktopMotion, time: Int) = renderMotionFrame(motion, time).let { it.pixel(it.width / 2, it.height / 2) }

    private fun ImageBitmap.pixel(x: Int, y: Int): Color {
        val buffer = IntArray(width * height)
        readPixels(buffer)
        return Color(buffer[y * width + x])
    }
}
