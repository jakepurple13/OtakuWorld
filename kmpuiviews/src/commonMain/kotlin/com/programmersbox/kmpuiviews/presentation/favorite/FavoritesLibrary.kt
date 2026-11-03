package com.programmersbox.kmpuiviews.presentation.favorite

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.repository.MatchField
import com.programmersbox.kmpuiviews.utils.adaptiveGridCell
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.fav_added_ago
import otakuworld.kmpuiviews.generated.resources.fav_best_match
import otakuworld.kmpuiviews.generated.resources.fav_chapters_short
import otakuworld.kmpuiviews.generated.resources.fav_grid_view
import otakuworld.kmpuiviews.generated.resources.fav_header_100
import otakuworld.kmpuiviews.generated.resources.fav_header_500
import otakuworld.kmpuiviews.generated.resources.fav_header_description_matches
import otakuworld.kmpuiviews.generated.resources.fav_header_earlier
import otakuworld.kmpuiviews.generated.resources.fav_header_note_matches
import otakuworld.kmpuiviews.generated.resources.fav_header_this_month
import otakuworld.kmpuiviews.generated.resources.fav_header_this_week
import otakuworld.kmpuiviews.generated.resources.fav_header_title_matches
import otakuworld.kmpuiviews.generated.resources.fav_header_under_100
import otakuworld.kmpuiviews.generated.resources.fav_in
import otakuworld.kmpuiviews.generated.resources.fav_in_description
import otakuworld.kmpuiviews.generated.resources.fav_in_note
import otakuworld.kmpuiviews.generated.resources.fav_in_title
import otakuworld.kmpuiviews.generated.resources.fav_list_view
import otakuworld.kmpuiviews.generated.resources.fav_no_filter_match
import otakuworld.kmpuiviews.generated.resources.fav_no_match_body
import otakuworld.kmpuiviews.generated.resources.fav_no_match_title
import otakuworld.kmpuiviews.generated.resources.fav_quick_multi
import otakuworld.kmpuiviews.generated.resources.fav_quick_off
import otakuworld.kmpuiviews.generated.resources.fav_quick_recent
import otakuworld.kmpuiviews.generated.resources.fav_remove_filter
import otakuworld.kmpuiviews.generated.resources.fav_result_line
import otakuworld.kmpuiviews.generated.resources.fav_results_for
import otakuworld.kmpuiviews.generated.resources.fav_snippet_description
import otakuworld.kmpuiviews.generated.resources.fav_snippet_note
import otakuworld.kmpuiviews.generated.resources.fav_sort_added
import otakuworld.kmpuiviews.generated.resources.fav_sort_az
import otakuworld.kmpuiviews.generated.resources.fav_sort_chapters
import otakuworld.kmpuiviews.generated.resources.fav_sources_n
import otakuworld.kmpuiviews.generated.resources.fav_updates_off_tag
import kotlin.time.Clock

@Composable
internal fun quickFilterLabel(filter: QuickFilter): String = stringResource(
    when (filter) {
        QuickFilter.RECENT -> Res.string.fav_quick_recent
        QuickFilter.MULTI_SOURCE -> Res.string.fav_quick_multi
        QuickFilter.UPDATES_OFF -> Res.string.fav_quick_off
    }
)

@Composable
private fun sectionLabel(header: SectionHeader): String = when (header) {
    is SectionHeader.Letter -> header.char.toString()
    SectionHeader.Added.THIS_WEEK -> stringResource(Res.string.fav_header_this_week)
    SectionHeader.Added.THIS_MONTH -> stringResource(Res.string.fav_header_this_month)
    SectionHeader.Added.EARLIER -> stringResource(Res.string.fav_header_earlier)
    SectionHeader.Chapters.OVER_500 -> stringResource(Res.string.fav_header_500)
    SectionHeader.Chapters.FROM_100 -> stringResource(Res.string.fav_header_100)
    SectionHeader.Chapters.UNDER_100 -> stringResource(Res.string.fav_header_under_100)
    is SectionHeader.Match -> stringResource(
        when (header.field) {
            MatchField.TITLE -> Res.string.fav_header_title_matches
            MatchField.DESCRIPTION -> Res.string.fav_header_description_matches
            MatchField.NOTE -> Res.string.fav_header_note_matches
        }
    )
}

