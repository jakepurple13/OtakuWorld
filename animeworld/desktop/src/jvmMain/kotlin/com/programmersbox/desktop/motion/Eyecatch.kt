package com.programmersbox.desktop.motion

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
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
import kotlin.math.abs

/*
 * Ported from the `Eyecatch` entry in docs/mockups/desktop-launch-quit-animations.html
 * (CSS `.room`, `.slab`, `.slab.a`, `.slab.b`).
 * Each track lists the mockup's `seq` frames; leading and trailing hold frames are implied by `track`.
 */

/** `.slab.b` background. */
internal val AnimeEyecatchPaper = Color(0xFFF3F1EC)

/** `.slab.b b` title colour. */
private val AnimeEyecatchInk = Color(0xFF0F121C)

/** `.slab small` episode label colour (`#ffffffcc`). */
private val AnimeEyecatchLabel = Color(0xCCFFFFFF)

/** `translateX(±105%)`: a slab fully off-screen. */
private const val SLAB_AWAY = 1.05f

/** One frame of Eyecatch, in the mockup's units. */
internal class EyecatchFrame(
    /** `.room` opacity, 0..1. */
    val roomAlpha: Float,
    /** `.slab.a` translateX, fraction of the host width. */
    val slabAShift: Float,
    /** `.slab.b` translateX, fraction of the host width. */
    val slabBShift: Float,
    /** App scale about its centre. */
    val appScale: Float,
)

/** Two diagonal slabs slide in from opposite sides like a mid-episode eyecatch, hold on the title, then move on. */
internal object Eyecatch : DesktopMotion {
    override val key = "eyecatch"
    override val title = "Eyecatch"
    override val durationMillis = 1900

    fun frame(time: Int) = EyecatchFrame(
        roomAlpha = track(time, Keyframe(1350, 1f), Keyframe(1360, 0f)),
        slabAShift = track(
            time,
            Keyframe(0, -SLAB_AWAY, MockupEasing.Out), Keyframe(420, 0f), Keyframe(1350, 0f, MockupEasing.In), Keyframe(1800, SLAB_AWAY),
        ),
        slabBShift = track(
            time,
            Keyframe(80, SLAB_AWAY, MockupEasing.Out), Keyframe(500, 0f), Keyframe(1400, 0f, MockupEasing.In), Keyframe(1850, -SLAB_AWAY),
        ),
        appScale = track(time, Keyframe(1350, 1.05f, MockupEasing.Out), Keyframe(1900, 1f)),
    )

    override fun Modifier.app(time: () -> Int): Modifier = graphicsLayer {
        val scale = frame(time()).appScale
        scaleX = scale
        scaleY = scale
    }

    /** Room over the app, then the two slabs, each clipped to its diagonal half and shifted by its own track. */
    @Composable
    override fun BoxScope.InFront(time: () -> Int) {
        val text = rememberTextMeasurer()
        Spacer(
            Modifier
                .matchParentSize()
                .drawWithCache {
                    val u = size.cqw
                    val w = size.width
                    val h = size.height
                    // clip-path: polygon(0 0, 100% 0, 100% 40%, 0 62%)
                    val slabA = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w, 0f)
                        lineTo(w, .4f * h)
                        lineTo(0f, .62f * h)
                        close()
                    }
                    // clip-path: polygon(0 62%, 100% 40%, 100% 100%, 0 100%)
                    val slabB = Path().apply {
                        moveTo(0f, .62f * h)
                        lineTo(w, .4f * h)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    // `.slab small`: 500 2cqw mono, letter-spacing .3em.
                    val episode = text.measure(
                        "EPISODE 01",
                        TextStyle(
                            color = AnimeEyecatchLabel,
                            fontSize = (2 * u).toSp(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = .3.em,
                        ),
                    )
                    // `.slab b`: 800 12cqw/0.9 display, letter-spacing -.02em.
                    val titleStyle = TextStyle(
                        fontSize = (12 * u).toSp(),
                        lineHeight = (10.8f * u).toSp(),
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-.02).em,
                    )
                    val anime = text.measure("ANIME", titleStyle.copy(color = Color.White))
                    val world = text.measure("WORLD", titleStyle.copy(color = AnimeEyecatchInk))
                    // `.slab.a`: padding 9cqw 6cqw, items from the top; small has margin-bottom 1cqw.
                    val episodeAt = Offset(6 * u, 9 * u)
                    val animeAt = Offset(6 * u, 9 * u + episode.size.height + u)
                    // `.slab.b`: items at the end both ways, padding 0 6cqw 7cqw.
                    val worldAt = Offset(w - 6 * u - world.size.width, h - 7 * u - world.size.height)
                    onDrawBehind {
                        val frame = frame(time())
                        if (frame.roomAlpha > 0f) drawRect(AnimeRoom, alpha = frame.roomAlpha)
                        if (abs(frame.slabAShift) < SLAB_AWAY) {
                            translate(left = frame.slabAShift * w) {
                                clipPath(slabA) {
                                    drawRect(AnimeAccent)
                                    drawText(episode, topLeft = episodeAt)
                                    drawText(anime, topLeft = animeAt)
                                }
                            }
                        }
                        if (abs(frame.slabBShift) < SLAB_AWAY) {
                            translate(left = frame.slabBShift * w) {
                                clipPath(slabB) {
                                    drawRect(AnimeEyecatchPaper)
                                    drawText(world, topLeft = worldAt)
                                }
                            }
                        }
                    }
                }
        )
    }
}
