package com.programmersbox.desktop.motion

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.skiaPaint
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import com.programmersbox.kmpuiviews.motion.cqw
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * The book shared by `Book opening` and `Book closing` in docs/mockups/desktop-launch-quit-animations.html
 * (`makeBook()` and CSS `.desk`, `.bk-zoom`, `.bk-slide`, `.book`, `.pages`, `.pages h4`, `.pages i`,
 * `.cover`, `.cover .front`, `.cover .back`).
 */

/** `.desk` radial gradient centre colour. */
private val NovelDeskLight = Color(0xFF3B2C22)

/** `.desk` radial gradient edge colour. */
private val NovelDeskDark = Color(0xFF1C140F)

/** `.pages` background. */
internal val BookPages = Color(0xFFF4ECDC)

/** `.pages` box-shadow: the first page edge peeking out (.35cqw). */
private val BookPageEdge = Color(0xFFE3D7C0)

/** `.pages` box-shadow: the second page edge peeking out (.7cqw). */
private val BookPageEdgeDeep = Color(0xFFD3C4A8)

/** `.pages h4` colour. */
private val BookInk = Color(0xFF3A2C20)

/** `.pages i` text-line bars (`#3a2c2030`). */
private val BookTextLine = Color(0x303A2C20)

/** Inset gutter shadow by the spine on pages and inside cover (`#0000003a`). */
private val BookGutterShadow = Color(0x3A000000)

/** `.pages` / `.cover .front` drop shadow (`#0007`). */
private val BookDropShadow = Color(0x77000000)

/** `.cover .front` background and inner band. */
internal val BookCover = Color(0xFF1F4D3C)

/** `.cover .front` gradient spine strip. */
private val BookCoverSpine = Color(0xFF123527)

/** `.cover .front` gilt rule (`#c9a85c80`). */
private val BookGiltRule = Color(0x80C9A85C)

/** `.cover .front span` title colour. */
private val BookGilt = Color(0xFFE4C77D)

/** `.cover .front span small` colour (`#c9a85ccc`). */
private val BookGiltSmall = Color(0xCCC9A85C)

/** `.cover .back`: the inside of the cover. */
internal val BookCoverInside = Color(0xFFEFE5D1)

/** `.book` size in cqw. */
private const val BOOK_WIDTH = 30f
private const val BOOK_HEIGHT = 40f

/** `.book` perspective stand-in for the cover's rotation, in density-scaled camera units. */
private const val COVER_CAMERA_DISTANCE = 12f

/** One frame of the book scene, in the mockup's units. */
internal class BookFrame(
    /** `.desk` opacity, 0..1. */
    val deskAlpha: Float,
    /** App opacity, 0..1. */
    val appAlpha: Float,
    /** App `scale()` about the host centre. */
    val appScale: Float,
    /** `.bk-zoom` scale about the host centre. */
    val zoom: Float,
    /** `.bk-zoom` opacity, 0..1. */
    val zoomAlpha: Float,
    /** `.bk-slide` translateX, cqw. */
    val slide: Float,
    /** `.book` opacity, 0..1. */
    val bookAlpha: Float,
    /** `.book` translateY, cqw. */
    val bookDrop: Float,
    /** `.cover` rotateY, degrees: 0 closed, -180 fully open to the left. */
    val coverAngle: Float,
)

/** App layer: opacity and scale about the host centre. */
internal fun Modifier.bookApp(frame: () -> BookFrame): Modifier = graphicsLayer {
    val f = frame()
    alpha = f.appAlpha
    scaleX = f.appScale
    scaleY = f.appScale
}

/** `.desk`: `radial-gradient(90% 80% at 50% 40%, #3b2c22, #1c140f)` at the frame's opacity. */
@Composable
internal fun BoxScope.BookDesk(frame: () -> BookFrame) {
    Spacer(
        Modifier
            .matchParentSize()
            .drawWithCache {
                val centre = Offset(size.width * .5f, size.height * .4f)
                val radiusX = size.width * .9f
                // Draw a circle-gradient then squash it vertically into the 90% × 80% ellipse.
                val squash = (size.height * .8f) / radiusX
                val brush = Brush.radialGradient(listOf(NovelDeskLight, NovelDeskDark), centre, radiusX)
                val top = centre.y - centre.y / squash
                val area = Size(size.width, size.height / squash)
                onDrawBehind {
                    val alpha = frame().deskAlpha
                    if (alpha > 0f) scale(1f, squash, centre) { drawRect(brush, Offset(0f, top), area, alpha = alpha) }
                }
            }
    )
}

