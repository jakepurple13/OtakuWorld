package com.programmersbox.uiviews.presentation.recent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.datastore.DataStoreHandling
import com.programmersbox.uiviews.presentation.components.InfiniteListHandler
import com.programmersbox.uiviews.presentation.components.NoSourcesInstalled
import com.programmersbox.uiviews.presentation.components.OtakuHazeScaffold
import com.programmersbox.uiviews.presentation.components.OtakuPullToRefreshBox
import com.programmersbox.uiviews.presentation.components.optionsSheet
import com.programmersbox.uiviews.presentation.navigateToDetails
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalNavHostPadding
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.PreviewThemeColorsSizes
import com.programmersbox.uiviews.utils.showSourceChooser
import dev.chrisbanes.haze.HazeProgressive
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun RecentView(
    recentVm: RecentViewModel = koinViewModel(),
) {
    val info = LocalGenericInfo.current
    val navController = LocalNavController.current
    val dataStoreHandling = koinInject<DataStoreHandling>()
    val state = recentVm.gridState
    val scope = rememberCoroutineScope()
    val source = recentVm.currentSource
    val pull = rememberPullToRefreshState()

    val showBlur by LocalSettingsHandling.current.rememberShowBlur()

    val isConnected by recentVm.observeNetwork.collectAsStateWithLifecycle(true)

    val showButton by remember { derivedStateOf { state.firstVisibleItemIndex > 0 } }

    val sourceList = recentVm.sources
    val initSource = remember(source) { sourceList.indexOfFirst { it.apiService == source } }
    val pagerState = rememberPagerState(
        initialPage = initSource.coerceAtLeast(0),
        initialPageOffsetFraction = 0f
    ) { sourceList.size }

    LaunchedEffect(initSource, source) {
        if (initSource != -1) pagerState.scrollToPage(initSource)
    }

    LaunchedEffect(pagerState, initSource) {
        snapshotFlow { pagerState.settledPage }
            .collect {
                if (initSource != -1) {
                    sourceList
                        .getOrNull(it)
                        ?.let { s -> dataStoreHandling.currentService.set(s.apiService.serviceName) }
                }
            }
    }

    var optionsSheet by optionsSheet(
        moreContent = {
            Crossfade(
                recentVm.favoriteList.any { f -> f.url == it.url }
            ) { target ->
                if (target) {
                    OptionsItem(
                        title = stringResource(R.string.removeFromFavorites),
                        onClick = { recentVm.favoriteAction(RecentViewModel.FavoriteAction.Remove(it.itemModel)) }
                    )
                } else {
                    OptionsItem(
                        title = stringResource(R.string.addToFavorites),
                        onClick = { recentVm.favoriteAction(RecentViewModel.FavoriteAction.Add(it.itemModel)) }
                    )
                }
            }
        }
    )

    var showSourceChooser by showSourceChooser()

    OtakuHazeScaffold(
        //state = LocalHazeState.current,
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = pagerState.targetPage,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                        slideOutVertically { height -> -height } + fadeOut()
                            } else {
                                slideInVertically { height -> -height } + fadeIn() togetherWith
                                        slideOutVertically { height -> height } + fadeOut()
                            }.using(SizeTransform(clip = false))
                        },
                        label = ""
                    ) { targetState ->
                        Text(sourceList.getOrNull(targetState)?.apiService?.serviceName.orEmpty())
                    }
                },
                actions = {
                    VerticalPager(
                        state = pagerState
                    ) {
                        Box(Modifier.fillMaxHeight()) {
                            IconButton(
                                onClick = { showSourceChooser = true },
                                modifier = Modifier.align(Alignment.Center)
                            ) { Icon(Icons.Default.Source, null) }
                        }
                    }
                    AnimatedVisibility(visible = showButton) {
                        IconButton(onClick = { scope.launch { state.animateScrollToItem(0) } }) {
                            Icon(Icons.Default.ArrowUpward, null)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (showBlur)
                        Color.Transparent
                    else
                        Color.Unspecified
                ),
            )
        },
        blurTopBar = showBlur,
        topBarBlur = {
            progressive = HazeProgressive.verticalGradient(
                startIntensity = 1f,
                endIntensity = 0f,
                preferPerformance = true
            )
        },
        snackbarHost = {
            SnackbarHost(
                recentVm.snackbarHostState,
                modifier = Modifier.padding(LocalNavHostPadding.current)
            )
        }
    ) { p ->
        Crossfade(
            targetState = isConnected,
            label = ""
        ) { connected ->
            if (!connected) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        Icons.Default.CloudOff,
                        null,
                        modifier = Modifier.size(50.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                    )
                    Text(stringResource(R.string.you_re_offline), style = MaterialTheme.typography.titleLarge)
                }
            } else {
                OtakuPullToRefreshBox(
                    isRefreshing = recentVm.isRefreshing,
                    onRefresh = { recentVm.reset() },
                    state = pull,
                    paddingValues = p
                ) {
                    when {
                        sourceList.isEmpty() -> NoSourcesInstalled(Modifier.fillMaxSize())

                        recentVm.filteredSourceList.isEmpty() -> {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(top = p.calculateTopPadding())
                            ) { info.ComposeShimmerItem() }
                        }

                        else -> {
                            info.ItemListView(
                                list = recentVm.filteredSourceList,
                                listState = state,
                                favorites = recentVm.favoriteList,
                                paddingValues = p,
                                onLongPress = { item, c ->
                                    optionsSheet = item
                                    //newItemModel(item)
                                    //newItemModel(if (c == ComponentState.Pressed) item else null)
                                    //showBanner = c == ComponentState.Pressed
                                },
                                modifier = Modifier.fillMaxSize()
                            ) { navController.navigateToDetails(it) }
                        }
                    }
                }

                if (source?.canScroll == true && recentVm.filteredSourceList.isNotEmpty()) {
                    InfiniteListHandler(listState = state, buffer = info.scrollBuffer) {
                        recentVm.loadMore()
                    }
                }
            }
        }
    }
}

@PreviewThemeColorsSizes
@Composable
private fun RecentPreview() {
    PreviewTheme {
        RecentView()
    }
}