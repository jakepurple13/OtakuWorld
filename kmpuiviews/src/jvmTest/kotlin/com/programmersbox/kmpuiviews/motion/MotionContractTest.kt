package com.programmersbox.kmpuiviews.motion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MotionContractTest {
    @Test
    fun fadeObeysLaunch() = assertEquals(emptyList(), MotionContract.violations(FadeInMotion, MotionPhase.Launch))

    @Test
    fun fadeObeysQuit() = assertEquals(emptyList(), MotionContract.violations(FadeOutMotion, MotionPhase.Quit))

    @Test
    fun offObeysLaunch() = assertEquals(emptyList(), MotionContract.violations(OffMotion, MotionPhase.Launch))

    @Test
    fun offObeysQuit() = assertEquals(emptyList(), MotionContract.violations(OffMotion, MotionPhase.Quit))

    @Test
    fun fadeInIsNotAQuit() = assertTrue(MotionContract.violations(FadeInMotion, MotionPhase.Quit).isNotEmpty())
}
