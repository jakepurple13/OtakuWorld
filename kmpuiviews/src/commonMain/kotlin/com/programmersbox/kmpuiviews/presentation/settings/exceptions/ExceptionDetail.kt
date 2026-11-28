package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.exc_at_time
import otakuworld.kmpuiviews.generated.resources.exc_copy
import otakuworld.kmpuiviews.generated.resources.exc_delete
import otakuworld.kmpuiviews.generated.resources.exc_fact_error
import otakuworld.kmpuiviews.generated.resources.exc_fact_extension
import otakuworld.kmpuiviews.generated.resources.exc_fact_seen
import otakuworld.kmpuiviews.generated.resources.exc_fact_source
import otakuworld.kmpuiviews.generated.resources.exc_no_selection
import otakuworld.kmpuiviews.generated.resources.exc_save
import otakuworld.kmpuiviews.generated.resources.exc_seen_week
import otakuworld.kmpuiviews.generated.resources.exc_share
import otakuworld.kmpuiviews.generated.resources.exc_source_menu
import otakuworld.kmpuiviews.generated.resources.exc_stack_trace
import otakuworld.kmpuiviews.generated.resources.exc_today
import otakuworld.kmpuiviews.generated.resources.exc_yesterday
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The detail pane. Single-pane: back + delete on top, Copy · Share/Save · Source at the bottom.
 * Two-pane: everything in one row at the top. The header (badge, title, time) is shared with the row.
 */
@Composable
fun ExceptionDetail(
    row: ExceptionRow,
    explanation: AiTextState,
    explainAvailable: Boolean,
    twoPane: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onShareOrSave: () -> Unit,
    onExplain: () -> Unit,
    onSourceAction: (SourceAction) -> Unit,
    sharedKeys: SharedKeys?,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    val actions = remember(row) { availableActions(row) }
    Surface(
        modifier = modifier
            .fillMaxSize()
            .sharedIf(sharedKeys, "exception-${row.id}", bounds = true),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                if (!twoPane) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
                Spacer(Modifier.weight(1f))
                if (twoPane) ActionButtons(row, actions, onCopy, onShareOrSave, onSourceAction)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.exc_delete))
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (twoPane) 24.dp else 16.dp, vertical = 8.dp),
            ) {
                Header(row, sharedKeys)
                Column(contentModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Facts(row, columns = if (twoPane) 4 else 2)
                    ExplanationBlock(row, explanation, explainAvailable, autoStart = true, onExplain = onExplain)
                    Text(
                        stringResource(Res.string.exc_stack_trace),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    StackTraceBlock(row.parsed)
                }
            }
            if (!twoPane) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    ActionButtons(row, actions, onCopy, onShareOrSave, onSourceAction, fill = true)
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ActionButtons(
    row: ExceptionRow,
    actions: List<SourceAction>,
    onCopy: () -> Unit,
    onShareOrSave: () -> Unit,
    onSourceAction: (SourceAction) -> Unit,
    fill: Boolean = false,
) {
    val weight = if (fill) Modifier.weight(1f) else Modifier
    FilledTonalButton(onClick = onCopy, modifier = weight) {
        Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(Res.string.exc_copy), maxLines = 1)
    }
    FilledTonalButton(onClick = onShareOrSave, modifier = weight) {
        Icon(if (hasShareSheet) Icons.Default.Share else Icons.Default.SaveAlt, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(if (hasShareSheet) Res.string.exc_share else Res.string.exc_save), maxLines = 1)
    }
    if (actions.isNotEmpty()) {
        var open by remember { mutableStateOf(false) }
        Box(weight) {
            FilledTonalButton(onClick = { open = true }, modifier = if (fill) Modifier.fillMaxWidth() else Modifier) {
                Text(stringResource(Res.string.exc_source_menu), maxLines = 1)
                Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp))
            }
            SourceActionsMenu(row, expanded = open, onDismiss = { open = false }, onAction = onSourceAction)
        }
    }
}

@Composable
private fun Header(row: ExceptionRow, sharedKeys: SharedKeys?) {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val today = remember { Clock.System.todayIn(timeZone) }
    val date = Instant.fromEpochMilliseconds(row.time).toLocalDateTime(timeZone).date
    val day = when (date) {
        today -> stringResource(Res.string.exc_today)
        today.minus(DatePeriod(days = 1)) -> stringResource(Res.string.exc_yesterday)
        else -> "${date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${date.day}"
    }
    val time = timeLabel(row.time, timeZone)
    val atTime = stringResource(Res.string.exc_at_time, day, "\u0000")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        KindBadge(row.parsed.kind, Modifier.sharedIf(sharedKeys, "exception-${row.id}-badge"))
        Text(
            headlineText(row),
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.sharedIf(sharedKeys, "exception-${row.id}-title"),
        )
        // "Today at 4:52 PM" with only the time part shared, so it lands where the row's time was.
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (before, after) = atTime.split("\u0000").let { it[0] to it.getOrElse(1) { "" } }
            val style = MaterialTheme.typography.bodySmall
            val color = MaterialTheme.colorScheme.onSurfaceVariant
            if (before.isNotEmpty()) Text(before, style = style, color = color)
            Text(time, style = style, color = color, modifier = Modifier.sharedIf(sharedKeys, "exception-${row.id}-time"))
            if (after.isNotEmpty()) Text(after, style = style, color = color)
        }
    }
}

@Composable
private fun Facts(row: ExceptionRow, columns: Int) {
    val facts: List<Triple<StringResource, String, Boolean>> = buildList {
        row.sourceName?.let { add(Triple(Res.string.exc_fact_source, row.source?.name ?: it, false)) }
        row.parsed.extension?.let { add(Triple(Res.string.exc_fact_extension, it.id, true)) }
        add(Triple(Res.string.exc_fact_error, row.parsed.shortType, false))
    }
    val seen = stringResource(Res.string.exc_seen_week, row.seenThisWeek)
    val all = facts.map { (label, value, mono) -> Triple(stringResource(label), value, mono) } +
        Triple(stringResource(Res.string.exc_fact_seen), seen, false)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        all.chunked(columns).forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                line.forEach { (label, value, mono) ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                value,
                                style = MaterialTheme.typography.bodyMedium.let { if (mono) it.copy(fontFamily = FontFamily.Monospace) else it },
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                repeat(columns - line.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Two-pane detail with nothing selected. */
@Composable
fun EmptyDetail(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(Res.string.exc_no_selection),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
