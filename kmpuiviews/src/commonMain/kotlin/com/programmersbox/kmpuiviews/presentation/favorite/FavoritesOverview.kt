package com.programmersbox.kmpuiviews.presentation.favorite

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.fav_ago
import otakuworld.kmpuiviews.generated.resources.fav_multi_source
import otakuworld.kmpuiviews.generated.resources.fav_recently_added
import otakuworld.kmpuiviews.generated.resources.fav_see_all
import otakuworld.kmpuiviews.generated.resources.fav_stat_chapters
import otakuworld.kmpuiviews.generated.resources.fav_stat_sources
import otakuworld.kmpuiviews.generated.resources.fav_stat_titles
import otakuworld.kmpuiviews.generated.resources.fav_titles_count
import otakuworld.kmpuiviews.generated.resources.fav_updates_off_row
import kotlin.time.Clock

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FavoritesOverview(
    overview: FavoritesOverview,
    contentPadding: PaddingValues,
    onSeeAll: (QuickFilter) -> Unit,
    onOpenFilters: () -> Unit,
    onGroupClick: (FavoriteGroup) -> Unit,
    onGroupLongPress: (FavoriteGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = Clock.System.now().toEpochMilliseconds()
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "stats") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .animateItem()
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
            ) {
                StatTile(overview.titleCount.toString(), stringResource(Res.string.fav_stat_titles), Modifier.weight(1f))
                StatTile(
                    value = overview.sourceCount.toString(),
                    label = stringResource(Res.string.fav_stat_sources) + " ›",
                    onClick = onOpenFilters,
                    modifier = Modifier.weight(1f),
                )
                StatTile(overview.chapterCount.toString(), stringResource(Res.string.fav_stat_chapters), Modifier.weight(1f))
            }
        }

        if (overview.recent.isNotEmpty()) {
            item(key = "recent-header") {
                ShelfHeader(
                    title = stringResource(Res.string.fav_recently_added),
                    onSeeAll = { onSeeAll(QuickFilter.RECENT) },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "recent-shelf") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.animateItem(),
                ) {
                    items(overview.recent, key = { it.title }) { group ->
                        FavoriteCover(
                            group = group,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .animateItem()
                                .size(118.dp, 177.dp)
                                .combinedClickable(
                                    onClick = { onGroupClick(group) },
                                    onLongClick = { onGroupLongPress(group) },
                                ),
                        ) {
                            CoverChip(
                                text = stringResource(Res.string.fav_ago, shortAge(group.latestCreatedAt, now)),
                                modifier = Modifier.align(Alignment.TopStart),
                            )
                            CoverTitle(group.title, Modifier.align(Alignment.BottomStart))
                        }
                    }
                }
            }
        }

        if (overview.multiSource.isNotEmpty()) {
            item(key = "multi-header") {
                ShelfHeader(
                    title = stringResource(Res.string.fav_multi_source),
                    onSeeAll = { onSeeAll(QuickFilter.MULTI_SOURCE) },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "multi-shelf") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.animateItem(),
                ) {
                    items(overview.multiSource, key = { it.title }) { group ->
                        FavoriteCover(
                            group = group,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .animateItem()
                                .size(84.dp, 126.dp)
                                .combinedClickable(
                                    onClick = { onGroupClick(group) },
                                    onLongClick = { onGroupLongPress(group) },
                                ),
                        ) {
                            SourceCountBadge(group.items.size, Modifier.align(Alignment.TopStart))
                        }
                    }
                }
            }
        }

        if (overview.updatesOff.isNotEmpty()) {
            item(key = "updates-off") {
                Surface(
                    onClick = { onSeeAll(QuickFilter.UPDATES_OFF) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Default.NotificationsOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(Res.string.fav_updates_off_row), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                pluralStringResource(Res.plurals.fav_titles_count, overview.updatesOff.size, overview.updatesOff.size) +
                                    " · " + overview.updatesOff.take(2).joinToString { it.title } +
                                    if (overview.updatesOff.size > 2) "…" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    val shape = RoundedCornerShape(14.dp)
    val color = MaterialTheme.colorScheme.surfaceContainer
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = color, modifier = modifier) { content() }
    } else {
        Surface(shape = shape, color = color, modifier = modifier) { content() }
    }
}

@Composable
private fun ShelfHeader(title: String, onSeeAll: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onSeeAll) { Text(stringResource(Res.string.fav_see_all)) }
    }
}

@Composable
internal fun CoverChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .padding(8.dp)
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
private fun CoverTitle(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
            )
            .padding(8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SourceCountBadge(count: Int, modifier: Modifier = Modifier) {
    Text(
        text = "×$count",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .padding(6.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
