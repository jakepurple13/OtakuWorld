package com.programmersbox.uiviews.recent

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.models.sourceFlow
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.ComponentState
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.M3OtakuBannerBox
import com.programmersbox.uiviews.utils.OtakuScaffold
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.components.InfiniteListHandler
import com.programmersbox.uiviews.utils.currentService
import com.programmersbox.uiviews.utils.navigateToDetails
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun RecentView(
    logo: MainLogo,
    dao: ItemDao = LocalItemDao.current,
    context: Context = LocalContext.current,
    recentVm: RecentViewModel = viewModel { RecentViewModel(dao, context) },
) {
    val info = LocalGenericInfo.current
    val navController = LocalNavController.current
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val source by sourceFlow.collectAsState(initial = null)
    val pull = rememberPullRefreshState(refreshing = recentVm.isRefreshing, onRefresh = { source?.let { recentVm.reset(context, it) } })

    val isConnected by recentVm.observeNetwork.collectAsState(initial = true)

    LaunchedEffect(isConnected) {
        if (recentVm.sourceList.isEmpty() && source != null && isConnected && recentVm.count != 1) recentVm.reset(context, source!!)
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val showButton by remember { derivedStateOf { state.firstVisibleItemIndex > 0 } }

    val sourceList = remember { info.sourceList() }
    val initSource = remember(source) { sourceList.indexOf(source) }
    val pagerState = rememberPagerState(initSource.coerceAtLeast(0))
    LaunchedEffect(initSource) {
        if (initSource != -1) pagerState.scrollToPage(initSource)
    }
    LaunchedEffect(pagerState.currentPage, initSource) {
        if (initSource != -1) {
            sourceList.getOrNull(pagerState.currentPage)?.let { service ->
                sourceFlow.emit(service)
                context.currentService = service.serviceName
            }
        }
    }

    OtakuScaffold(
        topBar = {
            InsetSmallTopAppBar(
                title = {
                    AnimatedContent(
                        targetState = pagerState.targetPage,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInVertically { height -> height } + fadeIn() with
                                        slideOutVertically { height -> -height } + fadeOut()
                            } else {
                                slideInVertically { height -> -height } + fadeIn() with
                                        slideOutVertically { height -> height } + fadeOut()
                            }.using(SizeTransform(clip = false))
                        },
                        label = ""
                    ) { targetState ->
                        Text(stringResource(R.string.currentSource, sourceList.getOrNull(targetState)?.serviceName.orEmpty()))
                    }
                },
                actions = {
                    VerticalPager(
                        pageCount = info.sourceList().size,
                        state = pagerState
                    ) {
                        Box(Modifier.fillMaxHeight()) {
                            IconButton(
                                onClick = { navController.navigate(Screen.SourceChooserScreen.route) },
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
                scrollBehavior = scrollBehavior
            )
        }
    ) { p ->
        var showBanner by remember { mutableStateOf(false) }
        M3OtakuBannerBox(
            showBanner = showBanner,
            placeholder = logo.logoId,
            modifier = Modifier.padding(p)
        ) { itemInfo ->
            Crossfade(
                targetState = isConnected,
                modifier = Modifier
                    .padding(p)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
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
                                colorFilter = ColorFilter.tint(M3MaterialTheme.colorScheme.onBackground)
                            )
                            Text(stringResource(R.string.you_re_offline), style = M3MaterialTheme.typography.titleLarge)
                        }
                    }

                    true -> {
                        Box(
                            modifier = Modifier.pullRefresh(pull)
                        ) {
                            when {
                                recentVm.sourceList.isEmpty() -> info.ComposeShimmerItem()
                                else -> {
                                    info.ItemListView(
                                        list = recentVm.sourceList,
                                        listState = state,
                                        favorites = recentVm.favoriteList,
                                        onLongPress = { item, c ->
                                            itemInfo.value = if (c == ComponentState.Pressed) item else null
                                            showBanner = c == ComponentState.Pressed
                                        }
                                    ) { navController.navigateToDetails(it) }
                                }
                            }
                            PullRefreshIndicator(
                                refreshing = recentVm.isRefreshing,
                                state = pull,
                                modifier = Modifier.align(Alignment.TopCenter),
                                backgroundColor = M3MaterialTheme.colorScheme.background,
                                contentColor = M3MaterialTheme.colorScheme.onBackground,
                                scale = true
                            )
                        }

                        if (source?.canScroll == true && recentVm.sourceList.isNotEmpty()) {
                            InfiniteListHandler(listState = state, buffer = info.scrollBuffer) {
                                source?.let { recentVm.loadMore(context, it) }
                            }
                        }
                    }
                }
            }
        }
    }
}