private fun SectionHeader.key(): String = "h-" + when (this) {
    is SectionHeader.Letter -> char.toString()
    is SectionHeader.Match -> field.name
    is Enum<*> -> name
    else -> toString()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FavoritesLibrary(
    sections: List<FavoriteSection>,
    isSearching: Boolean,
    query: String,
    resultCount: Int,
    totalCount: Int,
    gridView: Boolean,
    onGridViewChange: (Boolean) -> Unit,
    sort: FavoriteSort,
    onSortChange: (FavoriteSort) -> Unit,
    quickFilters: Set<QuickFilter>,
    selectedSources: Set<String>,
    displayName: (String) -> String,
    onRemoveQuick: (QuickFilter) -> Unit,
    onClearSources: () -> Unit,
    onOpenFilters: () -> Unit,
    contentPadding: PaddingValues,
    onGroupClick: (FavoriteGroup) -> Unit,
    onGroupLongPress: (FavoriteGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        ActiveFilterChips(
            quickFilters = quickFilters,
            selectedSources = selectedSources,
            displayName = displayName,
            onRemoveQuick = onRemoveQuick,
            onClearSources = onClearSources,
            onOpenFilters = onOpenFilters,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = !gridView,
                    onClick = { onGridViewChange(false) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = {},
                ) { Icon(Icons.AutoMirrored.Filled.ViewList, stringResource(Res.string.fav_list_view)) }
                SegmentedButton(
                    selected = gridView,
                    onClick = { onGridViewChange(true) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = {},
                ) { Icon(Icons.Default.GridView, stringResource(Res.string.fav_grid_view)) }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                if (isSearching) {
                    Text(
                        stringResource(Res.string.fav_best_match),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    SingleChoiceSegmentedButtonRow {
                        val sorts = listOf(
                            FavoriteSort.TITLE to Res.string.fav_sort_az,
                            FavoriteSort.CHAPTERS to Res.string.fav_sort_chapters,
                            FavoriteSort.ADDED to Res.string.fav_sort_added,
                        )
                        sorts.forEachIndexed { index, (option, label) ->
                            SegmentedButton(
                                selected = sort == option,
                                onClick = { onSortChange(option) },
                                shape = SegmentedButtonDefaults.itemShape(index, sorts.size),
                                icon = {},
                            ) { Text(stringResource(label), maxLines = 1) }
                        }
                    }
                }
            }
        }

        Text(
            if (isSearching) pluralStringResource(Res.plurals.fav_results_for, resultCount, resultCount, query)
            else stringResource(Res.string.fav_result_line, resultCount, totalCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        val listPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp)

        if (sections.isEmpty()) {
            EmptyLibrary(isSearching, query, Modifier.padding(listPadding))
        } else {
            val fadeSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            AnimatedContent(
                targetState = gridView,
                transitionSpec = { fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec) },
                label = "favoritesView",
            ) { grid ->
                CompositionLocalProvider(LocalFavoritesViewScope provides this) {
                    if (grid) {
                        LibraryGrid(sections, isSearching, sort, displayName, listPadding, onGroupClick, onGroupLongPress)
                    } else {
                        LibraryList(
                            sections = sections,
                            isSearching = isSearching,
                            sort = sort,
                            displayName = displayName,
                            contentPadding = listPadding,
                            onGroupClick = onGroupClick,
                            onGroupLongPress = onGroupLongPress,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFilterChips(
    quickFilters: Set<QuickFilter>,
    selectedSources: Set<String>,
    displayName: (String) -> String,
    onRemoveQuick: (QuickFilter) -> Unit,
    onClearSources: () -> Unit,
    onOpenFilters: () -> Unit,
) {
    if (quickFilters.isEmpty() && selectedSources.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(quickFilters.toList(), key = { it.name }) { filter ->
            val label = quickFilterLabel(filter)
            InputChip(
                selected = true,
                onClick = { onRemoveQuick(filter) },
                label = { Text(label) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        stringResource(Res.string.fav_remove_filter, label),
                        Modifier.size(InputChipDefaults.IconSize),
                    )
                },
                modifier = Modifier.animateItem(),
            )
        }
        if (selectedSources.isNotEmpty()) {
            item(key = "sources") {
                val label = selectedSources.singleOrNull()?.let(displayName)
                    ?: pluralStringResource(Res.plurals.fav_sources_n, selectedSources.size, selectedSources.size)
                InputChip(
                    selected = true,
                    onClick = onOpenFilters,
                    label = { Text(label) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            stringResource(Res.string.fav_remove_filter, label),
                            Modifier
                                .size(InputChipDefaults.IconSize)
                                .pointerInput(Unit) { detectTapGestures { onClearSources() } },
                        )
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun EmptyLibrary(isSearching: Boolean, query: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
    ) {
        if (isSearching) {
            Text(
                stringResource(Res.string.fav_no_match_title, query),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.fav_no_match_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Text(
                stringResource(Res.string.fav_no_filter_match),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SectionHeaderRow(header: SectionHeader, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    ) {
        Text(
            sectionLabel(header),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun LibraryList(
    sections: List<FavoriteSection>,
    isSearching: Boolean,
    sort: FavoriteSort,
    displayName: (String) -> String,
    contentPadding: PaddingValues,
    onGroupClick: (FavoriteGroup) -> Unit,
    onGroupLongPress: (FavoriteGroup) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val now = Clock.System.now().toEpochMilliseconds()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            sections.forEach { section ->
                stickyHeader(key = section.header.key()) {
                    SectionHeaderRow(section.header, section.groups.size, Modifier.animateItem())
                }
                items(section.groups, key = { it.title }) { group ->
                    LibraryRow(
                        group = group,
                        isSearching = isSearching,
                        showAdded = sort == FavoriteSort.ADDED && !isSearching,
                        now = now,
                        displayName = displayName,
                        modifier = Modifier
                            .animateItem()
                            .combinedClickable(
                                onClick = { onGroupClick(group) },
                                onLongClick = { onGroupLongPress(group) },
                            ),
                    )
                }
            }
        }

        val showRail = !isSearching && sort == FavoriteSort.TITLE && sections.size > 3
        if (showRail) {
            val headerIndexes = remember(sections) {
                sections.runningFold(0) { index, section -> index + 1 + section.groups.size }.dropLast(1)
            }
            LetterRail(
                letters = sections.map { (it.header as? SectionHeader.Letter)?.char ?: '#' },
                onLetter = { scope.launch { listState.animateScrollToItem(headerIndexes[it]) } },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryRow(
    group: FavoriteGroup,
    isSearching: Boolean,
    showAdded: Boolean,
    now: Long,
    displayName: (String) -> String,
    modifier: Modifier = Modifier,
) {
    val hit = group.hit
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 28.dp, top = 6.dp, bottom = 6.dp),
    ) {
        FavoriteCover(group = group, shape = RoundedCornerShape(6.dp), modifier = Modifier.size(48.dp, 72.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            val title = hit?.titleHighlight?.takeIf { isSearching }
            if (title != null) {
                Text(highlightedText(title), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(group.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                group.sources.forEach { Tag(displayName(it)) }
                if (group.updatesOff) Tag(stringResource(Res.string.fav_updates_off_tag))
            }
            if (showAdded && group.latestCreatedAt > 0) {
                Text(
                    stringResource(Res.string.fav_added_ago, shortAge(group.latestCreatedAt, now)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (isSearching && hit?.snippet != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(
                            if (hit.snippetField == MatchField.NOTE) Res.string.fav_snippet_note
                            else Res.string.fav_snippet_description
                        ).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        highlightedText(hit.snippet),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(group.maxChapters.toString(), style = MaterialTheme.typography.titleMedium)
            Text("ch", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Tag(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryGrid(
    sections: List<FavoriteSection>,
    isSearching: Boolean,
    sort: FavoriteSort,
    displayName: (String) -> String,
    contentPadding: PaddingValues,
    onGroupClick: (FavoriteGroup) -> Unit,
    onGroupLongPress: (FavoriteGroup) -> Unit,
) {
    val now = Clock.System.now().toEpochMilliseconds()
    LazyVerticalGrid(
        columns = adaptiveGridCell(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            bottom = contentPadding.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        sections.forEach { section ->
            item(key = section.header.key(), span = { GridItemSpan(maxLineSpan) }) {
                SectionHeaderRow(section.header, section.groups.size, Modifier.animateItem())
            }
            items(section.groups, key = { it.title }) { group ->
                GridTile(
                    group = group,
                    isSearching = isSearching,
                    showAdded = sort == FavoriteSort.ADDED && !isSearching,
                    now = now,
                    displayName = displayName,
                    modifier = Modifier
                        .animateItem()
                        .combinedClickable(
                            onClick = { onGroupClick(group) },
                            onLongClick = { onGroupLongPress(group) },
                        ),
                )
            }
        }
    }
}

@Composable
private fun GridTile(
    group: FavoriteGroup,
    isSearching: Boolean,
    showAdded: Boolean,
    now: Long,
    displayName: (String) -> String,
    modifier: Modifier = Modifier,
) {
    val hit = group.hit
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FavoriteCover(
            group = group,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
        ) {
            when {
                isSearching && hit != null && MatchField.NOTE in hit.matchedFields -> Icon(
                    Icons.AutoMirrored.Filled.Notes,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(3.dp)
                        .size(14.dp),
                )

                group.items.size > 1 -> SourceCountBadge(group.items.size, Modifier.align(Alignment.TopStart))
            }
            if (group.updatesOff) {
                Icon(
                    Icons.Default.NotificationsOff,
                    null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .padding(4.dp)
                        .size(14.dp),
                )
            }
        }
        val title = hit?.titleHighlight?.takeIf { isSearching }
        if (title != null) {
            Text(highlightedText(title), style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else {
            Text(group.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(
            text = when {
                isSearching && hit != null -> stringResource(
                    Res.string.fav_in,
                    listOfNotNull(
                        stringResource(Res.string.fav_in_title).takeIf { MatchField.TITLE in hit.matchedFields },
                        stringResource(Res.string.fav_in_description).takeIf { MatchField.DESCRIPTION in hit.matchedFields },
                        stringResource(Res.string.fav_in_note).takeIf { MatchField.NOTE in hit.matchedFields },
                    ).joinToString(" · ")
                )

                showAdded && group.latestCreatedAt > 0 ->
                    stringResource(Res.string.fav_added_ago, shortAge(group.latestCreatedAt, now))

                else -> stringResource(Res.string.fav_chapters_short, group.maxChapters) +
                    " · " + displayName(group.sources.first()) + if (group.sources.size > 1) " +${group.sources.size - 1}" else ""
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LetterRail(
    letters: List<Char>,
    onLetter: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var height by remember { mutableIntStateOf(1) }
    var last by remember { mutableIntStateOf(-1) }
    fun pick(y: Float) {
        val index = ((y / height) * letters.size).toInt().coerceIn(0, letters.lastIndex)
        if (index != last) {
            last = index
            onLetter(index)
        }
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .onSizeChanged { height = it.height.coerceAtLeast(1) }
                .pointerInput(letters) {
                    detectTapGestures {
                        last = -1
                        pick(it.y)
                    }
                }
                .pointerInput(letters) {
                    detectVerticalDragGestures(onDragEnd = { last = -1 }) { change, _ -> pick(change.position.y) }
                }
                .padding(horizontal = 4.dp, vertical = 6.dp),
        ) {
            letters.forEach {
                Text(it.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