/**
 * `.bk-zoom` > `.bk-slide` > `.book`: the pages with the cover hinged on their left edge. The zoom, slide, rise
 * and both opacities fold into one layer (the slide and rise are scaled by the zoom, as nested CSS transforms are).
 */
@Composable
internal fun BoxScope.BookOnDesk(frame: () -> BookFrame) {
    val text = rememberTextMeasurer()
    Box(
        Modifier
            .matchParentSize()
            .graphicsLayer {
                val f = frame()
                val u = size.cqw
                scaleX = f.zoom
                scaleY = f.zoom
                translationX = f.slide * u * f.zoom
                translationY = f.bookDrop * u * f.zoom
                alpha = f.zoomAlpha * f.bookAlpha
            }
    ) {
        Spacer(
            Modifier
                .bookBounds()
                .drawWithCache {
                    val pages = BookPagesFace(size, this, text)
                    onDrawBehind { with(pages) { draw() } }
                }
        )
        Spacer(
            Modifier
                .bookBounds()
                .graphicsLayer {
                    rotationY = frame().coverAngle
                    cameraDistance = COVER_CAMERA_DISTANCE * density
                    transformOrigin = TransformOrigin(0f, .5f)
                }
                .drawWithCache {
                    val cover = BookCoverFaces(size, this, text)
                    onDrawBehind {
                        // backface-visibility: hidden — past edge-on, the inside of the cover faces the camera.
                        with(cover) { if (abs(frame().coverAngle) > 90f) drawInside() else drawFront() }
                    }
                }
        )
    }
}

/**
 * Sizes the element to `.book` (30cqw × 40cqw) and centres it in the parent. On the mockup's 16:10 stage that is
 * (35cqw, 11.25cqw); centring keeps it in the middle of tall windows too.
 */
private fun Modifier.bookBounds(): Modifier = this.layout { measurable, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val u = width / 100f
    val placeable = measurable.measure(Constraints.fixed((BOOK_WIDTH * u).roundToInt(), (BOOK_HEIGHT * u).roundToInt()))
    layout(width, height) {
        placeable.place(((width - placeable.width) / 2f).roundToInt(), ((height - placeable.height) / 2f).roundToInt())
    }
}

/** The right-rounded page block shape shared by `.pages` and `.cover .back`; [u] is 1cqw of the host. */
private fun pageShape(size: Size, u: Float) = Path().apply {
    val corner = CornerRadius(.6f * u)
    addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius.Zero, corner, corner, CornerRadius.Zero))
}

/** A [Paint] for the blurred `1.4cqw 1.8cqw 3cqw #0007` drop shadow; made per `drawWithCache`. */
private fun dropShadowPaint(u: Float) = Paint().apply {
    color = BookDropShadow
    // CSS blur radius 3cqw is a Gaussian with sigma = radius / 2.
    skiaPaint.maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, 1.5f * u)
}

/** `inset 1.4cqw 0 1.6cqw -1cqw #0000003a`: a soft shade 1.2cqw wide along the left edge. */
private fun gutterShade(u: Float) = Brush.horizontalGradient(listOf(BookGutterShadow, Color.Transparent), 0f, 1.2f * u)

/** `.pages` for one book [size] (in the host's 30cqw × 40cqw); built in `drawWithCache`. */
private class BookPagesFace(size: Size, density: Density, text: TextMeasurer) {
    // The book is 30cqw wide.
    private val u = size.width / BOOK_WIDTH
    private val shape = pageShape(size, u)
    private val shadow = dropShadowPaint(u)
    private val gutter = gutterShade(u)
    private val gutterSize = Size(1.2f * u, size.height)

    /** `.pages h4`: italic 600 3.4cqw serif. */
    private val heading = text.measure(
        "Chapter One",
        TextStyle(
            color = BookInk,
            fontSize = with(density) { (3.4f * u).toSp() },
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
        ),
    )
    private val headingAt = Offset(3 * u, 4 * u)

    /** Eight `.pages i` bars below the heading (margin .6cqw, grid gap 1.4cqw); every third grid child is 80% wide. */
    private val lines: List<Rect> = run {
        val content = size.width - 6 * u
        var y = 4 * u + heading.size.height + .6f * u + 1.4f * u
        List(8) { index ->
            // Grid children are h4 then the bars, so bars 2, 5 and 8 are children 3, 6 and 9.
            val width = if ((index + 2) % 3 == 0) content * .8f else content
            Rect(Offset(3 * u, y), Size(width, .8f * u)).also { y += 2.2f * u }
        }
    }
    private val lineCorner = CornerRadius(.4f * u)

