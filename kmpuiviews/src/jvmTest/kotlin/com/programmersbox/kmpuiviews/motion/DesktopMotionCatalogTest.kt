package com.programmersbox.kmpuiviews.motion

import androidx.compose.animation.core.LinearEasing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DesktopMotionCatalogTest {
    private val a = fake("a-one")
    private val b = fake("a-two")
    private val catalog = DesktopMotionCatalog("AWorld", launch = listOf(a, b), quit = listOf(b))

    @Test
    fun optionsAreOffFadeThenOwn() =
        assertEquals(listOf("off", "fade", "a-one", "a-two"), catalog.options(MotionPhase.Launch).map { it.key })

    @Test
    fun quitFadeIsFadeOut() = assertSame(FadeOutMotion, catalog.options(MotionPhase.Quit)[1])

    @Test
    fun emptyKeyResolvesToDefault() = assertSame(a, catalog.resolve(MotionPhase.Launch, ""))

    @Test
    fun unknownKeyResolvesToDefault() = assertSame(a, catalog.resolve(MotionPhase.Launch, "crt-power-on"))

    @Test
    fun offResolves() = assertSame(OffMotion, catalog.resolve(MotionPhase.Quit, "off"))

    @Test
    fun progressClampsAndEases() {
        assertEquals(0f, a.progress(50, 100, 200, LinearEasing))
        assertEquals(0.5f, a.progress(150, 100, 200, LinearEasing))
        assertEquals(1f, a.progress(900, 100, 200, LinearEasing))
    }

    @Test
    fun progressZeroLengthSegment() {
        assertEquals(0f, a.progress(99, 100, 100, LinearEasing))
        assertEquals(1f, a.progress(100, 100, 100, LinearEasing))
    }

    private fun fake(k: String) = object : DesktopMotion {
        override val key = k
        override val title = k
        override val durationMillis = 1000
    }
}
