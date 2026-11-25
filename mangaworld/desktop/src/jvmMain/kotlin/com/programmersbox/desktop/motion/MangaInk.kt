package com.programmersbox.desktop.motion

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

/*
 * Pieces shared by the ink motions (Ink panels, Impact, Screentone fade, The End panel)
 * in docs/mockups/desktop-launch-quit-animations.html.
 */

/** `.paper` background. */
internal val Paper = Color(0xFFF7F5EF)

/** `.pnl` border and title ink. */
internal val PanelInk = Color(0xFF121212)

/** `.lines`, `.sfx`, `.sub`, `.cap` ink. */
internal val Ink = Color(0xFF111111)

/** `.tone` dot colour. */
internal val ToneInk = Color(0xFF0B0B0D)

/** `--sacc` for MangaWorld: the SFX text shadow. */
internal val MangaAccent = Color(0xFFE05571)

/** `font: Ncqw "Dela Gothic One"`: no bundled display face, so the default family at its heaviest weight. */
internal fun Density.displayStyle(sizePx: Float, color: Color) =
    TextStyle(color = color, fontSize = sizePx.toSp(), lineHeight = sizePx.toSp(), fontWeight = FontWeight.Black)

/**
 * CSS `filter: grayscale([grayscale]) contrast([contrast])`; contrast applies after grayscale.
 * Luma weights are the CSS filter spec's (0.2126, 0.7152, 0.0722).
 */
internal fun inkFilter(grayscale: Float, contrast: Float): ColorFilter {
    val keep = 1 - grayscale
    val r = floatArrayOf(.2126f + .7874f * keep, .7152f - .7152f * keep, .0722f - .0722f * keep)
    val g = floatArrayOf(.2126f - .2126f * keep, .7152f + .2848f * keep, .0722f - .0722f * keep)
    val b = floatArrayOf(.2126f - .2126f * keep, .7152f - .7152f * keep, .0722f + .9278f * keep)
    // ColorMatrix offsets are on the 0..255 scale.
    val offset = (.5f - .5f * contrast) * 255
    fun row(weights: FloatArray) = floatArrayOf(weights[0] * contrast, weights[1] * contrast, weights[2] * contrast, 0f, offset)
    return ColorFilter.colorMatrix(ColorMatrix(row(r) + row(g) + row(b) + floatArrayOf(0f, 0f, 0f, 1f, 0f)))
}

/** Steps from no ink to full ink in an [inkFilters] table; step 0 (the identity) is never drawn through a layer. */
internal const val INK_STEPS = 30

/**
 * Every [inkFilter] step from none to full grayscale, for motions whose contrast rises with grayscale (same keyframes)
 * from 1 to [fullContrast]. Immutable, so a motion builds it once (`by lazy`) and shares it across frames.
 */
internal fun inkFilters(fullContrast: Float): Array<ColorFilter> = Array(INK_STEPS + 1) {
    val grayscale = it.toFloat() / INK_STEPS
    inkFilter(grayscale, 1 + (fullContrast - 1) * grayscale)
}

/** Draws the content through [filters] at [grayscale]'s step; skips the extra layer at step 0. */
internal fun Modifier.inked(filters: Array<ColorFilter>, grayscale: () -> Float): Modifier = drawWithCache {
    val bounds = Rect(Offset.Zero, size)
    val paint = Paint()
    onDrawWithContent {
        val step = (grayscale() * INK_STEPS).roundToInt().coerceIn(0, INK_STEPS)
        if (step == 0) {
            drawContent()
        } else {
            paint.colorFilter = filters[step]
            drawIntoCanvas { canvas ->
                canvas.saveLayer(bounds, paint)
                drawContent()
                canvas.restore()
            }
        }
    }
}
