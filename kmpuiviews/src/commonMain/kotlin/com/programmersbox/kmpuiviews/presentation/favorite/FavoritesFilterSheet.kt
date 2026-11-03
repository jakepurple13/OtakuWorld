package com.programmersbox.kmpuiviews.presentation.favorite

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.fav_all
import otakuworld.kmpuiviews.generated.resources.fav_filter
import otakuworld.kmpuiviews.generated.resources.fav_hide_empty
import otakuworld.kmpuiviews.generated.resources.fav_long_press_hint
import otakuworld.kmpuiviews.generated.resources.fav_n_selected
import otakuworld.kmpuiviews.generated.resources.fav_n_shown
import otakuworld.kmpuiviews.generated.resources.fav_no_sources_match
import otakuworld.kmpuiviews.generated.resources.fav_no_titles_match
import otakuworld.kmpuiviews.generated.resources.fav_none
import otakuworld.kmpuiviews.generated.resources.fav_reset
import otakuworld.kmpuiviews.generated.resources.fav_search_sources
import otakuworld.kmpuiviews.generated.resources.fav_show_only
import otakuworld.kmpuiviews.generated.resources.fav_show_titles
import otakuworld.kmpuiviews.generated.resources.fav_sources

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun FavoritesFilterSheet(
    quickFilters: Set<QuickFilter>,
    quickCounts: Map<QuickFilter, Int>,
    onToggleQuick: (QuickFilter) -> Unit,
    sourceCounts: List<Pair<String, Int>>,
    selectedSources: Set<String>,
    displayName: (String) -> String,
    onToggleSource: (String) -> Unit,
    onOnlySource: (String) -> Unit,
    onSelectAll: (List<String>) -> Unit,
    onSelectNone: (List<String>) -> Unit,
    activeFilterCount: Int,
    onReset: () -> Unit,
    resultCount: Int,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    var hideEmpty by rememberSaveable { mutableStateOf(true) }
    val sourceQuery = rememberTextFieldState()
    val query = sourceQuery.text.toString().trim()

    val hiddenCount = sourceCounts.count { it.second == 0 }
    val available = if (hideEmpty) sourceCounts.filter { it.second > 0 } else sourceCounts
    val visible = available.filter {
        query.isEmpty() || displayName(it.first).contains(query, ignoreCase = true)
    }
    val visibleNames = visible.map { it.first }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxHeight(0.9f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp),
            ) {
                Text(
                    stringResource(Res.string.fav_filter),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onReset, enabled = activeFilterCount > 0) {
                    Text(stringResource(Res.string.fav_reset))
                }
            }

            LazyColumn(Modifier.weight(1f)) {
                item(key = "show-only-label") { SheetLabel(stringResource(Res.string.fav_show_only)) }
                item(key = "show-only") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 20.dp),
                    ) {
                        QuickFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = filter in quickFilters,
                                onClick = { onToggleQuick(filter) },
                                label = { Text("${quickFilterLabel(filter)}  ${quickCounts[filter] ?: 0}") },
                            )
                        }
                    }
                }

                item(key = "sources-label") {
                    SheetLabel(
                        text = stringResource(Res.string.fav_sources),
                        trailing = if (selectedSources.isEmpty()) stringResource(Res.string.fav_all)
                        else stringResource(Res.string.fav_n_selected, selectedSources.size),
                    )
                }
                item(key = "sources-search") {
                    OutlinedTextField(
                        state = sourceQuery,
                        placeholder = { Text(stringResource(Res.string.fav_search_sources, available.size)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
                item(key = "hide-empty") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                    ) {
                        Text(
                            stringResource(Res.string.fav_hide_empty) + if (hideEmpty) " ($hiddenCount)" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = hideEmpty, onCheckedChange = { hideEmpty = it })
                    }
                }
                item(key = "shown") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 12.dp),
                    ) {
                        Text(
                            stringResource(Res.string.fav_n_shown, visible.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onSelectAll(visibleNames) }) { Text(stringResource(Res.string.fav_all)) }
                        TextButton(onClick = { onSelectNone(visibleNames) }) { Text(stringResource(Res.string.fav_none)) }
                    }
                }

                if (visible.isEmpty()) {
                    item(key = "no-sources") {
                        Text(
                            stringResource(Res.string.fav_no_sources_match, query),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                        )
                    }
                }

                items(visible, key = { "src-" + it.first }) { (source, count) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onToggleSource(source) },
                                onLongClick = { onOnlySource(source) },
                            )
                            .padding(horizontal = 8.dp)
                            .alpha(if (count == 0) 0.55f else 1f),
                    ) {
                        Checkbox(checked = source in selectedSources, onCheckedChange = { onToggleSource(source) })
                        Text(displayName(source), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(
                            count.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    }
                }

                item(key = "hint") {
                    Text(
                        stringResource(Res.string.fav_long_press_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }

            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                val rollSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                AnimatedContent(
                    targetState = resultCount,
                    transitionSpec = {
                        val up = targetState > initialState
                        (slideInVertically(rollSpec) { if (up) it else -it } + fadeIn()) togetherWith
                            (slideOutVertically { if (up) -it else it } + fadeOut())
                    },
                    label = "resultCount",
                ) { count ->
                    Text(
                        if (count == 0) stringResource(Res.string.fav_no_titles_match)
                        else pluralStringResource(Res.plurals.fav_show_titles, count, count)
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetLabel(text: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
