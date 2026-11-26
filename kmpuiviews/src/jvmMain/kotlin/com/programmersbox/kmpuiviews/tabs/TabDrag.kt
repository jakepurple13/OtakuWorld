package com.programmersbox.kmpuiviews.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.awt.MouseInfo
import java.awt.Window

/** The AWT window the composition is in, provided by each app window. */
val LocalAwtWindow = staticCompositionLocalOf<Window?> { null }

/** Registers [window]'s on-screen geometry with [controller] while it is composed, so drags can find its strip. */
@Composable
fun RegisterWindowGeometry(controller: TabsController, window: WindowId) {
    val awt = LocalAwtWindow.current ?: return
    val density by rememberUpdatedState(LocalDensity.current.density)
    DisposableEffect(controller, window, awt) {
        controller.ui.geometry[window] = WindowGeometry(
            screenOrigin = {
                // The content pane holds the composition; fall back to the frame when it isn't showing.
                val at = runCatching { awt.let { (it as? javax.swing.RootPaneContainer)?.contentPane ?: it }.locationOnScreen }
                    .getOrElse { awt.location }
                Offset(at.x.toFloat(), at.y.toFloat())
            },
            density = { density },
            size = { Size(awt.width.toFloat(), awt.height.toFloat()) },
        )
        onDispose { controller.ui.geometry.remove(window) }
    }
}

/** Where [drag] would land at [point], from every window's strip and tabs as they are on screen now. */
internal fun TabsController.dropTargetFor(drag: TabDrag, point: Offset): DropTarget {
    val side = motion.position == TabPosition.Side
    val rects = windows.mapNotNull { w -> ui.geometry[w.id]?.let { w to it } }
    val windowRects = rects.map { (w, g) -> w.id to Rect(g.screenOrigin(), g.size()) }
    val strips = rects.mapNotNull { (w, g) ->
        val strip = ui.stripBounds[w.id]?.let(g::toScreen) ?: return@mapNotNull null
        // The whole title bar band counts as another window's top strip, not just its tabs.
        val frame = windowRects.first { it.first == w.id }.second
        w.id to if (side || w.id == drag.from) strip else Rect(frame.left, strip.top, frame.right, strip.bottom)
    }.toMap()
    // The source's own chips include the dragged tab's, so a reorder index counts its slot.
    val chips = rects.associate { (w, g) -> w.id to w.tabs.mapNotNull { motion.chipBounds[it.id]?.let(g::toScreen) } }
    return dropTarget(point, windowRects, strips, drag.from, side, chips, prefs.tearOut)
}

/** Ends [drag]: drops its tab and, for a tear-out into a new window, starts the tear-out motion. */
internal fun TabsController.finishDrag(drag: TabDrag) {
    val target = drag.target
    val before = windows.map { it.id }.toSet()
    val landed = drop(drag.tab, target) ?: return
    if (target is DropTarget.TearOut && landed !in before) {
        val style = tearOutStyle(motion.level, overlaySupported())
        if (style != TearOutStyle.None) ui.tearOuts[landed] = TearOutRun(drag.origin, style)
    }
}

/** The pointer in screen dp: AWT's, or [fallback] when the platform won't say. */
private fun pointerOnScreen(fallback: () -> Offset): Offset =
    MouseInfo.getPointerInfo()?.location?.let { Offset(it.x.toFloat(), it.y.toFloat()) } ?: fallback()

/**
 * Lets [tab] be dragged out of [window]'s strip: past touch slop the drag starts, and the release drops it (reorder,
 * move to another window, or tear out). The press and release of a drag are consumed, so the chip isn't activated by
 * its own click. Pinned tabs (Home) don't drag.
 */
@Composable
internal fun Modifier.tabDragSource(tab: TabState, window: BrowserWindowState, controller: TabsController): Modifier {
    if (tab.pinned) return this
    var chip by remember { mutableStateOf(Rect.Zero) }
    return onGloballyPositioned { chip = it.boundsInWindow() }
        .pointerInput(tab.id, window.id) {
            awaitPointerEventScope {
                while (true) {
                    val press = awaitPointerEvent(PointerEventPass.Initial)
                    if (press.type != PointerEventType.Press || !press.buttons.isPrimaryPressed) continue
                    val down = press.changes.first()
                    var drag: TabDrag? = null
                    while (true) {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes
                            .firstOrNull { it.id == down.id } ?: break
                        val geometry = controller.ui.geometry[window.id]
                        val point = pointerOnScreen {
                            val at = chip.topLeft + change.position
                            geometry?.toScreen(Rect(at, at))?.topLeft ?: at
                        }
                        if (drag == null && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                            drag = TabDrag(tab.id, window.id, geometry?.toScreen(chip) ?: chip)
                                .also { controller.ui.drag = it }
                        }
                        drag?.let {
                            change.consume()
                            it.point = point
                            it.target = controller.dropTargetFor(it, point)
                        }
                        if (!change.pressed) {
                            drag?.let {
                                controller.ui.drag = null
                                controller.finishDrag(it)
                            }
                            break
                        }
                    }
                    // A cancelled gesture (the pointer left the window's event stream) drops nothing.
                    if (controller.ui.drag === drag) controller.ui.drag = null
                }
            }
        }
}

/**
 * Drag feedback in [window]'s overlay: a drop indicator in its strip while a drag would land there, and a dashed
 * preview of the new window that follows the pointer once a drop would tear out.
 */
@Composable
fun TabDragLayer(controller: TabsController, window: BrowserWindowState, modifier: Modifier = Modifier) {
    val drag = controller.ui.drag ?: return
    val accent = MaterialTheme.colorScheme.primary
    var layerOrigin by remember { mutableStateOf(Offset.Zero) }
    Canvas(modifier.fillMaxSize().onGloballyPositioned { layerOrigin = it.positionInWindow() }) {
        val target = drag.target
        val index = when {
            target is DropTarget.Reorder && target.window == window.id -> target.index
            target is DropTarget.MoveTo && target.window == window.id -> target.index
            else -> null
        }
        if (index != null) {
            val side = controller.motion.position == TabPosition.Side
            val chips = window.tabs.mapNotNull { controller.motion.chipBounds[it.id] }
            val anchor = chips.getOrNull(index) ?: chips.lastOrNull()
            if (anchor != null) {
                val gap = 2.dp.toPx()
                val before = index < chips.size
                val line = if (side) {
                    val y = if (before) anchor.top - gap else anchor.bottom + gap
                    Rect(anchor.left, y - gap / 2, anchor.right, y + gap / 2)
                } else {
                    val x = if (before) anchor.left - gap else anchor.right + gap
                    Rect(x - gap / 2, anchor.top, x + gap / 2, anchor.bottom)
                }
                drawRect(accent, topLeft = line.topLeft - layerOrigin, size = line.size)
            }
        }
        if (target is DropTarget.TearOut && drag.from == window.id) {
            val geometry = controller.ui.geometry[window.id] ?: return@Canvas
            val b = target.bounds
            val r = geometry.fromScreen(Rect(b.x.value, b.y.value, b.x.value + b.width.value, b.y.value + b.height.value))
            drawRoundRect(
                color = accent,
                topLeft = r.topLeft - layerOrigin,
                size = r.size,
                cornerRadius = CornerRadius(10.dp.toPx()),
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))),
            )
        }
    }
}
