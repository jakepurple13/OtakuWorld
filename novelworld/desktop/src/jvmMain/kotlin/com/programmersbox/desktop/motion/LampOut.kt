package com.programmersbox.desktop.motion

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.skiaPaint
import com.programmersbox.kmpuiviews.motion.DesktopMotion
import com.programmersbox.kmpuiviews.motion.Keyframe
import com.programmersbox.kmpuiviews.motion.MockupEasing
import com.programmersbox.kmpuiviews.motion.cqw
import com.programmersbox.kmpuiviews.motion.track
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import kotlin.math.hypot
import kotlin.math.roundToInt

/*
 * Ported from the `Lamp out` entry in docs/mockups/desktop-launch-quit-animations.html
 * (CSS `.dark` with `@property --v`, `.flame`).
 * Each track lists the mockup's `seq` frames; leading and trailing hold frames are implied by `track`.
 */

/** `.dark` gradient end colour. */
internal val NovelLampDark = Color(0xFF0A0705)

/** `.flame` gradient centre. */
private val NovelFlameCore = Color(0xFFFFF6D0)

/** `.flame` gradient at 35%. */
private val NovelFlameBody = Color(0xFFFFC25A)

/** `.flame` gradient at 65%, fading to transparent by 72%. */
private val NovelFlameEdge = Color(0xFFFF7A12)

/** `.flame` box-shadow (`#ff962e66`). */
private val NovelFlameGlow = Color(0x66FF962E)

/** `--v` range in the mockup's keyframes, and the step its vignettes are cached at (CSS percent). */
private const val VIGNETTE_MIN = -30f
private const val VIGNETTE_MAX = 95f
private const val VIGNETTE_STEP = .5f

/** `.dark`: the ramp from transparent at `--v` to dark at `--v + 22%`. */
private const val VIGNETTE_RAMP = 22f

/** Warm filter steps between none and the full `sepia(.55) saturate(1.2)`. */
private const val WARM_STEPS = 30

/** One frame of Lamp out, in the mockup's units. */
internal class LampOutFrame(
    /** How far the app's filter is from none (0) to the full lamplight (1). */
    val warm: Float,
    /** App opacity, 0..1. */
    val appAlpha: Float,
    /** `.dark` `--v`, CSS percent of the gradient ray. */
    val vignette: Float,
    /** `.dark` opacity, 0..1. */
    val darkAlpha: Float,
    /** `.flame` opacity, 0..1. */
    val flameAlpha: Float,
    /** `.flame` scaleX. */
    val flameScaleX: Float,
    /** `.flame` scaleY. */
    val flameScaleY: Float,
)

/** The app warms to lamplight, darkness flickers in from the edges, and a last small flame gutters out. */
internal object LampOut : DesktopMotion {
    override val key = "lamp-out"
    override val title = "Lamp out"
    override val durationMillis = 2200

    fun frame(time: Int) = LampOutFrame(
        warm = track(time, Keyframe(0, 0f), Keyframe(800, 1f)),
        appAlpha = track(time, Keyframe(1490, 1f), Keyframe(1500, 0f)),
        vignette = track(
            time,
            Keyframe(0, 95f), Keyframe(300, 68f), Keyframe(420, 78f), Keyframe(700, 44f),
            Keyframe(820, 52f, MockupEasing.In), Keyframe(1250, 6f), Keyframe(1500, -30f),
        ),
        darkAlpha = track(time, Keyframe(1800, 1f), Keyframe(2200, 0f)),
        flameAlpha = track(
            time,
            Keyframe(900, 0f), Keyframe(1100, 1f), Keyframe(1220, .6f), Keyframe(1320, 1f),
            Keyframe(1450, .8f, MockupEasing.In), Keyframe(1700, 0f),
        ),
        flameScaleX = track(
            time,
            Keyframe(1100, 1f), Keyframe(1220, .9f), Keyframe(1320, 1f), Keyframe(1450, .8f, MockupEasing.In), Keyframe(1700, .2f),
        ),
        flameScaleY = track(
            time,
            Keyframe(1100, 1f), Keyframe(1220, 1.1f), Keyframe(1320, 1f), Keyframe(1450, .8f, MockupEasing.In), Keyframe(1700, .5f),
        ),
    )

