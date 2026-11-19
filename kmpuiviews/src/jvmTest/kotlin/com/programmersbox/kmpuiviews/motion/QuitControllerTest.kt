package com.programmersbox.kmpuiviews.motion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class QuitControllerTest {
    private var exits = 0
    private fun controller(motion: DesktopMotion, launch: MotionRun? = null) =
        QuitController({ QuitPlan(motion, QuitHost.Overlay) }, { launch }, { exits++ })

    @Test fun offExitsAtOnce() { controller(OffMotion).requestQuit(); assertEquals(1, exits) }
    @Test fun requestStartsPlaying() { val c = controller(FadeOutMotion); c.requestQuit(); assertIs<QuitController.State.Playing>(c.state); assertEquals(0, exits) }
    @Test fun secondRequestExitsNow() { val c = controller(FadeOutMotion); c.requestQuit(); c.requestQuit(); assertEquals(1, exits) }
    @Test fun finishedExitsOnce() { val c = controller(FadeOutMotion); c.requestQuit(); c.finished(); c.finished(); assertEquals(1, exits) }
    @Test fun quitDuringLaunchSkipsLaunch() { val l = MotionRun(FadeInMotion); controller(FadeOutMotion, l).requestQuit(); assertTrue(l.finished) }
    @Test fun hidesOnFirstFrame() { val c = controller(FadeOutMotion); c.requestQuit(); assertFalse(c.hideMainWindow); c.overlayFirstFrame(); assertTrue(c.hideMainWindow) }
}
