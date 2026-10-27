package com.programmersbox.anime.shared.videos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.anime.shared.downloads.QueuedDownload
import com.programmersbox.anime.shared.downloads.QueuedDownloadState
import com.programmersbox.anime.shared.downloads.formatBytes
import com.programmersbox.anime.shared.downloads.splitEpisodeTitle
import com.programmersbox.anime.shared.downloads.statusLabel
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

private val ThumbnailShape = RoundedCornerShape(12.dp)

private val ShortDateFormat = LocalDate.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day(padding = Padding.NONE)
}

internal fun Long.toDurationText(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    return if (hours > 0) "${hours.toString().padStart(2, '0')}:$mm:$ss" else "$mm:$ss"
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 2.dp)
    )
}

@Composable
fun VideoRow(
    item: SharedVideoContent,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    onExport: (() -> Unit)? = null,
) {
    VideoRowContent(
        item = item,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
    ) {
        var showDropDown by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { showDropDown = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showDropDown,
                onDismissRequest = { showDropDown = false }
            ) {
                if (onExport != null) {
                    DropdownMenuItem(
                        onClick = {
                            showDropDown = false
                            onExport()
                        },
                        text = { Text("Export…") }
                    )
                }
                DropdownMenuItem(
                    onClick = {
                        showDropDown = false
                        onRemove()
                    },
                    text = { Text("Remove") }
                )
            }
        }
    }
}

/** Thumbnail and text lines of a library row, without click handling, so the delete sheet can reuse it. */
@Composable
internal fun VideoRowContent(
    item: SharedVideoContent,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
    ) {
        LibraryThumbnail(item)

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            val name = remember(item.videoName) { item.videoName.substringBeforeLast('.') }
            if (item.isDownload) {
                val title = remember(name) { splitEpisodeTitle(name) }
                Text(
                    title.show,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                title.episode?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val labels = item.subtitleLabels
                    if (labels.isNotEmpty()) {
                        CcTag()
                        val shown = remember(labels) {
                            labels.take(2).joinToString(", ") + if (labels.size > 2) " +${labels.size - 2}" else ""
                        }
                        Text(
                            shown,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    // Unweighted so the size stays visible however long the labels are.
                    if (item.sizeBytes > 0) {
                        Text(
                            (if (labels.isNotEmpty()) "· " else "") + formatBytes(item.sizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            } else {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = remember(item.dateAddedMs, item.sizeBytes) {
                    listOfNotNull(
                        item.dateAddedMs.takeIf { it > 0 }?.let { ShortDateFormat.format(it.toLocalDateTime().date) },
                        item.sizeBytes.takeIf { it > 0 }?.let(::formatBytes)
                    ).joinToString(" · ")
                }
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        trailing()
    }
}

@Composable
private fun LibraryThumbnail(item: SharedVideoContent) {
    Box(
        modifier = Modifier
            .size(width = 112.dp, height = 63.dp)
            .clip(ThumbnailShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        VideoThumbnail(
            path = item.path,
            modifier = Modifier.size(width = 112.dp, height = 63.dp)
        )

        val runTimeString = remember(item.duration) { item.duration.toDurationText() }
        Text(
            runTimeString,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 6.dp, bottom = 8.dp)
                .background(Color(0xB8000000), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        )

        if (item.lastPlayedPositionMs > 0 && item.duration > 0) {
            val fraction = (item.lastPlayedPositionMs.toFloat() / item.duration).coerceIn(0f, 1f)
            ProgressBorder(
                fraction = fraction,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/** Watch progress as a border that traces the thumbnail clockwise from its top-left corner. */
@Composable
private fun ProgressBorder(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val strokeWidth = 3.dp.toPx()
            val inset = strokeWidth / 2
            val radius = 12.dp.toPx() - inset
            val left = inset
            val top = inset
            val right = size.width - inset
            val bottom = size.height - inset
            // Built by hand so the path starts at the top-left and runs clockwise.
            val outline = Path().apply {
                moveTo(left + radius, top)
                lineTo(right - radius, top)
                arcTo(Rect(right - 2 * radius, top, right, top + 2 * radius), -90f, 90f, false)
                lineTo(right, bottom - radius)
                arcTo(Rect(right - 2 * radius, bottom - 2 * radius, right, bottom), 0f, 90f, false)
                lineTo(left + radius, bottom)
                arcTo(Rect(left, bottom - 2 * radius, left + 2 * radius, bottom), 90f, 90f, false)
                lineTo(left, top + radius)
                arcTo(Rect(left, top, left + 2 * radius, top + 2 * radius), 180f, 90f, false)
                close()
            }
            val measure = PathMeasure().apply { setPath(outline, false) }
            val progress = Path().apply { measure.getSegment(0f, measure.length * fraction, this, true) }
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            onDrawBehind {
                drawPath(outline, Color.White.copy(alpha = 0.14f), style = Stroke(width = strokeWidth))
                drawPath(progress, color, style = stroke)
            }
        }
    )
}

@Composable
private fun CcTag() {
    Text(
        "CC",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp)
    )
}

@Composable
fun QueuedDownloadRow(
    item: QueuedDownload,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 112.dp, height = 63.dp)
                .clip(ThumbnailShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            val title = remember(item.title) { splitEpisodeTitle(item.title) }
            Text(
                title.show,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            title.episode?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (item.state == QueuedDownloadState.Running) {
                val progress = item.progress
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            Text(
                item.statusLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = if (item.state == QueuedDownloadState.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (item.state == QueuedDownloadState.Failed) {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onRetry) { Text("Retry") }
                TextButton(onClick = onRemove) { Text("Remove") }
            }
        } else {
            IconButton(onClick = onCancel) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cancel download",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
