package com.programmersbox.desktop.motion

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.programmersbox.kmpuiviews.motion.DesktopMotion
import com.programmersbox.kmpuiviews.motion.MotionContract
import com.programmersbox.kmpuiviews.motion.MotionPhase
import com.programmersbox.kmpuiviews.motion.renderMotionFrame
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun durationsMatchMockup() {
        assertEquals(2300, CrtPowerOn.durationMillis)
        assertEquals(1500, CrtPowerOff.durationMillis)
    }

    /** Mid-run frames that show each scene's key moment actually draws (not only its start and end). */
    @Test
    fun keyMomentsDraw() {
        // Power-on: the bright dot peaks at 380 ms in the middle of the dark screen.
        assertEquals(Color.White, centre(CrtPowerOn, 380))
        // Power-off: the picture has collapsed and the dot peaks at 500 ms.
        assertEquals(Color.White, centre(CrtPowerOff, 500))
        // Power-on: before the click, the room is dark around the TV.
        assertEquals(CrtRoom, renderMotionFrame(CrtPowerOn, 0).pixel(2, 2))
    }

    private fun centre(motion: DesktopMotion, time: Int) = renderMotionFrame(motion, time).let { it.pixel(it.width / 2, it.height / 2) }

    private fun ImageBitmap.pixel(x: Int, y: Int): Color {
        val buffer = IntArray(width * height)
        readPixels(buffer)
        return Color(buffer[y * width + x])
    }
}
