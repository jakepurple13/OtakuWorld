package com.programmersbox.desktop.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import com.programmersbox.kmpuiviews.motion.DesktopMotion
import com.programmersbox.kmpuiviews.motion.Keyframe
import com.programmersbox.kmpuiviews.motion.MockupEasing
import com.programmersbox.kmpuiviews.motion.track

/*
 * Ported from the `CRT power-on` / `CRT power-off` entries in docs/mockups/desktop-launch-quit-animations.html.
 * Each track lists the mockup's `seq` frames; leading and trailing hold frames are implied by `track`.
 */

/** One frame of CRT power-on, in the mockup's units. */
internal class CrtPowerOnFrame(
    /** `.tv` opacity (set, bezel and stand), 0..1. */
    val tvAlpha: Float,
    /** App `scale()` x about the host centre, before [zoom]; .6 fills the TV screen. */
    val appScaleX: Float,
    /** App `scale()` y about the host centre, before [zoom]. */
    val appScaleY: Float,
    /** App `filter: brightness()`, 1 = unchanged. */
    val brightness: Float,
    /** `.glow` opacity, 0..1. */
    val glowAlpha: Float,
    /** Static (`.scr` noise) opacity, 0..1. */
    val noiseAlpha: Float,
    /** `.scan` opacity, 0..1. */
    val scanAlpha: Float,
    /** `.zoomer` scale about the host centre: the push-in that takes TV and app together to fill the window. */
    val zoom: Float,
)

/** A TV in a dark room clicks on: dot, line, picture through static, then the camera pushes into the screen. */
internal object CrtPowerOn : DesktopMotion {
    override val key = "crt-power-on"
    override val title = "CRT power-on"
    override val durationMillis = 2300

    fun frame(time: Int) = CrtPowerOnFrame(
        tvAlpha = track(time, Keyframe(0, 0f), Keyframe(300, 1f), Keyframe(1650, 1f), Keyframe(2100, 0f)),
        appScaleX = track(time, Keyframe(0, 0f), Keyframe(350, .02f, MockupEasing.Out), Keyframe(650, .6f)),
        appScaleY = track(time, Keyframe(650, .004f, MockupEasing.Out), Keyframe(950, .6f)),
        brightness = track(time, Keyframe(650, 8f, MockupEasing.Out), Keyframe(950, 3f), Keyframe(1300, 1f)),
        glowAlpha = track(time, Keyframe(300, 0f), Keyframe(380, 1f), Keyframe(650, 0f)),
        noiseAlpha = track(time, Keyframe(650, 0f), Keyframe(750, .9f), Keyframe(1350, 0f)),
        scanAlpha = track(time, Keyframe(1650, 1f), Keyframe(2150, 0f)),
        // .6 × 1.6667 brings the TV screen to the window edges.
        zoom = track(time, Keyframe(1650, 1f, MockupEasing.InOut), Keyframe(2300, 1.6667f)),
    )

    override fun Modifier.app(time: () -> Int): Modifier = this
        .graphicsLayer {
            val frame = frame(time())
            scaleX = frame.appScaleX * frame.zoom
            scaleY = frame.appScaleY * frame.zoom
        }
        .drawWithCache {
            val bounds = Rect(Offset.Zero, size)
            onDrawWithContent { drawBrightened(bounds, frame(time()).brightness) }
        }

    @Composable
    override fun BoxScope.Behind(time: () -> Int) {
        Spacer(Modifier.matchParentSize().background(CrtRoom))
        Spacer(
            Modifier
                .matchParentSize()
                // Layer opacity so the bezel rings fade as one set.
                .graphicsLayer {
                    val frame = frame(time())
                    alpha = frame.tvAlpha
                    scaleX = frame.zoom
                    scaleY = frame.zoom
                }
                .drawWithCache {
                    val tv = CrtTv(size, this)
                    onDrawBehind { with(tv) { drawSet() } }
                }
        )
    }

    @Composable
    override fun BoxScope.InFront(time: () -> Int) {
        Spacer(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val zoom = frame(time()).zoom
                    scaleX = zoom
                    scaleY = zoom
                }
                .drawWithCache {
                    val tv = CrtTv(size, this)
                    val glow = CrtGlow(size)
                    onDrawBehind {
                        val now = time()
                        val frame = frame(now)
                        with(tv) {
                            drawStatic(now, frame.noiseAlpha)
                            drawScanlines(frame.scanAlpha)
                        }
                        with(glow) { drawGlow(frame.glowAlpha) }
                    }
                }
        )
    }
}

/** One frame of CRT power-off, in the mockup's units. */
internal class CrtPowerOffFrame(
    /** `.room` opacity, 0..1. */
    val roomAlpha: Float,
    /** App `scale()` x about the host centre. */
    val appScaleX: Float,
    /** App `scale()` y about the host centre. */
    val appScaleY: Float,
    /** App `filter: brightness()`, 1 = unchanged. */
    val brightness: Float,
    /** `.glow` opacity, 0..1. */
    val glowAlpha: Float,
    /** `.glow` scale about the host centre. */
    val glowScale: Float,
)

/** The picture collapses to a bright line, then to a dot that lingers and fades, like an old tube TV. */
internal object CrtPowerOff : DesktopMotion {
    override val key = "crt-power-off"
    override val title = "CRT power-off"
    override val durationMillis = 1500

    /** `cubic-bezier(.6,0,.9,.4)`: the vertical collapse. */
    private val Collapse = CubicBezierEasing(.6f, 0f, .9f, .4f)

    /** `cubic-bezier(.3,0,.2,1)`: the dot shrinking away. */
    private val DotFade = CubicBezierEasing(.3f, 0f, .2f, 1f)

    fun frame(time: Int) = CrtPowerOffFrame(
        roomAlpha = track(time, Keyframe(1150, 1f), Keyframe(1500, 0f)),
        appScaleX = track(time, Keyframe(260, 1f, MockupEasing.InOut), Keyframe(480, 0f)),
        appScaleY = track(time, Keyframe(0, 1f, Collapse), Keyframe(260, .006f)),
        brightness = track(time, Keyframe(0, 1f, Collapse), Keyframe(260, 6f, MockupEasing.InOut), Keyframe(480, 8f)),
        glowAlpha = track(time, Keyframe(440, 0f), Keyframe(500, 1f, DotFade), Keyframe(1150, 0f)),
        glowScale = track(time, Keyframe(500, 1f, DotFade), Keyframe(1150, .2f)),
    )

    override fun Modifier.app(time: () -> Int): Modifier = this
        .graphicsLayer {
            val frame = frame(time())
            scaleX = frame.appScaleX
            scaleY = frame.appScaleY
        }
        .drawWithCache {
            val bounds = Rect(Offset.Zero, size)
            onDrawWithContent { drawBrightened(bounds, frame(time()).brightness) }
        }

    @Composable
    override fun BoxScope.Behind(time: () -> Int) {
        Spacer(Modifier.matchParentSize().drawBehind { drawRect(CrtRoom, alpha = frame(time()).roomAlpha) })
    }

    @Composable
    override fun BoxScope.InFront(time: () -> Int) {
        Spacer(
            Modifier
                .matchParentSize()
                .drawWithCache {
                    val glow = CrtGlow(size)
                    onDrawBehind {
                        val frame = frame(time())
                        with(glow) { drawGlow(frame.glowAlpha, frame.glowScale) }
                    }
                }
        )
    }
}
