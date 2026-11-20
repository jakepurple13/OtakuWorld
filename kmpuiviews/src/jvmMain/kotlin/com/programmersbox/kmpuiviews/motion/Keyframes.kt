package com.programmersbox.kmpuiviews.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope

/*
 * Helpers for porting the `seq` keyframes and `cqw` units of docs/mockups/desktop-launch-quit-animations.html
 * into a [DesktopMotion].
 */

/**
 * The mockup's `E` easings. Named apart from Compose's `EaseOut`/`EaseInOut`/`EaseIn`, which use different curves.
 */
object MockupEasing {
    /** `E.o`: a strong ease-out. */
    val Out = CubicBezierEasing(.2f, .8f, .2f, 1f)

    /** `E.io`: a symmetric ease-in-out. */
    val InOut = CubicBezierEasing(.65f, 0f, .35f, 1f)

    /** `E.i`: a strong ease-in. */
    val In = CubicBezierEasing(.55f, 0f, .9f, .35f)

    /** `E.back`: overshoots then settles. */
    val Back = CubicBezierEasing(.3f, 1.55f, .5f, 1f)
}

/** 1cqw (1% of the host width) in pixels for a host of [this] size. */
val Size.cqw get() = width / 100

/**
 * Unit for vertical moves, in pixels: 1cqw on the mockup's 16:10 stage (height = 62.5cqw), growing with
 * taller hosts so off-screen drops and lifts still clear the window.
 */
val Size.verticalCqw get() = maxOf(cqw, height / 62.5f)

/** 1cqw in pixels for the current draw [DrawScope.size]. */
val DrawScope.cqw get() = size.cqw

/** [Size.verticalCqw] for the current draw [DrawScope.size]. */
val DrawScope.verticalCqw get() = size.verticalCqw

/** One mockup `seq` frame: [value] at [at] ms; [easing] shapes the segment that starts here. */
class Keyframe(val at: Int, val value: Float, val easing: Easing = LinearEasing)

/** Piecewise interpolation of [keys] (sorted by time) at [time] ms; holds the first/last value outside them. */
fun track(time: Int, vararg keys: Keyframe): Float {
    if (time <= keys.first().at) return keys.first().value
    for (i in 0 until keys.lastIndex) {
        val from = keys[i]
        val to = keys[i + 1]
        if (time < to.at) {
            val fraction = from.easing.transform((time - from.at).toFloat() / (to.at - from.at))
            return from.value + (to.value - from.value) * fraction
        }
    }
    return keys.last().value
}
