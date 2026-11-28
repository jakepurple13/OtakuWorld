package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.presentation.components.ai.AiCard
import com.programmersbox.kmpuiviews.presentation.components.ai.Spark
import androidx.compose.material3.AssistChip
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.exc_copy
import otakuworld.kmpuiviews.generated.resources.exc_counts
import otakuworld.kmpuiviews.generated.resources.exc_day_count
import otakuworld.kmpuiviews.generated.resources.exc_delete
import otakuworld.kmpuiviews.generated.resources.exc_empty_body
import otakuworld.kmpuiviews.generated.resources.exc_empty_title
import otakuworld.kmpuiviews.generated.resources.exc_last_24h
import otakuworld.kmpuiviews.generated.resources.exc_more_info
import otakuworld.kmpuiviews.generated.resources.exc_summarize
import otakuworld.kmpuiviews.generated.resources.exc_summary
import otakuworld.kmpuiviews.generated.resources.exc_today
import otakuworld.kmpuiviews.generated.resources.exc_yesterday
import kotlin.time.Clock
import kotlin.time.Instant

/** The list pane: 24-hour digest card, then rows grouped by day on a rail. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExceptionTimeline(
    days: List<ExceptionDay>,
    hourly: List<Int>,
    digest: AiTextState,
    digestAvailable: Boolean,
    explainAvailable: Boolean,
    twoPane: Boolean,
    selected: Long?,
    onSelect: (Long) -> Unit,
    onMoreInfo: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onCopy: (ExceptionRow) -> Unit,
    onSummarize: () -> Unit,
    explanation: (ExceptionRow) -> StateFlow<AiTextState>,
    onExplain: (ExceptionRow) -> Unit,
    modifier: Modifier = Modifier,
    sharedKeys: SharedKeys? = null,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    /** Rows sliding out after Delete; [onExited] fires when each one is gone. */
    exiting: Set<Long> = emptySet(),
    onExited: (Long) -> Unit = {},
    onSave: (ExceptionRow) -> Unit = {},
    onOpenWebsite: ((ExceptionRow) -> Unit)? = null,
) {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val today = remember { Clock.System.todayIn(timeZone) }
    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize(),
    ) {
        if (days.isEmpty()) {
            item(key = "empty") { EmptyTimeline(Modifier.animateItem()) }
            return@LazyColumn
        }
        if (hourly.sum() > 0) {
            item(key = "digest") {
                // Rows are newest first, so the first hourly.sum() rows are the last 24 hours.
                val recent = days.flatMap { it.rows }.take(hourly.sum())
                DigestCard(
                    hourly = hourly,
                    sources = recent.mapNotNull { it.sourceName }.distinct().size,
                    digest = digest,
                    digestAvailable = digestAvailable,
                    onSummarize = onSummarize,
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        days.forEach { day ->
            stickyHeader(key = "day-${day.date}") {
                DayHeader(day, today, Modifier.animateItem())
            }
            items(day.rows, key = { it.id }) { row ->
                val state by explanation(row).collectAsStateWithLifecycle()
                val visible = remember(row.id) { MutableTransitionState(true) }
                // Only ever hide: once exited, the row may still be composed while the list catches up.
                if (row.id in exiting) visible.targetState = false
                LaunchedEffect(visible.isIdle, visible.currentState) {
                    if (visible.isIdle && !visible.currentState) onExited(row.id)
                }
                AnimatedVisibility(
                    visibleState = visible,
                    enter = fadeIn() + expandVertically(),
                    exit = slideOutHorizontally(tween(220)) { -it / 6 } + fadeOut(tween(180)) +
                        shrinkVertically(tween(260, delayMillis = 120)),
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(250),
                        fadeOutSpec = tween(200),
                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold),
                    ),
                ) {
                RowContextMenu(
                    onCopy = { onCopy(row) },
                    onSave = { onSave(row) },
                    onOpenWebsite = onOpenWebsite?.takeIf { row.source != null }?.let { open -> { open(row) } },
                    onDelete = { onDelete(row.id) },
                ) {
                TimelineRow(
                    row = row,
                    timeZone = timeZone,
                    twoPane = twoPane,
                    selected = row.id == selected,
                    explanation = state,
                    explainAvailable = explainAvailable,
                    onSelect = { onSelect(row.id) },
                    onMoreInfo = { onMoreInfo(row.id) },
                    onDelete = { onDelete(row.id) },
                    onCopy = { onCopy(row) },
                    onExplain = { onExplain(row) },
                    sharedKeys = sharedKeys,
                )
                }
                }
            }
        }
    }
}

