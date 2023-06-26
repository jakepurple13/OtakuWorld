package com.programmersbox.uiviews.all

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BrowseGallery
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.sourceFlow
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.ComponentState
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.OtakuBannerBox
import com.programmersbox.uiviews.utils.OtakuScaffold
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.components.InfiniteListHandler
import com.programmersbox.uiviews.utils.navigateToDetails
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun AllView(
    logo: MainLogo,
    context: Context = LocalContext.current,
    dao: ItemDao = LocalItemDao.current,
    allVm: AllViewModel = viewModel { AllViewModel(dao, context) },
) {
    val isConnected by allVm.observeNetwork.collectAsState(initial = true)
    val source by sourceFlow.collectAsState(initial = null)

    LaunchedEffect(isConnected) {
        if (allVm.sourceList.isEmpty() && source != null && isConnected && allVm.count != 1) allVm.reset(context, source!!)
    }

    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val showButton by remember { derivedStateOf { state.firstVisibleItemIndex > 0 } }
    val scrollBehaviorTop = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) { 2 }

    OtakuScaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorTop.nestedScrollConnection),
        topBar = {
            Column {
                InsetSmallTopAppBar(
                    title = { Text(stringResource(R.string.currentSource, source?.serviceName.orEmpty())) },
                    actions = {
                        AnimatedVisibility(visible = showButton) {
                            IconButton(onClick = { scope.launch { state.animateScrollToItem(0) } }) {
                                Icon(Icons.Default.ArrowUpward, null)
                            }
                        }
                    },
                    scrollBehavior = scrollBehaviorTop
                )

                TabRow(
                    // Our selected tab is our current page
                    selectedTabIndex = pagerState.currentPage,
                    // Override the indicator, using the provided pagerTabIndicatorOffset modifier
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                        )
                    }
                ) {
                    // Add tabs for all of our pages
                    LeadingIconTab(
                        text = { Text(stringResource(R.string.all)) },
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        icon = { Icon(Icons.Default.BrowseGallery, null) }
                    )

                    LeadingIconTab(
                        text = { Text(stringResource(R.string.search)) },
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        icon = { Icon(Icons.Default.Search, null) }
                    )
                }
            }
        }
    ) { p1 ->
        var showBanner by remember { mutableStateOf(false) }
        OtakuBannerBox(
            showBanner = showBanner,
            placeholder = logo.logoId,
            modifier = Modifier.padding(p1)
        ) {
            Crossfade(targetState = isConnected, label = "") { connected ->
                when (connected) {
                    false -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(p1),
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
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = p1
                        ) { page ->
                            when (page) {
                                0 -> AllScreen(
                                    itemInfoChange = this@OtakuBannerBox::onNewItem,
                                    state = state,
                                    showBanner = { showBanner = it },
                                    isRefreshing = allVm.isRefreshing,
                                    sourceList = allVm.sourceList,
                                    favoriteList = allVm.favoriteList,
                                    onLoadMore = allVm::loadMore,
                                    onReset = allVm::reset
                                )

                                1 -> SearchScreen(
                                    itemInfoChange = this@OtakuBannerBox::onNewItem,
                                    showBanner = { showBanner = it },
                                    searchList = allVm.searchList,
                                    searchText = allVm.searchText,
                                    onSearchChange = { allVm.searchText = it },
                                    isSearching = allVm.isSearching,
                                    search = allVm::search,
                                    favoriteList = allVm.favoriteList
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AllScreen(
    isRefreshing: Boolean,
    sourceList: List<ItemModel>,
    favoriteList: List<DbModel>,
    onLoadMore: (Context?, ApiService) -> Unit,
    onReset: (Context?, ApiService) -> Unit,
    itemInfoChange: (ItemModel?) -> Unit,
    state: LazyGridState,
    showBanner: (Boolean) -> Unit
) {
    val info = LocalGenericInfo.current
    val source by sourceFlow.collectAsState(initial = null)
    val navController = LocalNavController.current
    val context = LocalContext.current
    val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = { source?.let { onReset(context, it) } })
    OtakuScaffold { p ->
        Box(
            modifier = Modifier
                .padding(p)
                .pullRefresh(pullRefreshState)
        ) {
            if (sourceList.isEmpty()) {
                info.ComposeShimmerItem()
            } else {
                info.AllListView(
                    list = sourceList,
                    listState = state,
                    favorites = favoriteList,
                    onLongPress = { item, c ->
                        itemInfoChange(if (c == ComponentState.Pressed) item else null)
                        showBanner(c == ComponentState.Pressed)
                    }
                ) { navController.navigateToDetails(it) }
            }
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = M3MaterialTheme.colorScheme.background,
                contentColor = M3MaterialTheme.colorScheme.onBackground,
                scale = true
            )
        }

        if (source?.canScrollAll == true && sourceList.isNotEmpty()) {
            InfiniteListHandler(listState = state, buffer = info.scrollBuffer) {
                source?.let { onLoadMore(context, it) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun SearchScreen(
    itemInfoChange: (ItemModel?) -> Unit,
    showBanner: (Boolean) -> Unit,
    searchList: List<ItemModel>,
    searchText: String,
    onSearchChange: (String) -> Unit,
    isSearching: Boolean,
    favoriteList: List<DbModel>,
    search: () -> Unit
) {
    val info = LocalGenericInfo.current
    val focusManager = LocalFocusManager.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val source by sourceFlow.collectAsState(initial = null)
    val navController = LocalNavController.current

    OtakuScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchChange,
                label = {
                    Text(
                        stringResource(
                            R.string.searchFor,
                            source?.serviceName.orEmpty()
                        )
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(searchList.size.toString())
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Cancel, null)
                        }
                    }
                },
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    search()
                })
            )
        }
    ) { p ->
        val pullRefreshState = rememberPullRefreshState(isSearching, onRefresh = {})
        Box(
            modifier = Modifier
                .pullRefresh(pullRefreshState, false)
                .padding(p)
        ) {
            info.SearchListView(
                list = searchList,
                listState = rememberLazyGridState(),
                favorites = favoriteList,
                onLongPress = { item, c ->
                    itemInfoChange(if (c == ComponentState.Pressed) item else null)
                    showBanner(c == ComponentState.Pressed)
                }
            ) { navController.navigateToDetails(it) }

            PullRefreshIndicator(
                refreshing = isSearching,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = M3MaterialTheme.colorScheme.background,
                contentColor = M3MaterialTheme.colorScheme.onBackground,
                scale = true
            )
        }
    }
}

// This is because we can't get access to the library one
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerTabIndicatorOffset(
    pagerState: PagerState,
    tabPositions: List<TabPosition>,
    pageIndexMapping: (Int) -> Int = { it },
): Modifier = layout { measurable, constraints ->
    if (tabPositions.isEmpty()) {
        // If there are no pages, nothing to show
        layout(constraints.maxWidth, 0) {}
    } else {
        val currentPage = minOf(tabPositions.lastIndex, pageIndexMapping(pagerState.currentPage))
        val currentTab = tabPositions[currentPage]
        val previousTab = tabPositions.getOrNull(currentPage - 1)
        val nextTab = tabPositions.getOrNull(currentPage + 1)
        val fraction = pagerState.currentPageOffsetFraction
        val indicatorWidth = if (fraction > 0 && nextTab != null) {
            lerp(currentTab.width, nextTab.width, fraction).roundToPx()
        } else if (fraction < 0 && previousTab != null) {
            lerp(currentTab.width, previousTab.width, -fraction).roundToPx()
        } else {
            currentTab.width.roundToPx()
        }
        val indicatorOffset = if (fraction > 0 && nextTab != null) {
            lerp(currentTab.left, nextTab.left, fraction).roundToPx()
        } else if (fraction < 0 && previousTab != null) {
            lerp(currentTab.left, previousTab.left, -fraction).roundToPx()
        } else {
            currentTab.left.roundToPx()
        }
        val placeable = measurable.measure(
            Constraints(
                minWidth = indicatorWidth,
                maxWidth = indicatorWidth,
                minHeight = 0,
                maxHeight = constraints.maxHeight
            )
        )
        layout(constraints.maxWidth, maxOf(placeable.height, constraints.minHeight)) {
            placeable.placeRelative(
                indicatorOffset,
                maxOf(constraints.minHeight - placeable.height, 0)
            )
        }
    }
}

@LightAndDarkPreviews
@Composable
private fun AllScreenPreview() {
    PreviewTheme {
        AllScreen(
            itemInfoChange = {},
            state = rememberLazyGridState(),
            showBanner = {},
            isRefreshing = true,
            sourceList = emptyList(),
            favoriteList = emptyList(),
            onLoadMore = { _, _ -> },
            onReset = { _, _ -> }
        )
    }
}

@LightAndDarkPreviews
@Composable
private fun SearchPreview() {
    PreviewTheme {
        SearchScreen(
            itemInfoChange = {},
            showBanner = {},
            searchList = emptyList(),
            searchText = "",
            onSearchChange = {},
            isSearching = true,
            search = {},
            favoriteList = emptyList()
        )
    }
}