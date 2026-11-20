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

    @Test fun offExitsAtOnce() { val c = controller(OffMotion); c.requestQuit(); assertEquals(1, exits); assertEquals(QuitController.State.Exited, c.state) }
    @Test fun requestStartsPlaying() { val c = controller(FadeOutMotion); c.requestQuit(); assertIs<QuitController.State.Playing>(c.state); assertEquals(0, exits) }
    @Test fun secondRequestExitsNow() { val c = controller(FadeOutMotion); c.requestQuit(); c.requestQuit(); assertEquals(1, exits); assertEquals(QuitController.State.Exited, c.state) }
    @Test fun finishedExitsOnce() { val c = controller(FadeOutMotion); c.requestQuit(); c.finished(); c.finished(); assertEquals(1, exits); assertEquals(QuitController.State.Exited, c.state) }
    @Test fun quitDuringLaunchSkipsLaunch() { val l = MotionRun(FadeInMotion); controller(FadeOutMotion, l).requestQuit(); assertTrue(l.finished) }
    @Test fun hidesOnFirstFrame() { val c = controller(FadeOutMotion); c.requestQuit(); assertFalse(c.hideMainWindow); c.overlayFirstFrame(); assertTrue(c.hideMainWindow) }
    @Test fun requestAfterExitIsNoOp() { val c = controller(OffMotion); c.requestQuit(); c.requestQuit(); c.requestQuit(); assertEquals(1, exits); assertEquals(QuitController.State.Exited, c.state) }
    @Test fun planEvaluatedOncePerQuit() {
        var plans = 0
        val c = QuitController({ plans++; QuitPlan(FadeOutMotion, QuitHost.Overlay) }, { null }, { exits++ })
        c.requestQuit(); c.finished()
        assertEquals(1, plans)
    }

    // PendingQuitReply wired as in DesktopUi: exit = exitApplication (never releases); desktopSetup wraps the app loop in
    // reply.releaseAfter { application { ... } }, so the OS is answered only after the loop (and all disposal) ends.
    private val reply = PendingQuitReply()
    private var replies = 0
    private val events = mutableListOf<String>()
    private fun replying(motion: DesktopMotion) =
        QuitController({ QuitPlan(motion, QuitHost.Overlay) }, { null }, { exits++; events += "exit" })
    private fun osQuit(c: QuitController) {
        reply.hold { replies++; events += "reply" }
        c.requestQuit()
    }
    private fun appLoop(body: () -> Unit) = reply.releaseAfter { body(); events += "loopEnd" }

    @Test fun osQuitWithOffRepliesAfterLoop() {
        appLoop { osQuit(replying(OffMotion)); assertEquals(0, replies) }
        assertEquals(1, exits); assertEquals(listOf("exit", "loopEnd", "reply"), events)
    }
    @Test fun osQuitRepliesOnlyAfterMotionAndLoop() {
        appLoop {
            val c = replying(FadeOutMotion); osQuit(c)
            assertEquals(0, replies)
            c.finished()
            assertEquals(0, replies)
        }
        assertEquals(listOf("exit", "loopEnd", "reply"), events)
    }
    @Test fun closeButtonExitsWithoutReply() {
        appLoop { replying(OffMotion).requestQuit() }
        assertEquals(1, exits); assertEquals(0, replies)
    }
    @Test fun osQuitWhilePlayingExitsAndRepliesOnce() {
        appLoop { val c = replying(FadeOutMotion); osQuit(c); osQuit(c); c.finished() }
        assertEquals(1, exits); assertEquals(1, replies)
    }
    @Test fun osQuitAfterExitStillRepliesAfterLoop() {
        appLoop { val c = replying(OffMotion); c.requestQuit(); osQuit(c); assertEquals(0, replies) }
        assertEquals(1, exits); assertEquals(1, replies); assertEquals(listOf("exit", "loopEnd", "reply"), events)
    }
    @Test fun newerHoldReplacesOlder() {
        var older = 0
        reply.hold { older++ }
        reply.hold { replies++ }
        reply.release(); reply.release()
        assertEquals(0, older); assertEquals(1, replies)
    }
    @Test fun releaseWithoutHoldIsNoOp() { PendingQuitReply().release() }
}
