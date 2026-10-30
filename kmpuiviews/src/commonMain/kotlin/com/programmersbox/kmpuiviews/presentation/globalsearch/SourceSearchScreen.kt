package com.programmersbox.kmpuiviews.presentation.globalsearch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.NormalOtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.optionsKmpSheet
import com.programmersbox.kmpuiviews.presentation.components.placeholder.M3PlaceHolderCoverCard
import com.programmersbox.kmpuiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.kmpuiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.kmpuiviews.presentation.components.placeholder.shimmer
import com.programmersbox.kmpuiviews.presentation.components.plus
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.adaptiveGridCell
import com.programmersbox.kmpuiviews.utils.composables.InfiniteListHandler
import com.programmersbox.kmpuiviews.utils.rememberBiometricOpening
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.source_search_end
import otakuworld.kmpuiviews.generated.resources.source_search_failed
import otakuworld.kmpuiviews.generated.resources.source_search_no_results
import otakuworld.kmpuiviews.generated.resources.source_search_page
import otakuworld.kmpuiviews.generated.resources.source_search_retry
import otakuworld.kmpuiviews.generated.resources.source_search_scroll_to_top
import otakuworld.kmpuiviews.generated.resources.source_search_subtitle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SourceSearchScreen(
    screen: Screen.SourceSearchScreen,
    viewModel: SourceSearchViewModel = koinViewModel { parametersOf(screen) },
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val biometric = rememberBiometricOpening()
    var optionsSheet by optionsKmpSheet()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val showScrollToTop by remember { derivedStateOf { gridState.firstVisibleItemIndex > 6 } }

    val language = viewModel.source?.language
    val sourceName = viewModel.source
        ?.displayName
        ?.let { name -> language?.let { name.removeSuffix(" ($it)") } ?: name }
        ?: screen.serviceName

    NormalOtakuScaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton() },
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(sourceName)
                            language?.let { LanguageBadge(it) }
                        }
                        Text(
                            pluralStringResource(
                                Res.plurals.source_search_subtitle,
                                viewModel.itemCount,
                                viewModel.query,
                                viewModel.itemCount,
                                viewModel.pages.size.coerceAtLeast(1),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = scaleIn(),
                exit = scaleOut(),
                modifier = Modifier.padding(LocalNavHostPadding.current)
            ) {
                FloatingActionButton(onClick = { scope.launch { gridState.animateScrollToItem(0) } }) {
                    Icon(Icons.Default.ArrowUpward, stringResource(Res.string.source_search_scroll_to_top))
                }
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        LazyVerticalGrid(
            columns = adaptiveGridCell(),
            state = gridState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = padding + LocalNavHostPadding.current,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            viewModel.pages.forEach { page ->
                if (page.number > 1) {
                    item(
                        key = "page_${page.number}",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = "divider",
                    ) { PageDivider(page.number, Modifier.animateItem()) }
                }
                items(page.items, key = { it.url }, contentType = { "cover" }) { model ->
                    SearchCoverCard(
                        model = model,
                        onLongPress = { optionsSheet = model },
                        modifier = Modifier.animateItem(),
                    ) { scope.launch { biometric.openIfNotIncognito(model) } }
                }
            }

            footer(
                viewModel = viewModel,
                sourceName = sourceName,
            )
        }

        if (viewModel.pages.isNotEmpty()) {
            InfiniteListHandler(listState = gridState, buffer = 6) { viewModel.loadMore() }
        }
    }
}

private fun LazyGridScope.footer(
    viewModel: SourceSearchViewModel,
    sourceName: String,
) {
    val error = viewModel.error
    when {
        viewModel.isLoading || viewModel.source == null -> items(6, contentType = { "placeholder" }) {
            M3PlaceHolderCoverCard(
                placeHolder = painterLogo(),
                modifier = Modifier
                    .animateItem()
                    .m3placeholder(true, highlight = PlaceholderHighlight.shimmer())
            )
        }

        error != null -> item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    stringResource(Res.string.source_search_failed, viewModel.pages.size + 1, sourceName),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = viewModel::retry) { Text(stringResource(Res.string.source_search_retry)) }
            }
        }

        viewModel.endReached -> item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
            Text(
                stringResource(
                    if (viewModel.pages.isEmpty()) Res.string.source_search_no_results else Res.string.source_search_end,
                    sourceName,
                    viewModel.query,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun PageDivider(number: Int, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        HorizontalDivider(Modifier.weight(1f))
        Text(
            stringResource(Res.string.source_search_page, number),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(Modifier.weight(1f))
    }
}

@Composable
internal fun LanguageBadge(language: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            language,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
