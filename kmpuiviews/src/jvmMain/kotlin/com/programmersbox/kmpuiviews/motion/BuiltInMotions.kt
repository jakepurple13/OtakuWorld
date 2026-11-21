package com.programmersbox.kmpuiviews.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

object OffMotion : DesktopMotion {
    override val key = "off"
    override val title = "Off"
    override val durationMillis = 0
}

object FadeInMotion : DesktopMotion {
    override val key = "fade"
    override val title = "Fade"
    override val durationMillis = 450

    private val easing = CubicBezierEasing(.2f, .8f, .2f, 1f)

    override fun Modifier.app(time: () -> Int): Modifier = graphicsLayer {
        val p = progress(time(), 0, durationMillis, easing)
        alpha = p
        val s = 0.985f + 0.015f * p
        scaleX = s
        scaleY = s
    }
}

object FadeOutMotion : DesktopMotion {
    override val key = "fade"
    override val title = "Fade"
    override val durationMillis = 350

    private val easing = CubicBezierEasing(.55f, 0f, .9f, .35f)

    override fun Modifier.app(time: () -> Int): Modifier = graphicsLayer {
        val p = progress(time(), 0, durationMillis, easing)
        alpha = 1f - p
        val s = 1f - 0.015f * p
        scaleX = s
        scaleY = s
    }
}
