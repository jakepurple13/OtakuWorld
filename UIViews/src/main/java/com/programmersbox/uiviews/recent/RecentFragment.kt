@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.programmersbox.uiviews.recent

import android.content.Context
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.CurrentSourceRepository
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.ComponentState
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalCurrentSource
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.LocalSourcesRepository
import com.programmersbox.uiviews.utils.OtakuBannerBox
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.components.InfiniteListHandler
import com.programmersbox.uiviews.utils.components.NoSourcesInstalled
import com.programmersbox.uiviews.utils.components.OtakuHazeScaffold
import com.programmersbox.uiviews.utils.components.OtakuPullToRefreshBox
import com.programmersbox.uiviews.utils.currentService
import com.programmersbox.uiviews.utils.navigateToDetails
import com.programmersbox.uiviews.utils.showSourceChooser
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun RecentView(
    dao: ItemDao = LocalItemDao.current,
    context: Context = LocalContext.current,
    sourceRepository: SourceRepository = LocalSourcesRepository.current,
    currentSourceRepository: CurrentSourceRepository = LocalCurrentSource.current,
    recentVm: RecentViewModel = viewModel { RecentViewModel(dao, context, sourceRepository, currentSourceRepository) },
) {
    val info = LocalGenericInfo.current
    val navController = LocalNavController.current
    val state = recentVm.gridState
    val scope = rememberCoroutineScope()
    val source = recentVm.currentSource
    val pull = rememberPullToRefreshState()

    val showBlur by LocalSettingsHandling.current.rememberShowBlur()

    val isConnected by recentVm.observeNetwork.collectAsState(initial = true)

    LaunchedEffect(isConnected) {
        if (recentVm.sourceList.isEmpty() && source != null && isConnected && recentVm.count != 1) recentVm.reset(context)
    }

    val showButton by remember { derivedStateOf { state.firstVisibleItemIndex > 0 } }

    val sourceList = recentVm.sources
    val initSource = remember(source) { sourceList.indexOfFirst { it.apiService == source } }
    val pagerState = rememberPagerState(
        initialPage = initSource.coerceAtLeast(0),
        initialPageOffsetFraction = 0f
    ) { sourceList.size }

    LaunchedEffect(initSource) {
        if (initSource != -1) pagerState.scrollToPage(initSource)
    }

    LaunchedEffect(pagerState, initSource) {
        snapshotFlow { pagerState.settledPage }.collect {
            if (initSource != -1) {
                sourceList.getOrNull(it)?.let { service ->
                    currentSourceRepository.emit(service.apiService)
                    context.currentService = service.apiService.serviceName
                }
            }
        }
    }

    var showSourceChooser by showSourceChooser()

    OtakuHazeScaffold(
        topBar = {
            InsetSmallTopAppBar(
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
                        ListItem(
                            overlineContent = { Text("Current Source:") },
                            headlineContent = {
                                Text(sourceList.getOrNull(targetState)?.apiService?.serviceName.orEmpty())
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (showBlur) Color.Transparent else Color.Unspecified),
            )
        },
        blurTopBar = showBlur
    ) { p ->
        var showBanner by remember { mutableStateOf(false) }
        OtakuBannerBox(
            showBanner = showBanner,
            placeholder = koinInject<AppLogo>().logoId,
            modifier = Modifier.padding(p)
        ) {
            Crossfade(
                targetState = isConnected,
                label = ""
            ) { connected ->
                when (connected) {
                    false -> {
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
                    }

                    true -> {
                        OtakuPullToRefreshBox(
                            isRefreshing = recentVm.isRefreshing,
                            onRefresh = { recentVm.reset(context) },
                            state = pull,
                            paddingValues = p
                        ) {
                            when {
                                sourceList.isEmpty() -> NoSourcesInstalled(Modifier.fillMaxSize())
                                recentVm.sourceList.isEmpty() -> Box(
                                    Modifier
                                        .padding(p)
                                        .fillMaxSize()
                                ) { info.ComposeShimmerItem() }

                                else -> {
                                    info.ItemListView(
                                        list = recentVm.sourceList,
                                        listState = state,
                                        favorites = recentVm.favoriteList,
                                        paddingValues = p,
                                        onLongPress = { item, c ->
                                            newItemModel(if (c == ComponentState.Pressed) item else null)
                                            showBanner = c == ComponentState.Pressed
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    ) { navController.navigateToDetails(it) }
                                }
                            }
                        }

                        if (source?.canScroll == true && recentVm.sourceList.isNotEmpty()) {
                            InfiniteListHandler(listState = state, buffer = info.scrollBuffer) {
                                recentVm.loadMore(context)
                            }
                        }
                    }
                }
            }
        }
    }
}

@LightAndDarkPreviews
@Composable
private fun RecentPreview() {
    PreviewTheme {
        RecentView()
    }
}