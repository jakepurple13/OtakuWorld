package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.utils.composables.modifiers.staggerIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.tabs_history
import otakuworld.kmpuiviews.generated.resources.tabs_history_back
import otakuworld.kmpuiviews.generated.resources.tabs_history_forward
import otakuworld.kmpuiviews.generated.resources.tabs_history_now
import otakuworld.kmpuiviews.generated.resources.tabs_history_pages

/** How long the back or forward button must be held before the history menu opens. */
const val HISTORY_HOLD_MS = 450L

private const val ForwardAlpha = 0.72f
private val RowHeight = 36.dp
private val LabelHeight = 24.dp
private val RailWidth = 20.dp
private val MenuWidth = 300.dp
private val MenuMaxHeight = 340.dp

enum class HistorySection { Forward, Now, Back }

/** One page of a tab's history. [delta] is how many steps it is from the current page: forward is positive. */
data class HistoryRow(val index: Int, val key: NavKey, val delta: Int, val section: HistorySection)

/** Every page of the history, newest first. */
fun historyRows(entries: List<NavKey>, index: Int): List<HistoryRow> =
    entries.indices.reversed().map { i ->
        val section = when {
            i > index -> HistorySection.Forward
            i == index -> HistorySection.Now
            else -> HistorySection.Back
        }
        HistoryRow(index = i, key = entries[i], delta = i - index, section = section)
    }

/**
 * Where the pointer is during a hold-and-drag from a [HistoryButton] into the open menu, and where each menu row
 * is, both in screen coordinates. The press stays with the button, so the rows never see the pointer themselves.
 */
class HistoryDrag {
    var pointer: Offset? by mutableStateOf(null)
    val rowBounds = mutableStateMapOf<Int, Rect>()

    val hotIndex: Int?
        get() = pointer?.let { p -> rowBounds.entries.firstOrNull { it.value.contains(p) }?.key }
}

/**
 * Back or forward button. A click steps; holding it for [HISTORY_HOLD_MS], while a ring fills around it, or a
 * right-click calls [onOpenMenu]. After the hold opens the menu the press continues: [onHoldMove] gets the pointer's
 * screen position, and [onHoldRelease] where it was let go.
 */
@Composable
fun HistoryButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
    onHoldMove: (Offset) -> Unit = {},
    onHoldRelease: (Offset) -> Unit = {},
) {
    val currentClick by rememberUpdatedState(onClick)
    val currentOpen by rememberUpdatedState(onOpenMenu)
    val currentMove by rememberUpdatedState(onHoldMove)
    val currentRelease by rememberUpdatedState(onHoldRelease)
    val scope = rememberCoroutineScope()
    val ring = remember { Animatable(0f) }
    val interactions = remember { MutableInteractionSource() }
    val coordinates = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val ringColor = MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(36.dp)
            .onGloballyPositioned { coordinates[0] = it }
            .drawWithContent {
                drawContent()
                val progress = ring.value
                if (progress > 0f) {
                    val stroke = 2.dp.toPx()
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = size.copy(width = size.width - stroke, height = size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            .semantics {
                role = Role.Button
                contentDescription = description
                if (enabled) onClick { currentClick(); true } else disabled()
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Press) continue
                        val down = event.changes.first()
                        // The pressed buttons, not event.button: injected test input leaves that null
                        when {
                            event.buttons.isSecondaryPressed -> {
                                down.consume()
                                currentOpen()
                                continue
                            }
                            event.buttons.isPrimaryPressed -> down.consume()
                            else -> continue
                        }

                        val press = PressInteraction.Press(down.position)
                        interactions.tryEmit(press)
                        val fill = scope.launch {
                            ring.snapTo(0f)
                            ring.animateTo(1f, tween(HISTORY_HOLD_MS.toInt(), easing = LinearEasing))
                        }

                        var ended = false
                        var up: PointerInputChange? = null
                        withTimeoutOrNull(HISTORY_HOLD_MS) {
                            up = waitForUpOrCancellation()
                            ended = true
                        }
                        if (ended) {
                            fill.cancel()
                            scope.launch { ring.snapTo(0f) }
                            val released = up
                            if (released != null) {
                                released.consume()
                                interactions.tryEmit(PressInteraction.Release(press))
                                currentClick()
                            } else {
                                interactions.tryEmit(PressInteraction.Cancel(press))
                            }
                            continue
                        }

                        // Held: open the menu and keep following the press so it can pick a row
                        currentOpen()
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            val screen = coordinates[0]?.localToScreen(change.position) ?: change.position
                            if (change.pressed) {
                                currentMove(screen)
                            } else {
                                currentRelease(screen)
                                break
                            }
                        }
                        fill.cancel()
                        scope.launch { ring.snapTo(0f) }
                        interactions.tryEmit(PressInteraction.Cancel(press))
                    }
                }
            }
            // After the pointer input, so the press hits the whole square and not only the circle
            .clip(CircleShape)
            .indication(interactions, ripple()),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f),
        )
    }
}

