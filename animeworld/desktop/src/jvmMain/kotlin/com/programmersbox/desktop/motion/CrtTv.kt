package com.programmersbox.desktop.motion

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.motion.cqw
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random
import org.jetbrains.skia.Image as SkiaImage

/*
 * The CRT pieces shared by `CRT power-on` and `CRT power-off` in docs/mockups/desktop-launch-quit-animations.html
 * (CSS `.room`, `.tv`, `.tv::after`, `.scr`, `.scan`, `.glow`).
 */

/** `.tv` background: the screen while it is off. */
private val CrtScreenOff = Color(0xFF0A0C10)

/** `.tv` box-shadow inner ring (2.4cqw spread). */
private val CrtBezel = Color(0xFF262833)

/** `.tv` box-shadow outer ring (2.8cqw spread). */
private val CrtBezelEdge = Color(0xFF101117)

/** `.tv::after` stand. */
private val CrtStand = Color(0xFF1D1F27)

/** `.scan` dark line colour (`#0000004d`). */
private val CrtScanline = Color(0x4D000000)

/** `.glow` box-shadow colour. */
private val CrtGlowHalo = Color(0xFFCFE0FF)

/** `.tv` / `.scr` border-radius, cqw. */
private const val SCREEN_RADIUS = 1.6f

/** `.glow`: a 2cqw white dot with `box-shadow: 0 0 3cqw 1.4cqw` → visible out to 1 + 1.4 + 3cqw. */
private const val GLOW_RADIUS = 5.4f

/**
 * The TV screen (`.tv` / `.scr`): centred, 60% of the host each way. On the mockup's 16:10 stage that is
 * 60cqw × 37.5cqw at (20cqw, 12.5cqw); fractions keep it matching the app scaled by .6 in any window shape.
 */
private fun Size.crtScreen() = Rect(width * .2f, height * .2f, width * .8f, height * .8f)

/** The TV set (bezel, screen, stand) and the screen-sized overlays, for one host size; built in `drawWithCache`. */
internal class CrtTv(size: Size, density: Density) {
    private val u = size.cqw
    val screen = size.crtScreen()
    private val screenCorner = CornerRadius(SCREEN_RADIUS * u)
    private val bezel = screen.inflate(2.4f * u)
    private val bezelCorner = CornerRadius((SCREEN_RADIUS + 2.4f) * u)
    private val bezelEdge = screen.inflate(2.8f * u)
    private val bezelEdgeCorner = CornerRadius((SCREEN_RADIUS + 2.8f) * u)

    /** `.scr` clip: the rounded screen. */
    val screenClip = Path().apply { addRoundRect(RoundRect(screen, screenCorner)) }

    /** `.tv::after`: 16cqw × 2.6cqw, its bottom 6.2cqw below the screen, bottom corners rounded 1cqw. */
    private val stand = Path().apply {
        val foot = CornerRadius(u)
        val rect = Rect(screen.center.x - 8 * u, screen.bottom + 3.6f * u, screen.center.x + 8 * u, screen.bottom + 6.2f * u)
        addRoundRect(RoundRect(rect, CornerRadius.Zero, CornerRadius.Zero, foot, foot))
    }

    /** `.scan`: `repeating-linear-gradient(#0000004d 0 1px, transparent 1px 3px)`: 1dp lines every 3dp (CSS px are dp). */
    private val scanlines = Path().apply {
        val line = with(density) { 1.dp.toPx() }
        var y = screen.top
        while (y < screen.bottom) {
            addRect(Rect(screen.left, y, screen.right, minOf(y + line, screen.bottom)))
            y += 3 * line
        }
    }

    /** `.noise` tile size: `background-size: 14cqw`. */
    private val noiseTile = ceil(14 * u).toInt().coerceAtLeast(1)

    /** `.tv` (with its box-shadow rings) and `.tv::after`. The blurred `0 3cqw 6cqw #000a` drop shadow is left out: it is invisible on `.room`. */
    fun DrawScope.drawSet() {
        drawPath(stand, CrtStand)
        drawRoundRect(CrtBezelEdge, bezelEdge.topLeft, bezelEdge.size, bezelEdgeCorner)
        drawRoundRect(CrtBezel, bezel.topLeft, bezel.size, bezelCorner)
        drawRoundRect(CrtScreenOff, screen.topLeft, screen.size, screenCorner)
    }

