package com.programmersbox.desktop.motion

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.em
import com.programmersbox.kmpuiviews.motion.DesktopMotion
import com.programmersbox.kmpuiviews.motion.Keyframe
import com.programmersbox.kmpuiviews.motion.MockupEasing
import com.programmersbox.kmpuiviews.motion.cqw
import com.programmersbox.kmpuiviews.motion.track
import kotlin.math.roundToInt

/*
 * Ported from the `The End` entry in docs/mockups/desktop-launch-quit-animations.html
 * (CSS `.cream`, `.end`, `.end span`, `.end em`).
 * Each track lists the mockup's `seq` frames; leading and trailing hold frames are implied by `track`.
 */

/** `.end em` fleuron colour. */
private val NovelFleuron = Color(0xFF7A6142)

/** `.end span` text. */
private const val THE_END = "The End."

/** The app's `blur(6px)` at its height. */
private const val BLUR_PX = 6f

/** Blur steps between none and [BLUR_PX] (quarter CSS pixels). */
private const val BLUR_STEPS = 24

/** One frame of The End, in the mockup's units. */
internal class TheEndFrame(
    /** App `blur()`, CSS px. */
    val blur: Float,
    /** App opacity, 0..1. */
    val appAlpha: Float,
    /** `.cream` opacity, 0..1. */
    val paperAlpha: Float,
    /** Characters of `.end span` typed. */
    val chars: Int,
    /** `.end em` opacity, 0..1. */
    val fleuronAlpha: Float,
    /** `.end` opacity, 0..1. */
    val endAlpha: Float,
)

/** Ruled paper settles over a blurred app, "The End." types out with a fleuron beneath it, and the page fades. */
internal object TheEnd : DesktopMotion {
    override val key = "the-end"
    override val title = "The End"
    override val durationMillis = 2400

    fun frame(time: Int) = TheEndFrame(
        blur = track(time, Keyframe(0, 0f), Keyframe(500, BLUR_PX)),
        appAlpha = track(time, Keyframe(1890, 1f), Keyframe(1900, 0f)),
        paperAlpha = track(time, Keyframe(0, 0f), Keyframe(500, 1f), Keyframe(1900, 1f, MockupEasing.In), Keyframe(2400, 0f)),
        chars = typedChars(time, 650, 1250, THE_END.length),
        fleuronAlpha = track(time, Keyframe(1300, 0f), Keyframe(1600, 1f), Keyframe(1900, 1f, MockupEasing.In), Keyframe(2400, 0f)),
        endAlpha = track(time, Keyframe(1900, 1f, MockupEasing.In), Keyframe(2400, 0f)),
    )

    override fun Modifier.app(time: () -> Int): Modifier {
        val blurs = TheEndBlurSteps()
        return graphicsLayer {
            val f = frame(time())
            alpha = f.appAlpha
            renderEffect = blurs.at((f.blur / BLUR_PX * BLUR_STEPS).roundToInt(), density)
        }
    }

    /** `.cream`, then `.end` over it. */
    @Composable
    override fun BoxScope.InFront(time: () -> Int) {
        val text = rememberTextMeasurer()
        Spacer(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = frame(time()).paperAlpha }
                .drawWithCache {
                    val paper = NovelPaper(size)
                    onDrawBehind { with(paper) { drawPaper() } }
                }
        )
        Spacer(
            Modifier
                .matchParentSize()
                .drawWithCache {
                    val u = size.cqw
                    // `.end span`: 700 7cqw mono, line-height 1.1.
                    val words = NovelTypedLine(
                        text.measure(
                            THE_END,
                            TextStyle(
                                color = NovelTypeInk,
                                fontSize = (7 * u).toSp(),
                                lineHeight = 1.1.em,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    )
                    // `.end em`: normal 5cqw serif.
                    val fleuron = text.measure(
                        "❦",
                        TextStyle(color = NovelFleuron, fontSize = (5 * u).toSp(), fontFamily = FontFamily.Serif),
                    )
                    // `.end`: top 40%, items centred in a grid with a 2cqw gap.
                    val top = size.height * .4f
                    val fleuronAt = Offset((size.width - fleuron.size.width) / 2, top + words.height + 2 * u)
                    onDrawBehind {
                        val f = frame(time())
                        if (f.endAlpha > 0f) {
                            // The span grows from the centre: `justify-items: center` re-centres its typed width.
                            val typedAt = Offset((size.width - words.width(f.chars)) / 2, top)
                            with(words) { drawTyped(typedAt, f.chars, f.endAlpha) }
                            val fleuronAlpha = f.fleuronAlpha * f.endAlpha
                            if (fleuronAlpha > 0f) drawText(fleuron, topLeft = fleuronAt, alpha = fleuronAlpha)
                        }
                    }
                }
        )
    }
}

/**
 * The app's blur effect per step, built the first time each step is shown for a density (one set per [TheEnd.app]
 * chain), so frames reuse immutable effects instead of allocating one each.
 */
private class TheEndBlurSteps {
    private var density = 0f
    private val effects = arrayOfNulls<RenderEffect>(BLUR_STEPS + 1)

    fun at(step: Int, density: Float): RenderEffect? {
        if (step <= 0) return null
        if (density != this.density) {
            effects.fill(null)
            this.density = density
        }
        return effects[step] ?: run {
            // CSS blur(Npx) is a Gaussian with sigma N; Compose's BlurEffect radius maps to sigma = .57735r + .5.
            val sigma = BLUR_PX * step / BLUR_STEPS * density
            val radius = ((sigma - .5f) / .57735f).coerceAtLeast(.01f)
            BlurEffect(radius, radius, TileMode.Decal).also { effects[step] = it }
        }
    }
}
