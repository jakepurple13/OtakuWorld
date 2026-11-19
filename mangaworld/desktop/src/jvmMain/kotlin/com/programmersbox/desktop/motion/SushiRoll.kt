package com.programmersbox.desktop.motion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import com.programmersbox.kmpuiviews.motion.DesktopMotion
import kotlin.math.cos
import kotlin.math.sin

/**
 * One frame of the sushi scene. Lengths are in cqw (percent of the host width) and angles in
 * degrees, as in the mockup; the insets are the `clip-path: inset(0 right 0 left)` percentages.
 */
internal class SushiFrame(
    /** App and rice sheet clip. */
    val insetLeft: Float,
    val insetRight: Float,
    val sheetAlpha: Float,
    val boardAlpha: Float,
    /** `.rw` translateY: moves the roll and the chopsticks together. */
    val rollDrop: Float,
    /** `.rx` translateX: the roll only. */
    val rollTravel: Float,
    /** `.maki` rotate. */
    val rollSpin: Float,
    /** How far each chopstick moves outwards. */
    val stickSpread: Float,
    /** How far each chopstick leans in. */
    val stickTilt: Float,
    /** The chopsticks' own translateY. */
    val stickLift: Float,
)

/**
 * Board behind the app, rice sheet + maki roll + chopsticks in front, app revealed by a clip.
 * Subclasses only supply the keyframe tracks; time is read in draw/layer lambdas only.
 */
internal abstract class SushiScene : DesktopMotion {
    abstract fun frame(time: Int): SushiFrame

    override fun Modifier.app(time: () -> Int): Modifier = drawWithContent {
        clipReveal(frame(time())) { this@drawWithContent.drawContent() }
    }

    @Composable
    override fun BoxScope.Behind(time: () -> Int) {
        Canvas(Modifier.matchParentSize()) { drawBoard(frame(time()).boardAlpha) }
    }

    @Composable
    override fun BoxScope.InFront(time: () -> Int) {
        Canvas(Modifier.matchParentSize().graphicsLayer { alpha = frame(time()).sheetAlpha }) {
            clipReveal(frame(time())) { drawRiceSheet() }
        }
        Canvas(Modifier.matchParentSize()) {
            val frame = frame(time())
            drawRoll(frame)
            drawChopsticks(frame)
        }
    }
}

private val BoardStripes = arrayOf(
    0f to Color(0xFFC79B6B), .3f to Color(0xFFC79B6B),
    .3f to Color(0xFFBF9161), .46f to Color(0xFFBF9161),
    .46f to Color(0xFFCDA274), .9f to Color(0xFFCDA274),
    .9f to Color(0xFFC39566), 1f to Color(0xFFC39566),
)
private val SheetRice = Color(0xFFF4EFE3)
private val GrainLight = Color(0xFFFFFDF6)
private val GrainShade = Color(0xFFEEE7D8)
private val SalmonLight = Color(0xFFFF9A6C)
private val SalmonDark = Color(0xFFEE7547)
private val CucumberEdge = Color(0xFF7CB35B)
private val Nori = Color(0xFF1C271D)
private val RollShadow = Color(0x66000000)
private val RollRice = Color(0xFFF1EBDD)
private val FishLight = Color(0xFFFFAB7E)
private val FishDark = Color(0xFFF0703F)
private val Cucumber = Color(0xFF86C35C)
private val CucumberSkin = Color(0xFF2F6B2A)
private val StickLight = Color(0xFFD29E66)
private val StickDark = Color(0xFFA8743F)

/** 1cqw in pixels. */
private val DrawScope.cqw get() = size.width / 100

/** Roll centre x before travel: `.rw` left 6cqw + half its 20cqw width. */
private const val ROLL_X = 16f

private inline fun DrawScope.clipReveal(frame: SushiFrame, block: DrawScope.() -> Unit) = clipRect(
    left = size.width * frame.insetLeft / 100,
    right = size.width * (1 - frame.insetRight / 100),
    block = block,
)

/** `.board`: repeating-linear-gradient(92deg, …) with a 10cqw period. */
private fun DrawScope.drawBoard(alpha: Float) {
    val angle = Math.toRadians(92.0)
    val period = Offset(sin(angle).toFloat(), -cos(angle).toFloat()) * (10 * cqw)
    drawRect(
        Brush.linearGradient(*BoardStripes, start = Offset.Zero, end = period, tileMode = TileMode.Repeated),
        alpha = alpha,
    )
}