private sealed interface HistoryItem {
    data class Label(val section: HistorySection) : HistoryItem
    data class Page(val row: HistoryRow) : HistoryItem
}

/**
 * The tab's whole history as a timeline, newest first, under the button that opened it. The current page is not
 * clickable; any other row calls [onJump] with its history index. [hotIndex] is the row under a held press.
 */
@Composable
fun TabHistoryMenu(
    tab: TabState,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit,
    hotIndex: Int?,
    keys: TabKeys = koinInject(),
    reducedMotion: Boolean = false,
    onRowBounds: (index: Int, bounds: Rect?) -> Unit = { _, _ -> },
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        val history = tab.nav.history
        val rows = historyRows(history.entries, history.index)
        val items = buildList {
            rows.groupBy { it.section }.let { bySection ->
                bySection[HistorySection.Forward]?.let { add(HistoryItem.Label(HistorySection.Forward)); it.forEach { r -> add(HistoryItem.Page(r)) } }
                bySection[HistorySection.Now]?.forEach { add(HistoryItem.Page(it)) }
                bySection[HistorySection.Back]?.let { add(HistoryItem.Label(HistorySection.Back)); it.forEach { r -> add(HistoryItem.Page(r)) } }
            }
        }
        // A fixed height: the menu measures its content's intrinsic size, which a lazy list can't answer
        fun heightOf(item: HistoryItem) = if (item is HistoryItem.Label) LabelHeight else RowHeight
        val menuHeight = items.fold(0.dp) { sum, item -> sum + heightOf(item) }.coerceAtMost(MenuMaxHeight)
        val nowPosition = items.indexOfFirst { it is HistoryItem.Page && it.row.section == HistorySection.Now }
        var selected by remember { mutableIntStateOf(history.index) }
        val played = remember { mutableStateOf(reducedMotion) }
        val focus = remember { FocusRequester() }

        // Opens scrolled so the current page sits in the middle; the list clamps it at either end
        val density = LocalDensity.current
        val listState = remember {
            var skip = items.take(nowPosition).fold(0.dp) { sum, item -> sum + heightOf(item) } - (menuHeight - RowHeight) / 2
            var first = 0
            while (first < items.lastIndex && skip >= heightOf(items[first])) {
                skip -= heightOf(items[first])
                first++
            }
            LazyListState(first, with(density) { skip.coerceAtLeast(0.dp).roundToPx() })
        }

        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

        // Keeps the keyboard selection in view
        LaunchedEffect(selected) {
            val position = items.indexOfFirst { it is HistoryItem.Page && it.row.index == selected }
            val visible = listState.layoutInfo.visibleItemsInfo
            if (position >= 0 && visible.isNotEmpty() && (position <= visible.first().index || position >= visible.last().index)) {
                listState.animateScrollToItem((position - 1).coerceAtLeast(0))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(MenuWidth).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                stringResource(Res.string.tabs_history),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(Res.string.tabs_history_pages, history.entries.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .width(MenuWidth)
                .height(menuHeight)
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        // Newest first, so up is a later page
                        Key.DirectionUp -> { selected = (selected + 1).coerceAtMost(history.entries.lastIndex); true }
                        Key.DirectionDown -> { selected = (selected - 1).coerceAtLeast(0); true }
                        Key.Enter, Key.NumPadEnter -> { if (selected != history.index) onJump(selected); true }
                        Key.Escape -> { onDismiss(); true }
                        else -> false
                    }
                },
        ) {
            itemsIndexed(
                items,
                key = { _, item ->
                    when (item) {
                        is HistoryItem.Label -> "label-${item.section}"
                        is HistoryItem.Page -> item.row.index
                    }
                },
            ) { position, item ->
                when (item) {
                    is HistoryItem.Label -> SectionLabel(
                        section = item.section,
                        showRail = position > 0,
                        modifier = Modifier.animateItem().staggerIn(position, played),
                    )
                    is HistoryItem.Page -> HistoryRowItem(
                        row = item.row,
                        title = keys.labelOf(item.row.key).text,
                        highlighted = (hotIndex ?: selected) == item.row.index && item.row.section != HistorySection.Now,
                        first = position == 0,
                        last = position == items.lastIndex,
                        onClick = { onJump(item.row.index) },
                        onBounds = { onRowBounds(item.row.index, it) },
                        modifier = Modifier.animateItem().staggerIn(position, played),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(section: HistorySection, showRail: Boolean, modifier: Modifier = Modifier) {
    val railColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(LabelHeight)
            .padding(horizontal = 12.dp),
    ) {
        Box(
            Modifier
                .width(RailWidth)
                .fillMaxHeight()
                .drawBehind {
                    if (showRail) {
                        drawLine(railColor, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 2.dp.toPx())
                    }
                }
        )
        Text(
            stringResource(if (section == HistorySection.Forward) Res.string.tabs_history_forward else Res.string.tabs_history_back),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun HistoryRowItem(
    row: HistoryRow,
    title: String,
    highlighted: Boolean,
    first: Boolean,
    last: Boolean,
    onClick: () -> Unit,
    onBounds: (Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNow = row.section == HistorySection.Now
    val railColor = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val currentBounds by rememberUpdatedState(onBounds)
    DisposableEffect(Unit) { onDispose { currentBounds(null) } }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (highlighted) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .then(if (isNow) Modifier else Modifier.clickable(onClick = onClick))
            .onGloballyPositioned { currentBounds(Rect(it.localToScreen(Offset.Zero), it.size.toSize())) }
            .padding(horizontal = 8.dp)
            .alpha(if (row.section == HistorySection.Forward) ForwardAlpha else 1f),
    ) {
        Box(
            Modifier
                .width(RailWidth)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    val cy = size.height / 2
                    val line = 2.dp.toPx()
                    drawLine(railColor, Offset(x, if (first) cy else 0f), Offset(x, if (last) cy else size.height), line)
                    val center = Offset(x, cy)
                    when (row.section) {
                        HistorySection.Now -> drawCircle(accent, radius = 5.dp.toPx(), center = center)
                        HistorySection.Forward -> drawCircle(
                            outline,
                            radius = 4.dp.toPx(),
                            center = center,
                            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))),
                        )
                        HistorySection.Back ->
                            drawCircle(outline, radius = 4.dp.toPx(), center = center, style = Stroke(1.5.dp.toPx()))
                    }
                }
        )
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isNow) FontWeight.SemiBold else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            when {
                row.delta > 0 -> "+${row.delta}"
                row.delta == 0 -> stringResource(Res.string.tabs_history_now)
                else -> "−${-row.delta}"
            },
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = if (isNow) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