    fun DrawScope.draw() {
        translate(1.4f * u, 1.8f * u) { drawIntoCanvas { it.drawPath(shape, shadow) } }
        translate(.7f * u, .7f * u) { drawPath(shape, BookPageEdgeDeep) }
        translate(.35f * u, .35f * u) { drawPath(shape, BookPageEdge) }
        drawPath(shape, BookPages)
        drawRect(gutter, size = gutterSize)
        drawText(heading, topLeft = headingAt)
        for (i in lines.indices) {
            val line = lines[i]
            drawRoundRect(BookTextLine, line.topLeft, line.size, lineCorner)
        }
    }
}

/** `.cover .front` and `.cover .back` for one book [size]; built in `drawWithCache`. */
private class BookCoverFaces(size: Size, density: Density, text: TextMeasurer) {
    private val u = size.width / BOOK_WIDTH

    /** `.cover .front`: `border-radius: 0 .8cqw .8cqw 0`. */
    private val frontShape = Path().apply {
        val corner = CornerRadius(.8f * u)
        addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius.Zero, corner, corner, CornerRadius.Zero))
    }

    /** `linear-gradient(100deg, #123527 0 6%, #1f4d3c 8% 100%)`, laid along the CSS gradient line. */
    private val frontFill: Brush = run {
        val angle = Math.toRadians(100.0)
        val dx = sin(angle).toFloat()
        val dy = -cos(angle).toFloat()
        val half = (abs(size.width * dx) + abs(size.height * dy)) / 2
        val centre = Offset(size.width / 2, size.height / 2)
        Brush.linearGradient(
            0f to BookCoverSpine,
            .06f to BookCoverSpine,
            .08f to BookCover,
            1f to BookCover,
            start = centre - Offset(dx, dy) * half,
            end = centre + Offset(dx, dy) * half,
        )
    }
    private val shadow = dropShadowPaint(u)

    /** `inset 0 0 0 1.6cqw #c9a85c80` under `inset 0 0 0 1.4cqw #1f4d3c`: a gilt rule 1.4..1.6cqw in. */
    private val giltBand = Stroke(1.6f * u)
    private val giltAt = Offset(.8f * u, .8f * u)
    private val giltSize = Size(size.width - 1.6f * u, size.height - 1.6f * u)
    private val coverBand = Stroke(1.4f * u)
    private val coverBandAt = Offset(.7f * u, .7f * u)
    private val coverBandSize = Size(size.width - 1.4f * u, size.height - 1.4f * u)

    /** `.cover .front span`: italic 600 4.6cqw serif, line-height 1.05. */
    private val title = text.measure(
        "NovelWorld",
        TextStyle(
            color = BookGilt,
            fontSize = with(density) { (4.6f * u).toSp() },
            lineHeight = 1.05.em,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
        ),
    )

    /** `.cover .front span small`: 500 1.5cqw mono, letter-spacing .3em, margin-top 1.4cqw. */
    private val volume = text.measure(
        "VOL. I",
        TextStyle(
            color = BookGiltSmall,
            fontSize = with(density) { (1.5f * u).toSp() },
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            letterSpacing = .3.em,
        ),
    )

    // `place-items: center`: the title block sits in the middle of the cover.
    private val titleTop = (size.height - title.size.height - 1.4f * u - volume.size.height) / 2
    private val titleAt = Offset((size.width - title.size.width) / 2, titleTop)
    private val volumeAt = Offset((size.width - volume.size.width) / 2, titleTop + title.size.height + 1.4f * u)

    private val insideShape = pageShape(size, u)
    private val gutter = gutterShade(u)
    private val gutterSize = Size(1.2f * u, size.height)

    fun DrawScope.drawFront() {
        translate(1.4f * u, 1.8f * u) { drawIntoCanvas { it.drawPath(frontShape, shadow) } }
        clipPath(frontShape) {
            drawRect(frontFill)
            drawRect(BookGiltRule, giltAt, giltSize, style = giltBand)
            drawRect(BookCover, coverBandAt, coverBandSize, style = coverBand)
        }
        drawText(title, topLeft = titleAt)
        drawText(volume, topLeft = volumeAt)
    }

    /**
     * `.cover .back`, drawn in the cover's own (mirrored once open) coordinates: its rounded corners land on the
     * outer edge and its inset shade along the spine, as the mockup's `rotateY(180deg)` back face does.
     */
    fun DrawScope.drawInside() {
        drawPath(insideShape, BookCoverInside)
        drawRect(gutter, size = gutterSize)
    }
}
