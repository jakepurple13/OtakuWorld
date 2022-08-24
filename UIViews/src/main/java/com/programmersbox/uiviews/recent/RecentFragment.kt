package com.programmersbox.uiviews.recent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.programmersbox.models.sourceFlow
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.*
import com.programmersbox.uiviews.utils.components.InfiniteListHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import ru.beryukhov.reactivenetwork.ReactiveNetwork
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun RecentView(
    recentVm: RecentViewModel,
    info: GenericInfo,
    navController: NavController,
    logo: MainLogo
) {
    val context = LocalContext.current
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val source by sourceFlow.collectAsState(initial = null)
    val refresh = rememberSwipeRefreshState(isRefreshing = recentVm.isRefreshing)

    val isConnected by ReactiveNetwork()
        .observeInternetConnectivity()
        .flowOn(Dispatchers.IO)
        .collectAsState(initial = true)

    LaunchedEffect(isConnected) {
        if (recentVm.sourceList.isEmpty() && source != null && isConnected && recentVm.count != 1) recentVm.reset(context, source!!)
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val showButton by remember { derivedStateOf { state.firstVisibleItemIndex > 0 } }
    OtakuScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            InsetSmallTopAppBar(
                title = { Text(stringResource(R.string.currentSource, source?.serviceName.orEmpty())) },
                actions = {
                    AnimatedVisibility(visible = showButton) {
                        IconButton(onClick = { scope.launch { state.animateScrollToItem(0) } }) {
                            Icon(Icons.Default.ArrowUpward, null)
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = { Spacer(modifier = Modifier.height(1.dp)) }
    ) { p ->
        var showBanner by remember { mutableStateOf(false) }
        M3OtakuBannerBox(
            showBanner = showBanner,
            placeholder = logo.logoId,
            modifier = Modifier.padding(p)
        ) { itemInfo ->
            Crossfade(
                targetState = isConnected,
                modifier = Modifier.padding(p)
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
                                modifier = Modifier.size(50.dp, 50.dp),
                                colorFilter = ColorFilter.tint(M3MaterialTheme.colorScheme.onBackground)
                            )
                            Text(stringResource(R.string.you_re_offline), style = M3MaterialTheme.typography.titleLarge)
                        }
                    }
                    true -> {
                        when {
                            recentVm.sourceList.isEmpty() -> info.ComposeShimmerItem()
                            else -> {
                                SwipeRefresh(
                                    //modifier = Modifier.padding(p),
                                    state = refresh,
                                    onRefresh = { source?.let { recentVm.reset(context, it) } }
                                ) {
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