package com.programmersbox.desktop.motion

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import com.programmersbox.kmpuiviews.motion.DesktopMotion
import com.programmersbox.kmpuiviews.motion.Keyframe
import com.programmersbox.kmpuiviews.motion.MockupEasing
import com.programmersbox.kmpuiviews.motion.track

/*
 * Ported from the `Iris out` entry in docs/mockups/desktop-launch-quit-animations.html (CSS `.black`).
 * Each track lists the mockup's `seq` frames; leading and trailing hold frames are implied by `track`.
 */

/** One frame of Iris out, in the mockup's units. */
internal class IrisOutFrame(
    /** App `clip-path: circle(r% at 50% 50%)` radius, CSS percent. */
    val irisPercent: Float,
    /** `.black` opacity, 0..1. */
    val blackAlpha: Float,
)

/** A shrinking circle closes on the centre of the app, pauses as a small spot, and snaps shut on black. */
internal object IrisOut : DesktopMotion {
    override val key = "iris-out"
    override val title = "Iris out"
    override val durationMillis = 1700

    fun frame(time: Int) = IrisOutFrame(
        irisPercent = track(
            time,
            Keyframe(0, 75f, MockupEasing.In), Keyframe(700, 8f), Keyframe(950, 8f, MockupEasing.In), Keyframe(1150, 0f),
        ),
        blackAlpha = track(time, Keyframe(1300, 1f), Keyframe(1700, 0f)),
    )

    override fun Modifier.app(time: () -> Int): Modifier = drawWithCache {
        val iris = AnimeIris(size)
        onDrawWithContent { with(iris) { drawThrough(frame(time()).irisPercent) } }
    }

    @Composable
    override fun BoxScope.Behind(time: () -> Int) {
        Spacer(
            Modifier
                .matchParentSize()
                .drawBehind {
                    val alpha = frame(time()).blackAlpha
                    if (alpha > 0f) drawRect(Color.Black, alpha = alpha)
                }
        )
    }
}
