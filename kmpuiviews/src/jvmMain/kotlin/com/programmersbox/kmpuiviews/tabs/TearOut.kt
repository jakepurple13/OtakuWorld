package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.programmersbox.kmpuiviews.motion.OverlayPlatform
import kotlin.math.roundToInt

/** How a torn-out window appears. */
enum class TearOutStyle {
    /** Grows from its tab in a transparent overlay window (450 ms), then the window shows. */
    Overlay,

    /** Shows at its final spot, scaling up from 0.92 and fading in (380 ms). */
    Lift,

    /** Just appears. */
    None,
}

/** Only Full tab motion animates a tear-out; Reduced and Off (and the OS reduce-motion setting, via System) don't. */
fun tearOutStyle(level: TabMotionLevel, overlay: Boolean): TearOutStyle = when {
    level != TabMotionLevel.Full -> TearOutStyle.None
    overlay -> TearOutStyle.Overlay
    else -> TearOutStyle.Lift
}

/**
 * Whether the transparent tear-out overlay can run. macOS only: it is never used on Windows (develop's motion policy),
 * and Linux stays on Lift until a hand test passes there (then `-Dotaku.tearOutOverlay=true`). [forced] is that JVM
 * property; "false" turns the overlay off on macOS too, for checking Lift.
 */
fun overlaySupported(
    platform: OverlayPlatform = OverlayPlatform.current(),
    forced: String? = System.getProperty("otaku.tearOutOverlay"),
): Boolean = when (platform) {
    OverlayPlatform.Windows -> false
    OverlayPlatform.MacOS -> forced != "false"
    OverlayPlatform.LinuxX11, OverlayPlatform.LinuxWayland -> forced == "true"
}

/** Lift's starting scale. */
private const val LiftFromScale = 0.92f

/**
 * A transparent, undecorated, non-focusable, always-on-top window spanning [from] (the tab, screen dp) and [to]. A
 * rounded rect of [color] grows from the tab to the window's bounds over [durationMs], starting once the overlay's
 * first frame is drawn (the same handshake as `QuitOverlayWindow`). Calls [onDone] at the end.
 */
@Composable
fun TearOutOverlay(
    from: Rect,
    to: WindowBounds,
    color: Color,
    durationMs: Int,
    onDone: () -> Unit,
) {
    val done by rememberUpdatedState(onDone)
    val target = Rect(to.x.value, to.y.value, to.x.value + to.width.value, to.y.value + to.height.value)
    val span = remember(from, target) {
        Rect(
            minOf(from.left, target.left),
            minOf(from.top, target.top),
            maxOf(from.right, target.right),
            maxOf(from.bottom, target.bottom),
        )
    }
    val t = remember { Animatable(0f) }
    Window(
        onCloseRequest = {},
        state = rememberWindowState(
            position = WindowPosition(span.left.dp, span.top.dp),
            size = DpSize(span.width.dp, span.height.dp),
        ),
        title = "",
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = false,
        alwaysOnTop = true,
    ) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            // The first frame has been drawn once the next one starts.
            withFrameNanos { }
            t.animateTo(1f, tween(durationMs, easing = EmphasizedDecelerate))
            done()
        }
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .layout { measurable, _ ->
                        val f = t.value
                        fun lerp(a: Float, b: Float) = a + (b - a) * f
                        val r = Rect(
                            lerp(from.left, target.left) - span.left,
                            lerp(from.top, target.top) - span.top,
                            lerp(from.right, target.right) - span.left,
                            lerp(from.bottom, target.bottom) - span.top,
                        )
                        val px = density
                        val placeable = measurable.measure(
                            Constraints.fixed(
                                (r.width * px).roundToInt().coerceAtLeast(0),
                                (r.height * px).roundToInt().coerceAtLeast(0),
                            )
                        )
                        layout(0, 0) { placeable.place((r.left * px).roundToInt(), (r.top * px).roundToInt()) }
                    }
                    .graphicsLayer {
                        shape = RoundedCornerShape((8f + 2f * t.value).dp)
                        clip = true
                        shadowElevation = 12.dp.toPx() * t.value
                    }
                    .background(color),
            )
        }
    }
}

/**
 * Lift: the window's content scales up from 0.92 and fades in as [progress] goes 0..1 (see [rememberLift]). The window
 * itself has no fade of its own (a decorated window can't change its opacity on every OS), so its frame appears at
 * once.
 */
fun Modifier.lift(progress: () -> Float): Modifier = graphicsLayer {
    val f = progress()
    val scale = LiftFromScale + (1f - LiftFromScale) * f
    scaleX = scale
    scaleY = scale
    alpha = f
}

/** Runs Lift for [run] and returns its progress 0..1 (1 when there is nothing to play). */
@Composable
fun rememberLift(run: TearOutRun?, durationMs: Int, onDone: () -> Unit): () -> Float {
    val done by rememberUpdatedState(onDone)
    val progress = remember { Animatable(if (run?.style == TearOutStyle.Lift) 0f else 1f) }
    LaunchedEffect(run) {
        if (run?.style != TearOutStyle.Lift) return@LaunchedEffect
        // After the window's first frame, so the whole motion is seen.
        withFrameNanos { }
        progress.animateTo(1f, tween(durationMs, easing = EmphasizedDecelerate))
        done()
    }
    return { progress.value }
}