    override fun Modifier.app(time: () -> Int): Modifier = this
        .graphicsLayer { alpha = frame(time()).appAlpha }
        .drawWithCache {
            val bounds = Rect(Offset.Zero, size)
            val paint = Paint()
            onDrawWithContent {
                val step = (frame(time()).warm * WARM_STEPS).roundToInt().coerceIn(0, WARM_STEPS)
                if (step == 0) {
                    drawContent()
                } else {
                    paint.colorFilter = warmFilters[step]
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(bounds, paint)
                        drawContent()
                        canvas.restore()
                    }
                }
            }
        }

    /** Every warm filter step, built once on first use; step 0 is never drawn through a layer. */
    private val warmFilters: Array<ColorFilter> by lazy { Array(WARM_STEPS + 1) { warmFilter(it.toFloat() / WARM_STEPS) } }

    /** `.dark`, then `.flame`. */
    @Composable
    override fun BoxScope.InFront(time: () -> Int) {
        Spacer(
            Modifier
                .matchParentSize()
                .drawWithCache {
                    // `radial-gradient(circle at 50% 55%, …)`: the ray runs to the farthest corner, a top one.
                    val centre = Offset(size.width * .5f, size.height * .55f)
                    val ray = hypot(size.width * .5f, size.height * .55f)
                    // The gradient's stops move with --v, which no fixed brush can scale into (the ramp keeps its
                    // width while its start moves), so each half-percent of --v gets its own brush, built when first
                    // shown for this size and reused after.
                    val vignettes = arrayOfNulls<Brush>(((VIGNETTE_MAX - VIGNETTE_MIN) / VIGNETTE_STEP).roundToInt() + 1)
                    onDrawBehind {
                        val f = frame(time())
                        if (f.darkAlpha <= 0f) return@onDrawBehind
                        val index = ((f.vignette - VIGNETTE_MIN) / VIGNETTE_STEP).roundToInt().coerceIn(0, vignettes.lastIndex)
                        val v = VIGNETTE_MIN + index * VIGNETTE_STEP
                        if (v <= -VIGNETTE_RAMP) {
                            // The whole ramp lies before the centre: dark everywhere.
                            drawRect(NovelLampDark, alpha = f.darkAlpha)
                        } else {
                            val brush = vignettes[index] ?: vignetteBrush(v, centre, ray).also { vignettes[index] = it }
                            drawRect(brush, alpha = f.darkAlpha)
                        }
                    }
                }
        )
        Spacer(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val f = frame(time())
                    alpha = f.flameAlpha
                    scaleX = f.flameScaleX
                    scaleY = f.flameScaleY
                    // Scaled about the centre of the `.flame` box: 55% down, less its -3cqw margin, plus half its height.
                    transformOrigin = TransformOrigin(.5f, (size.height * .55f - .8f * size.cqw) / size.height)
                }
                .drawWithCache {
                    val u = size.cqw
                    // `.flame`: 3cqw × 4.4cqw at (50%, 55%) with margin -3cqw 0 0 -1.5cqw.
                    val box = Rect(Offset(size.width * .5f - 1.5f * u, size.height * .55f - 3 * u), Size(3 * u, 4.4f * u))
                    // `border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%`.
                    val flame = flameShape(box, 0f)
                    // `radial-gradient(circle at 50% 70%, …)`, its ray running to the farthest (top) corner.
                    val core = Offset(box.center.x, box.top + box.height * .7f)
                    val fill = Brush.radialGradient(
                        0f to NovelFlameCore,
                        .35f to NovelFlameBody,
                        .65f to NovelFlameEdge,
                        .72f to NovelFlameEdge.copy(alpha = 0f),
                        center = core,
                        radius = hypot(box.width * .5f, box.height * .7f),
                    )
                    // `box-shadow: 0 0 7cqw 2.5cqw #ff962e66`: the shape grown by 2.5cqw, blurred with sigma 3.5cqw,
                    // drawn only outside the flame's box as CSS outer shadows are.
                    val glowShape = flameShape(box, 2.5f * u)
                    val glow = Paint().apply {
                        color = NovelFlameGlow
                        skiaPaint.maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, 3.5f * u)
                    }
                    onDrawBehind {
                        clipPath(flame, ClipOp.Difference) { drawIntoCanvas { it.drawPath(glowShape, glow) } }
                        drawPath(flame, fill)
                    }
                }
        )
    }
}

/** The `.flame` outline for [box], grown by [spread] on every side with its corner radii grown to match. */
private fun flameShape(box: Rect, spread: Float) = Path().apply {
    val x = box.width * .5f + spread
    val top = CornerRadius(x, box.height * .6f + spread)
    val bottom = CornerRadius(x, box.height * .4f + spread)
    addRoundRect(RoundRect(box.inflate(spread), top, top, bottom, bottom))
}

/**
 * `.dark` at `--v` = [v]%: transparent at v% of the [ray] from [centre], dark at (v + 22)%. Below 0 the stop
 * colour at the centre is already part-way into the ramp.
 */
private fun vignetteBrush(v: Float, centre: Offset, ray: Float): Brush {
    val end = v + VIGNETTE_RAMP
    return Brush.radialGradient(
        maxOf(v, 0f) / end to NovelLampDark.copy(alpha = maxOf(-v, 0f) / VIGNETTE_RAMP),
        1f to NovelLampDark,
        center = centre,
        radius = end / 100 * ray,
    )
}

/**
 * CSS `filter: sepia(.55p) saturate(1 + .2p)` as one colour matrix, using the Filter Effects spec matrices composed
 * in order. Browsers clamp between filters; the composed matrix only clamps at the end.
 */
private fun warmFilter(progress: Float): ColorFilter {
    val s = 1 - .55f * progress
    val sepia = floatArrayOf(
        .393f + .607f * s, .769f - .769f * s, .189f - .189f * s,
        .349f - .349f * s, .686f + .314f * s, .168f - .168f * s,
        .272f - .272f * s, .534f - .534f * s, .131f + .869f * s,
    )
    val t = 1 + .2f * progress
    val saturate = floatArrayOf(
        .213f + .787f * t, .715f - .715f * t, .072f - .072f * t,
        .213f - .213f * t, .715f + .285f * t, .072f - .072f * t,
        .213f - .213f * t, .715f - .715f * t, .072f + .928f * t,
    )
    val values = FloatArray(20)
    for (row in 0 until 3) {
        for (col in 0 until 3) {
            var sum = 0f
            for (k in 0 until 3) sum += saturate[row * 3 + k] * sepia[k * 3 + col]
            values[row * 5 + col] = sum
        }
    }
    values[18] = 1f
    return ColorFilter.colorMatrix(ColorMatrix(values))
}
