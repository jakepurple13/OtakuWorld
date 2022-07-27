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
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.programmersbox.models.sourcePublish
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.ComponentState
import com.programmersbox.uiviews.utils.M3OtakuBannerBox
import com.programmersbox.uiviews.utils.components.InfiniteListHandler
import com.programmersbox.uiviews.utils.navigateToDetails
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
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
    val source by sourcePublish.subscribeAsState(initial = null)
    val refresh = rememberSwipeRefreshState(isRefreshing = recentVm.isRefreshing)

    val isConnected by ReactiveNetwork.observeInternetConnectivity()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeAsState(initial = true)

    LaunchedEffect(isConnected) {
        if (recentVm.sourceList.isEmpty() && source != null && isConnected && recentVm.count != 1) recentVm.reset(context, source!!)
    }

    val topAppBarScrollState = rememberTopAppBarState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topAppBarScrollState) }
    val showButton by remember { derivedStateOf { state.firstVisibleItemIndex > 0 } }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SmallTopAppBar(
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
        }
    ) { p ->
        var showBanner by remember { mutableStateOf(false) }
        M3OtakuBannerBox(
            showBanner = showBanner,
            placeholder = logo.logoId,
            modifier = Modifier.padding(p)
        ) { itemInfo ->
            Crossfade(targetState = isConnected) { connected ->
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