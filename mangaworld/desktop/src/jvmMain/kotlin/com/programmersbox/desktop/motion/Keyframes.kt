package com.programmersbox.desktop.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing

/** The mockup's shared easings (`E.o`, `E.io`, `E.i` in docs/mockups/desktop-launch-quit-animations.html). */
internal val EaseOut = CubicBezierEasing(.2f, .8f, .2f, 1f)
internal val EaseInOut = CubicBezierEasing(.65f, 0f, .35f, 1f)
internal val EaseIn = CubicBezierEasing(.55f, 0f, .9f, .35f)

/** One mockup `seq` frame: [value] at [at] ms; [easing] shapes the segment that starts here. */
internal class Key(val at: Int, val value: Float, val easing: Easing = LinearEasing)

/** Piecewise interpolation of [keys] (sorted by time) at [time] ms; holds the first/last value outside them. */
internal fun track(time: Int, vararg keys: Key): Float {
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