@Composable
private fun DigestCard(
    hourly: List<Int>,
    sources: Int,
    digest: AiTextState,
    digestAvailable: Boolean,
    onSummarize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(stringResource(Res.string.exc_last_24h), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    stringResource(Res.string.exc_counts, hourly.sum(), sources),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HourBars(hourly, Modifier.fillMaxWidth().height(44.dp))
            if (digestAvailable) {
                if (digest == AiTextState.Idle) {
                    AssistChip(
                        onClick = onSummarize,
                        label = { Text(stringResource(Res.string.exc_summarize)) },
                        leadingIcon = { Spark(spinning = false) },
                    )
                } else {
                    AiCard(label = stringResource(Res.string.exc_summary), state = digest, onRetry = onSummarize)
                }
            }
        }
    }
}

private const val BAR_MS = 600
private const val BAR_STAGGER_MS = 18

/** 24 bars, oldest left; each grows in after the one before it. Empty hours are faint stubs. */
@Composable
private fun HourBars(hourly: List<Int>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val total = (BAR_MS + BAR_STAGGER_MS * hourly.size).toFloat()
        clock.animateTo(total, tween(total.toInt()))
    }
    val max = (hourly.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(modifier) {
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (hourly.size - 1)) / hourly.size
        hourly.forEachIndexed { i, count ->
            val t = ((clock.value - i * BAR_STAGGER_MS) / BAR_MS).coerceIn(0f, 1f)
            val target = if (count == 0) 0.12f else count.toFloat() / max
            val h = size.height * target * FastOutSlowInEasing.transform(t)
            drawRoundRect(
                color = color.copy(alpha = if (count == 0) 0.25f else 0.85f),
                topLeft = Offset(i * (w + gap), size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
        }
    }
}

@Composable
private fun DayHeader(day: ExceptionDay, today: LocalDate, modifier: Modifier = Modifier) {
    val label = when (day.date) {
        today -> stringResource(Res.string.exc_today)
        today.minus(DatePeriod(days = 1)) -> stringResource(Res.string.exc_yesterday)
        else -> "${day.date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${day.date.day}"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(
            stringResource(Res.string.exc_day_count, day.rows.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimelineRow(
    row: ExceptionRow,
    timeZone: TimeZone,
    twoPane: Boolean,
    selected: Boolean,
    explanation: AiTextState,
    explainAvailable: Boolean,
    onSelect: () -> Unit,
    onMoreInfo: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onExplain: () -> Unit,
    sharedKeys: SharedKeys?,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(row.id) { mutableStateOf(false) }
    val rail = MaterialTheme.colorScheme.outlineVariant
    val dot = kindColor(row.parsed.kind)
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp)
            .drawBehind {
                val x = 11.dp.toPx()
                drawLine(rail, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
                val r = 6.dp.toPx()
                val c = Offset(x, 20.dp.toPx())
                drawCircle(surface, radius = r, center = c)
                drawCircle(dot, radius = r - 1.5.dp.toPx(), center = c, style = Stroke(3.dp.toPx()))
            }
            .padding(start = 26.dp, top = 2.dp, bottom = 2.dp)
    ) {
        Surface(
            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
            // Surface's onClick clips the ripple to the shape.
            onClick = { if (twoPane) onSelect() else expanded = !expanded },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .sharedIf(sharedKeys, "exception-${row.id}", bounds = true),
        ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        headlineText(row),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .sharedIf(sharedKeys, "exception-${row.id}-title"),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        timeLabel(row.time, timeZone),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.sharedIf(sharedKeys, "exception-${row.id}-time"),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    KindBadge(row.parsed.kind, Modifier.sharedIf(sharedKeys, "exception-${row.id}-badge"))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        row.parsed.shortType,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AnimatedVisibility(
                    visible = expanded && !twoPane,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 10.dp),
                    ) {
                        ExplanationBlock(row, explanation, explainAvailable, autoStart = false, onExplain = onExplain)
                        StackTraceBlock(row.parsed)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TextButton(onClick = onCopy) {
                                Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(Res.string.exc_copy))
                            }
                            TextButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(Res.string.exc_delete))
                            }
                            FilledTonalButton(onClick = onMoreInfo) {
                                Icon(Icons.Outlined.Info, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(Res.string.exc_more_info))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTimeline(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 64.dp),
    ) {
        Text(stringResource(Res.string.exc_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.exc_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "4:52 PM". */
fun timeLabel(time: Long, timeZone: TimeZone): String {
    val t = Instant.fromEpochMilliseconds(time).toLocalDateTime(timeZone)
    val h = if (t.hour % 12 == 0) 12 else t.hour % 12
    return "$h:${t.minute.toString().padStart(2, '0')} ${if (t.hour < 12) "AM" else "PM"}"
}
