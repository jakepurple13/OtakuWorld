package com.programmersbox.kmpuiviews.presentation.recent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material.icons.filled.HideSource
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.components.NoSourcesInstalled
import com.programmersbox.kmpuiviews.presentation.components.OtakuHazeScaffold
import com.programmersbox.kmpuiviews.presentation.components.OtakuPullToRefreshBox
import com.programmersbox.kmpuiviews.presentation.components.optionsKmpSheet
import com.programmersbox.kmpuiviews.presentation.settings.utils.showSourceChooser
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.composables.InfiniteListHandler
import com.programmersbox.kmpuiviews.utils.rememberBiometricOpening
import dev.chrisbanes.haze.HazeProgressive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.addToFavorites
import otakuworld.kmpuiviews.generated.resources.removeFromFavorites
import otakuworld.kmpuiviews.generated.resources.you_re_offline

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun RecentView(
    viewModel: RecentViewModel = koinViewModel(),
) {
    val info = koinInject<KmpGenericInfo>()
    val navController = LocalNavActions.current
    val settingsHandling: NewSettingsHandling = koinInject()
    val dataStoreHandling = koinInject<DataStoreHandling>()
    val itemDao = koinInject<ItemDao>()
    val state = viewModel.gridState
    val scope = rememberCoroutineScope()
    val source = viewModel.currentSource
    val pull = rememberPullToRefreshState()

    val showBlur by settingsHandling.rememberShowBlur()

    val isConnected by viewModel.observeNetwork.collectAsStateWithLifecycle(true)

    val showButton by remember { derivedStateOf { state.firstVisibleItemIndex > 0 } }

    val sourceList = viewModel.sources
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

    var optionsSheet by optionsKmpSheet(
        moreContent = {
            Crossfade(
                viewModel.favoriteList.any { f -> f.url == it.url }
            ) { target ->
                if (target) {
                    OptionsItem(
                        title = stringResource(Res.string.removeFromFavorites),
                        onClick = { viewModel.favoriteAction(RecentViewModel.FavoriteAction.Remove(it.itemModel)) }
                    )
                } else {
                    OptionsItem(
                        title = stringResource(Res.string.addToFavorites),
                        onClick = { viewModel.favoriteAction(RecentViewModel.FavoriteAction.Add(it.itemModel)) }
                    )
                }
            }
        }
    )

    var showSourceChooser by showSourceChooser()

    OtakuHazeScaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    AnimatedVisibility(
                        viewModel.isIncognitoSource,
                        enter = slideInHorizontally() + fadeIn(),
                        exit = fadeOut() + slideOutHorizontally()
                    ) {
                        Icon(Icons.Default.HideSource, null)
                    }
                },
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
                viewModel.snackbarHostState,
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
                    Text(stringResource(Res.string.you_re_offline), style = MaterialTheme.typography.titleLarge)
                }
            } else {
                val biometric = rememberBiometricOpening()

                OtakuPullToRefreshBox(
                    isRefreshing = viewModel.isRefreshing,
                    onRefresh = { viewModel.reset() },
                    state = pull,
                    paddingValues = p
                ) {
                    when {
                        sourceList.isEmpty() -> NoSourcesInstalled(Modifier.fillMaxSize())

                        viewModel.filteredSourceList.isEmpty() -> {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(top = p.calculateTopPadding())
                            ) { info.ComposeShimmerItem() }
                        }

                        else -> {
                            info.ItemListView(
                                list = viewModel.filteredSourceList,
                                listState = state,
                                favorites = viewModel.favoriteList,
                                paddingValues = p,
                                onLongPress = { item, c ->
                                    optionsSheet = item
                                    //newItemModel(item)
                                    //newItemModel(if (c == ComponentState.Pressed) item else null)
                                    //showBanner = c == ComponentState.Pressed
                                },
                                modifier = Modifier.fillMaxSize()
                            ) { scope.launch { biometric.openIfNotIncognito(it) } }
                        }
                    }
                }

                if (source?.canScroll == true && viewModel.filteredSourceList.isNotEmpty()) {
                    InfiniteListHandler(listState = state, buffer = info.scrollBuffer) {
                        viewModel.loadMore()
                    }
                }
            }
        }
    }
}