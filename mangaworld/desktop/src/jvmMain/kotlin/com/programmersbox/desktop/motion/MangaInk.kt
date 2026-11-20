package com.programmersbox.desktop.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density

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

/** Draws the content through [inkFilter]; skips the extra layer while the filter is still the identity. */
internal fun ContentDrawScope.drawInked(grayscale: Float, contrast: Float) {
    if (grayscale == 0f && contrast == 1f) {
        drawContent()
        return
    }
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { colorFilter = inkFilter(grayscale, contrast) })
        drawContent()
        canvas.restore()
    }
}
