package com.programmersbox.kmpuiviews.presentation.debug.navmap

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val MIN_ZOOM = 0.25f
private const val MAX_ZOOM = 2.5f

/** World-space geometry of the map, in px. */
private class MapGeometry(val rects: Map<String, Rect>, val columnX: List<Float>, val columnWidth: Float, val height: Float)

/**
 * Pannable, zoomable map of the whole graph. Columns follow [GraphLayout]; tapping a node selects it
 * and lights up its inbound (tertiary) and outbound (primary) edges.
 */
@Composable
fun NavMapCanvas(
    state: NavMapState,
    graph: NavGraph,
    layout: GraphLayout,
    searchTarget: String?,
    /** Changes on every centre request, so asking for the same target twice still re-centres. */
    centerKey: Int,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 256)
    val colors = MaterialTheme.colorScheme

    val visible = remember(graph, state.showEntryPoints) {
        graph.nodes.filter { state.showEntryPoints || !it.pseudo }.associateBy { it.id }
    }
    val geometry = remember(graph, layout, state.showArgs, visible, density) {
        with(density) {
            val colWidth = 176.dp.toPx()
            val nodeWidth = 150.dp.toPx()
            val top = 34.dp.toPx()
            val gap = 10.dp.toPx()
            val y = mutableMapOf<Int, Float>()
            val rects = mutableMapOf<String, Rect>()
            layout.placements.values.sortedWith(compareBy({ it.column }, { it.row })).forEach { p ->
                val node = visible[p.id] ?: return@forEach
                val h = (if (state.showArgs && node.args.isNotEmpty()) 34.dp else 24.dp).toPx()
                val yy = y.getOrElse(p.column) { top }
                rects[p.id] = Rect(Offset(p.column * colWidth + 12.dp.toPx(), yy), Size(nodeWidth, h))
                y[p.column] = yy + h + gap
            }
            MapGeometry(rects, List(layout.columnCount) { it * colWidth }, colWidth, (y.values.maxOrNull() ?: top) + gap)
        }
    }

    var offset by remember { mutableStateOf(Offset(8f, 8f)) }
    var scale by remember { mutableStateOf(0.8f) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    fun centerOn(id: String) {
        val r = geometry.rects[id] ?: return
        scale = max(scale, 0.8f)
        offset = Offset(viewport.width / 2f - r.center.x * scale, viewport.height * 0.38f - r.center.y * scale)
    }

    fun fit() {
        val w = geometry.columnX.size * geometry.columnWidth
        scale = min(viewport.width / w, viewport.height / geometry.height).coerceIn(MIN_ZOOM, MAX_ZOOM)
        offset = Offset((viewport.width - w * scale) / 2f, 8f)
    }

    LaunchedEffect(searchTarget, centerKey, viewport) {
        if (viewport != IntSize.Zero) searchTarget?.let(::centerOn)
    }

    val selection by animateFloatAsState(if (state.selected != null) 1f else 0f, label = "selection")
    val related = remember(state.selected, graph) {
        state.selected?.let { s -> setOf(s) + graph.inbound(s).map { it.from } + graph.outbound(s).map { it.to } }.orEmpty()
    }
    val query = state.query.trim()

    Box(modifier.onSizeChanged { viewport = it }) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        offset = (offset - centroid) * (newScale / scale) + centroid + pan
                        scale = newScale
                    }
                }
                .pointerInput(geometry) {
                    detectTapGestures { tap ->
                        val world = (tap - offset) / scale
                        onSelect(geometry.rects.entries.firstOrNull { it.value.contains(world) }?.key)
                    }
                }
        ) {
            // Dot grid backdrop.
            val dot = 16.dp.toPx() * scale
            if (dot > 6f) {
                var x = offset.x % dot
                while (x < size.width) {
                    var y = offset.y % dot
                    while (y < size.height) {
                        drawCircle(colors.outlineVariant, radius = 1f, center = Offset(x, y)); y += dot
                    }
                    x += dot
                }
            }

            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                geometry.columnX.forEachIndexed { column, x ->
                    if (column == 0 && !state.showEntryPoints) return@forEachIndexed
                    drawRoundRect(
                        colors.surfaceContainerHigh.copy(alpha = 0.5f),
                        topLeft = Offset(x + 4.dp.toPx(), 4.dp.toPx()),
                        size = Size(geometry.columnWidth - 8.dp.toPx(), geometry.height),
                        cornerRadius = CornerRadius(10.dp.toPx()),
                    )
                    val label = when (column) {
                        0 -> "ENTRY POINTS"
                        layout.unreachableColumn -> "NO WAY IN"
                        else -> if (column == 1) "1 STEP" else "$column STEPS"
                    }
                    drawText(
                        measurer, label, Offset(x + 14.dp.toPx(), 12.dp.toPx()),
                        TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.sp, color = colors.onSurfaceVariant),
                    )
                }

                graph.edges.forEach { e ->
                    val from = geometry.rects[e.from] ?: return@forEach
                    val to = geometry.rects[e.to] ?: return@forEach
                    if (state.overridesOnly && !e.isOverride) return@forEach
                    val inbound = e.to == state.selected
                    val outbound = e.from == state.selected
                    val color = when {
                        inbound -> colors.tertiary
                        outbound -> colors.primary
                        else -> colors.outlineVariant.copy(alpha = 1f - 0.9f * selection)
                    }
                    drawEdge(from, to, color, if (inbound || outbound) 2.2.dp.toPx() else 1.2.dp.toPx(), e.isOverride)
                }

                graph.nodes.forEach { n ->
                    val r = geometry.rects[n.id] ?: return@forEach
                    val isSelected = n.id == state.selected
                    val orphan = !n.pseudo && graph.inbound(n.id).isEmpty()
                    val hit = query.isNotEmpty() && (n.id.contains(query, true) || n.label.contains(query, true))
                    var alpha = if (state.selected != null && n.id !in related) 1f - 0.75f * selection else 1f
                    if (state.orphansOnly && !orphan) alpha = min(alpha, 0.2f)
                    val fill = when {
                        isSelected -> colors.primary
                        n.pseudo -> colors.surfaceContainerHighest
                        else -> colors.surface
                    }
                    val stroke = when {
                        hit -> colors.primary
                        orphan -> colors.error
                        else -> colors.outlineVariant
                    }
                    val corner = CornerRadius(7.dp.toPx())
                    drawRoundRect(fill.copy(alpha = fill.alpha * alpha), r.topLeft, r.size, corner)
                    drawRoundRect(
                        stroke.copy(alpha = alpha), r.topLeft, r.size, corner,
                        style = Stroke(
                            width = (if (hit || orphan) 2f else 1f).dp.toPx(),
                            pathEffect = if (n.pseudo) PathEffect.dashPathEffect(floatArrayOf(6f, 4f)) else null,
                        ),
                    )
                    val textColor = (if (isSelected) colors.onPrimary else colors.onSurface).copy(alpha = alpha)
                    drawText(
                        measurer, n.label, Offset(r.left + 8.dp.toPx(), r.top + 5.dp.toPx()),
                        TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = textColor, fontWeight = if (isSelected) FontWeight.SemiBold else null),
                        overflow = TextOverflow.Ellipsis, maxLines = 1,
                        size = Size(r.width - 14.dp.toPx(), 14.dp.toPx()),
                    )
                    if (state.showArgs && n.args.isNotEmpty()) {
                        drawText(
                            measurer, n.args.joinToString { it.name + if (it.nullable) "?" else "" },
                            Offset(r.left + 8.dp.toPx(), r.top + 19.dp.toPx()),
                            TextStyle(fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = textColor.copy(alpha = textColor.alpha * 0.7f)),
                            overflow = TextOverflow.Ellipsis, maxLines = 1,
                            size = Size(r.width - 14.dp.toPx(), 12.dp.toPx()),
                        )
                    }
                }
            }
        }

        Column(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            FilledTonalIconButton(onClick = { zoomAround(viewport, 1.25f, scale, offset) { s, o -> scale = s; offset = o } }) {
                Icon(Icons.Default.Add, "Zoom in")
            }
            FilledTonalIconButton(onClick = { zoomAround(viewport, 0.8f, scale, offset) { s, o -> scale = s; offset = o } }) {
                Icon(Icons.Default.Remove, "Zoom out")
            }
            FilledTonalIconButton(onClick = ::fit) { Icon(Icons.Default.FitScreen, "Fit all") }
            Text(
                "${(scale * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

private fun zoomAround(viewport: IntSize, factor: Float, scale: Float, offset: Offset, apply: (Float, Offset) -> Unit) {
    val center = Offset(viewport.width / 2f, viewport.height / 2f)
    val newScale = (scale * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
    apply(newScale, (offset - center) * (newScale / scale) + center)
}

/** Curve from [from] to [to]: forward edges leave the right side, backward edges the left, same-column edges bulge right. */
private fun DrawScope.drawEdge(from: Rect, to: Rect, color: Color, width: Float, dashed: Boolean) {
    val path = Path()
    if (abs(from.left - to.left) < 1f) {
        val x = from.right
        val bulge = 26.dp.toPx() + min(60.dp.toPx(), abs(to.center.y - from.center.y) * 0.15f)
        path.moveTo(x, from.center.y)
        path.cubicTo(x + bulge, from.center.y, x + bulge, to.center.y, x, to.center.y)
    } else {
        val forward = to.left > from.left
        val x1 = if (forward) from.right else from.left
        val x2 = if (forward) to.left else to.right
        val dx = max(30.dp.toPx(), abs(x2 - x1) * 0.45f) * if (forward) 1 else -1
        path.moveTo(x1, from.center.y)
        path.cubicTo(x1 + dx, from.center.y, x2 - dx, to.center.y, x2, to.center.y)
    }
    drawPath(path, color, style = Stroke(width, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) else null))
    // Arrow head at the target.
    val tip = if (abs(from.left - to.left) < 1f) Offset(to.right, to.center.y) else if (to.left > from.left) Offset(to.left, to.center.y) else Offset(to.right, to.center.y)
    val dir = if (abs(from.left - to.left) < 1f || to.left < from.left) 1f else -1f
    val head = 5.dp.toPx()
    drawPath(
        Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(tip.x + dir * head, tip.y - head * 0.6f)
            lineTo(tip.x + dir * head, tip.y + head * 0.6f)
            close()
        },
        color,
    )
}
