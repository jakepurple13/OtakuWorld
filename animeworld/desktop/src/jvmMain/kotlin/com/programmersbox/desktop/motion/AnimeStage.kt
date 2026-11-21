package com.programmersbox.desktop.motion

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.hypot
import kotlin.math.sqrt

/*
 * Pieces shared by the AnimeWorld motions in docs/mockups/desktop-launch-quit-animations.html.
 */

/** `.room` background. */
internal val AnimeRoom = Color(0xFF06070A)

/** `--sacc` for AnimeWorld. */
internal val AnimeAccent = Color(0xFF5F7DFF)

/**
 * CSS `clip-path: circle(r% at 50% 50%)` for a host of [size]; built in `drawWithCache`.
 * One circle path is built per size and scaled per frame, so drawing allocates nothing.
 */
internal class AnimeIris(size: Size) {
    private val centre = size.center

    /** The radius that reaches the host corners: a larger circle clips nothing. */
    private val coverRadius = hypot(size.width, size.height) / 2

    /** 1% of a CSS `circle()` radius: percentages resolve against sqrt(w² + h²) / sqrt(2). */
    private val percent = hypot(size.width, size.height) / sqrt(2f) / 100

    private val cover = Path().apply { addOval(Rect(centre, coverRadius)) }

    /** Draws the content through a centred circle of [radiusPercent]% (CSS units); nothing at 0. */
    fun ContentDrawScope.drawThrough(radiusPercent: Float) {
        val radius = radiusPercent * percent
        when {
            radius >= coverRadius -> drawContent()
            radius <= 0f -> Unit
            else -> {
                val k = radius / coverRadius
                scale(k, centre) {
                    clipPath(cover) {
                        scale(1 / k, centre) { this@drawThrough.drawContent() }
                    }
                }
            }
        }
    }
}