/** `.nori`: two offset grids of rice grains, then the salmon stripe with its cucumber edge (`::after`). */
private fun DrawScope.drawRiceSheet() {
    val u = cqw
    drawRect(SheetRice)
    val all = Rect(Offset.Zero, size)
    val grain = Size(.52f * u, .26f * u)
    val step = Size(3 * u, 2.2f * u)
    drawGrains(all, first = Offset(1.5f * u, 1.1f * u), step, grain, GrainLight)
    drawGrains(all, first = Offset.Zero, step, grain, GrainShade)

    val top = size.height * .43f
    val height = size.height * .14f
    drawRect(
        Brush.verticalGradient(listOf(SalmonLight, SalmonDark), startY = top, endY = top + height),
        topLeft = Offset(0f, top),
        size = Size(size.width, height),
    )
    drawRect(CucumberEdge, topLeft = Offset(0f, top + height - u), size = Size(size.width, u))
}

/** `.maki`: a 20cqw nori circle with rice, fish and cucumber, spun about its centre. */
private fun DrawScope.drawRoll(frame: SushiFrame) {
    val u = cqw
    val centre = Offset((ROLL_X + frame.rollTravel) * u, size.height / 2 + frame.rollDrop * u)
    // box-shadow: 0 1.4cqw 2cqw #0006
    val shadowCentre = centre + Offset(0f, 1.4f * u)
    drawCircle(
        Brush.radialGradient(.8f to RollShadow, 1f to Color.Transparent, center = shadowCentre, radius = 11 * u),
        radius = 11 * u,
        center = shadowCentre,
    )
    rotate(frame.rollSpin, pivot = centre) {
        val origin = centre - Offset(10 * u, 10 * u)
        drawCircle(Nori, radius = 10 * u, center = centre)

        val rice = Rect(centre, 8.7f * u)
        drawCircle(RollRice, radius = rice.width / 2, center = centre)
        clipPath(Path().apply { addOval(rice) }) {
            drawGrains(rice, first = rice.topLeft + Offset(1.2f * u, .9f * u), Size(2.4f * u, 1.8f * u), Size(.47f * u, .23f * u), GrainLight)
        }

        // .fish: inset 5.6 4.6 5 5.4 (cqw), border-radius 42% 58% 50% 50%, 135deg gradient
        val fish = Rect(origin + Offset(5.4f * u, 5.6f * u), Size(10 * u, 9.4f * u))
        fun corner(fraction: Float) = CornerRadius(fish.width * fraction, fish.height * fraction)
        drawPath(
            Path().apply { addRoundRect(RoundRect(fish, corner(.42f), corner(.58f), corner(.5f), corner(.5f))) },
            Brush.linearGradient(listOf(FishLight, FishDark), start = fish.topLeft, end = fish.bottomRight),
        )

        // .cuc: 3.2cqw circle at (11.5, 5) with a .6cqw skin
        val cucumber = origin + Offset(13.1f * u, 6.6f * u)
        drawCircle(CucumberSkin, radius = 1.6f * u, center = cucumber)
        drawCircle(Cucumber, radius = 1f * u, center = cucumber)
    }
}

/** `.stick`: 48cqw tapered sticks either side of the roll, each leaning in about its tip. */
private fun DrawScope.drawChopsticks(frame: SushiFrame) {
    val u = cqw
    val tipY = size.height / 2 + (frame.rollDrop + frame.stickLift - 1.4f) * u
    for (side in floatArrayOf(-1f, 1f)) {
        val tip = Offset((ROLL_X + side * (10.6f + frame.stickSpread)) * u, tipY)
        rotate(-side * frame.stickTilt, pivot = tip) {
            val stick = Path().apply {
                moveTo(tip.x - .33f * u, tip.y)
                lineTo(tip.x + .33f * u, tip.y)
                lineTo(tip.x + .75f * u, tip.y - 48 * u)
                lineTo(tip.x - .75f * u, tip.y - 48 * u)
                close()
            }
            drawPath(stick, Brush.horizontalGradient(listOf(StickLight, StickDark), startX = tip.x - .75f * u, endX = tip.x + .75f * u))
        }
    }
}

/** Ovals of [radius] every [step] from [first], covering [area]. */
private fun DrawScope.drawGrains(area: Rect, first: Offset, step: Size, radius: Size, color: Color) {
    var y = first.y
    while (y - radius.height < area.bottom) {
        var x = first.x
        while (x - radius.width < area.right) {
            drawOval(color, topLeft = Offset(x - radius.width, y - radius.height), size = radius * 2f)
            x += step.width
        }
        y += step.height
    }
}
