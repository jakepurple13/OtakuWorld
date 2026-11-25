package com.programmersbox.kmpuiviews.motion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MotionPolicyTest {
    private val sushi = object : DesktopMotion {
        override val key = "sushi-unroll"
        override val title = "Sushi"
        override val durationMillis = 3200
    }
    private val mac = MotionConditions(reduceMotion = false, platform = OverlayPlatform.MacOS)

    @Test fun launchPlaysChosen() = assertSame(sushi, launchMotion(sushi, mac))
    @Test fun reducedMotionFades() = assertSame(FadeInMotion, launchMotion(sushi, mac.copy(reduceMotion = true)))
    @Test fun reducedMotionKeepsOff() = assertSame(OffMotion, launchMotion(OffMotion, mac.copy(reduceMotion = true)))
    @Test fun quitUsesOverlayOnMac() = assertEquals(QuitPlan(sushi, QuitHost.Overlay), quitPlan(sushi, mac))
    @Test fun minimizedQuitIsOff() = assertEquals(OffMotion, quitPlan(sushi, mac.copy(minimized = true)).motion)
    @Test fun quitReducedMotionFades() = assertSame(FadeOutMotion, quitPlan(sushi, mac.copy(reduceMotion = true)).motion)
    @Test fun fullscreenQuitPlaysInWindow() = assertEquals(QuitHost.InWindow, quitPlan(sushi, mac.copy(fullscreen = true)).host)
    @Test fun linuxQuitPlaysInWindow() =
        assertEquals(QuitHost.InWindow, quitPlan(sushi, mac.copy(platform = OverlayPlatform.LinuxX11)).host)
}