    /** `.scan` at [alpha]. */
    fun DrawScope.drawScanlines(alpha: Float) {
        if (alpha > 0f) clipPath(screenClip) { drawPath(scanlines, CrtScanline, alpha = alpha) }
    }

    /**
     * `.scr` static at [alpha]: the noise tile repeated over the screen. The mockup steps `background-position`
     * through `((i*37)%96px, (i*61)%96px)` for i = 0..4, 48 ms each (`steps(1,end)` over 240 ms), so the static
     * jumps rather than slides; offsets are scaled from the 96px tile to [noiseTile].
     */
    fun DrawScope.drawStatic(time: Int, alpha: Float) {
        if (alpha <= 0f) return
        val step = (time / 48) % 5
        val dx = (step * 37 % 96) * noiseTile / 96
        val dy = (step * 61 % 96) * noiseTile / 96
        val tileSize = IntSize(noiseTile, noiseTile)
        clipPath(screenClip) {
            var y = screen.top.toInt() - noiseTile + dy
            while (y < screen.bottom) {
                var x = screen.left.toInt() - noiseTile + dx
                while (x < screen.right) {
                    drawImage(CrtNoise, IntOffset.Zero, CrtNoiseSize, IntOffset(x, y), tileSize, alpha = alpha)
                    x += noiseTile
                }
                y += noiseTile
            }
        }
    }
}

/** `.glow`: the bright dot in the middle of the host, for one host size; built in `drawWithCache`. */
internal class CrtGlow(size: Size) {
    private val centre = size.center
    private val radius = GLOW_RADIUS * size.cqw
    private val brush = Brush.radialGradient(
        0f to Color.White,
        // 1cqw white core
        1f / GLOW_RADIUS to Color.White,
        1.2f / GLOW_RADIUS to CrtGlowHalo,
        // box-shadow spread edge (1 + 1.4cqw), half covered by the 3cqw blur
        2.4f / GLOW_RADIUS to CrtGlowHalo.copy(alpha = .5f),
        1f to CrtGlowHalo.copy(alpha = 0f),
        center = centre,
        radius = radius,
    )

    /** The dot at [alpha], scaled by [scale] about the host centre. */
    fun DrawScope.drawGlow(alpha: Float, scale: Float = 1f) {
        if (alpha > 0f) drawCircle(brush, radius * scale, centre, alpha = alpha)
    }
}

private val CrtNoiseSize = IntSize(96, 96)

/** The mockup's `noiseTile()`: 96×96 opaque grey static, generated once. */
private val CrtNoise: ImageBitmap by lazy {
    val random = Random(96)
    val pixels = ByteArray(96 * 96 * 4)
    for (i in pixels.indices step 4) {
        val v = random.nextInt(256).toByte()
        pixels[i] = v
        pixels[i + 1] = v
        pixels[i + 2] = v
        pixels[i + 3] = -1
    }
    SkiaImage.makeRaster(ImageInfo.makeN32(96, 96, ColorAlphaType.OPAQUE), pixels, 96 * 4).toComposeImageBitmap()
}

/** CSS `filter: brightness(x)` steps: 1/20 apart from 1 up to [MAX_BRIGHTNESS]. */
private const val BRIGHTNESS_STEPS = 20
private const val MAX_BRIGHTNESS = 8

/** Every `brightness(x)` step as a colour matrix (RGB scaled by x, clamped by the layer; alpha unchanged), built once. */
private val brightnessFilters: Array<ColorFilter> by lazy {
    Array((MAX_BRIGHTNESS - 1) * BRIGHTNESS_STEPS + 1) { index ->
        val x = 1f + index.toFloat() / BRIGHTNESS_STEPS
        ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(x, x, x, 1f) })
    }
}

/**
 * Draws the content through CSS `brightness([brightness])` into [bounds]; no extra layer at 1.
 * [paint] is the caller's own, made once in its `drawWithCache`.
 */
internal fun ContentDrawScope.drawBrightened(bounds: Rect, brightness: Float, paint: Paint) {
    if (brightness <= 1f) {
        drawContent()
        return
    }
    val index = ((brightness - 1) * BRIGHTNESS_STEPS).roundToInt().coerceIn(0, brightnessFilters.lastIndex)
    paint.colorFilter = brightnessFilters[index]
    drawIntoCanvas { canvas ->
        canvas.saveLayer(bounds, paint)
        drawContent()
        canvas.restore()
    }
}
