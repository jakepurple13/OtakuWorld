package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.asState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val MiniWidth = 250.dp
private val MiniInset = 16.dp
private val MiniShape = RoundedCornerShape(12.dp)

/** How long a window's focus loss waits, so focus moving to another app window is not the app losing it. */
private const val FOCUS_LOSS_GRACE_MS = 150L

/**
 * The mini player, drawn in [window]'s overlay while [window] owns the tab it shows. 250 dp wide, 16 dp inside the
 * content area at the saved corner, and draggable for the session. A click returns to the tab; × stops the video.
 * It shrinks from the content area into the corner and grows back when its tab returns (Reduced fades, Off is
 * instant).
 */
@Composable
fun MiniPlayerOverlay(registry: MiniPlayerRegistry, window: BrowserWindowState) {
    val controller = registry.controller
    val motion = controller.motion

    val focused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(focused) {
        if (!focused) delay(FOCUS_LOSS_GRACE_MS)
        registry.windowFocusChanged(window.id, focused)
    }
    DisposableEffect(window.id) { onDispose { registry.forgetWindow(window.id) } }

    val showing by registry.showing.collectAsState()
    // Reads the windows' tab lists, so the mini player follows its tab to another window.
    val mine = showing?.takeIf { registry.showingWindow == window.id }

    // The tab drawn now; kept through the exit motion after [mine] is gone.
    var shown by remember { mutableStateOf<TabId?>(null) }
    var shownSource by remember { mutableStateOf<MiniPlayerSource?>(null) }
    // 0 = the content area (or invisible), 1 = in the corner.
    val progress = remember { Animatable(0f) }
    // Whether [progress] moves the player between the content and the corner, or only fades it.
    var geometry by remember { mutableStateOf(true) }

    LaunchedEffect(mine) {
        val spec = tween<Float>(motion.ms(TabMotion.MiniPlayer), easing = LinearEasing)
        if (mine != null) {
            shown = mine
            shownSource = registry.sourceOf(mine)
            geometry = !motion.reduced
            progress.snapTo(0f)
            progress.animateTo(1f, spec)
        } else if (shown != null) {
            val returning = shown?.let { tab -> window.tabs.any { it.id == tab } && window.activeId.value == tab } == true
            geometry = !motion.reduced && returning
            progress.animateTo(0f, spec)
            shown = null
            shownSource = null
        }
    }

    val tab = shown ?: return
    val source = shownSource ?: return
    val corner by miniPlayerCornerPref.asState()
    var drag by registry.drag
    var layerOrigin by remember { mutableStateOf(Offset.Zero) }
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val width = with(density) { MiniWidth.toPx() }
    val size = Size(width, width * 9f / 16f)
    val inset = with(density) { MiniInset.roundToPx() }

    fun area(): Rect = motion.contentBounds[window.id]
        ?: Rect(layerOrigin, Size(layerSize.width.toFloat(), layerSize.height.toFloat()))

    fun cornerRect(dragBy: Offset = drag): Rect {
        val area = area()
        val container = IntSize(area.width.roundToInt(), area.height.roundToInt())
        val player = IntSize(size.width.roundToInt(), size.height.roundToInt())
        val at = miniPlayerOffset(corner, container, player, inset, dragBy)
        return Rect(Offset(area.left + at.x, area.top + at.y), size)
    }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                layerOrigin = it.positionInWindow()
                layerSize = it.size
            },
    ) {
        val easing = motion.easing(TabMotion.MiniPlayer)
        Box(
            Modifier
                .layout { measurable, _ ->
                    val f = easing.transform(progress.value)
                    val r = if (geometry) lerpRect(area(), cornerRect(), f) else cornerRect()
                    val placeable = measurable.measure(
                        Constraints.fixed(r.width.roundToInt().coerceAtLeast(0), r.height.roundToInt().coerceAtLeast(0))
                    )
                    layout(0, 0) {
                        placeable.place((r.left - layerOrigin.x).roundToInt(), (r.top - layerOrigin.y).roundToInt())
                    }
                }
                .graphicsLayer {
                    alpha = if (geometry) 1f else progress.value
                    shape = MiniShape
                    clip = true
                    shadowElevation = 8.dp.toPx()
                }
                .background(Color.Black)
                .pointerInput(tab) {
                    detectDragGestures { change, amount ->
                        change.consume()
                        // Store what the clamp allowed, so dragging back from an edge responds at once.
                        val base = cornerRect(Offset.Zero).topLeft
                        drag = cornerRect(drag + amount).topLeft - base
                    }
                }
                .clickable { controller.activate(tab) },
        ) {
            val frame by source.frame.collectAsState()
            frame?.let {
                Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))),
            ) {
                Text(
                    text = source.title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp, end = 40.dp),
                )
                IconButton(
                    onClick = { registry.dismiss(tab) },
                    modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close mini player", tint = Color.White)
                }
            }
        }
    }
}

private fun lerpRect(from: Rect, to: Rect, f: Float) = Rect(
    left = from.left + (to.left - from.left) * f,
    top = from.top + (to.top - from.top) * f,
    right = from.right + (to.right - from.right) * f,
    bottom = from.bottom + (to.bottom - from.bottom) * f,
)
