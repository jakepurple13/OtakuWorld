package com.programmersbox.uiviews.all

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.CurrentSourceRepository
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.ComponentState
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalCurrentSource
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.OtakuBannerBox
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.ToasterItemsSetup
import com.programmersbox.uiviews.utils.components.DynamicSearchBar
import com.programmersbox.uiviews.utils.components.InfiniteListHandler
import com.programmersbox.uiviews.utils.components.NormalOtakuScaffold
import com.programmersbox.uiviews.utils.components.OtakuPullToRefreshBox
import com.programmersbox.uiviews.utils.components.OtakuPullToRefreshDefaults
import com.programmersbox.uiviews.utils.components.OtakuScaffold
import com.programmersbox.uiviews.utils.navigateToDetails
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun AllView(
    dao: ItemDao = LocalItemDao.current,
    currentSourceRepository: CurrentSourceRepository = LocalCurrentSource.current,
    allVm: AllViewModel = viewModel { AllViewModel(dao, currentSourceRepository) },
    isHorizontal: Boolean = false,
) {
    val isConnected by allVm.observeNetwork.collectAsState(initial = true)
    val source by currentSourceRepository
        .asFlow()
        .collectAsState(initial = null)

    LaunchedEffect(isConnected) {
        if (allVm.sourceList.isEmpty() && source != null && isConnected && allVm.count != 1) allVm.reset(source!!)
    }

    val info = LocalGenericInfo.current
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val showButton by remember { derivedStateOf { state.firstVisibleItemIndex > 0 } }

    val navController = LocalNavController.current

    val focusManager = LocalFocusManager.current
    val showBlur by LocalSettingsHandling.current.rememberShowBlur()
    val hazeState = remember { HazeState() }

    OtakuScaffold(
        topBar = {
            var active by rememberSaveable { mutableStateOf(false) }

            DynamicSearchBar(
                isDocked = isHorizontal,
                query = allVm.searchText,
                onQueryChange = { allVm.searchText = it },
                onSearch = {
                    focusManager.clearFocus()
                    allVm.search()
                },
                active = active,
                onActiveChange = {
                    active = it
                    if (!active) focusManager.clearFocus()
                },
                placeholder = {
                    Text(
                        stringResource(
                            R.string.searchFor,
                            source?.serviceName.orEmpty()
                        )
                    )
                },
                leadingIcon = {
                    AnimatedVisibility(
                        visible = active,
                        enter = fadeIn() + slideInHorizontally(),
                        exit = slideOutHorizontally() + fadeOut()
                    ) {
                        IconButton(onClick = { active = false }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(allVm.searchText.isNotEmpty()) {
                            IconButton(onClick = { allVm.searchText = "" }) {
                                Icon(Icons.Default.Cancel, null)
                            }
                        }
                        AnimatedVisibility(active) {
                            Text(allVm.searchList.size.toString())
                        }

                        AnimatedVisibility(
                            visible = showButton,
                            enter = fadeIn() + slideInHorizontally { it / 2 },
                            exit = slideOutHorizontally { it / 2 } + fadeOut()
                        ) {
                            IconButton(onClick = { scope.launch { state.animateScrollToItem(0) } }) {
                                Icon(Icons.Default.ArrowUpward, null)
                            }
                        }
                    }
                },
                colors = SearchBarDefaults.colors(
                    containerColor = animateColorAsState(
                        MaterialTheme.colorScheme.surface.copy(
                            alpha = if (active) {
                                1f
                            } else {
                                if (showBlur) 0f else 1f
                            }
                        ),
                        label = ""
                    ).value,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .let {
                        if (showBlur) {
                            val surface = MaterialTheme.colorScheme.surface
                            it.hazeChild(hazeState) {
                                backgroundColor = surface
                            }
                        } else it
                    }
            ) {
                var showBanner by remember { mutableStateOf(false) }
                OtakuBannerBox(
                    showBanner = showBanner,
                    placeholder = koinInject<AppLogo>().logoId,
                ) {
                    OtakuPullToRefreshBox(
                        isRefreshing = allVm.isSearching,
                        onRefresh = {},
                        enabled = { false },
                    ) {
                        info.SearchListView(
                            list = allVm.searchList,
                            listState = rememberLazyGridState(),
                            favorites = allVm.favoriteList,
                            onLongPress = { item, c ->
                                newItemModel(if (c == ComponentState.Pressed) item else null)
                                showBanner = c == ComponentState.Pressed
                            },
                            paddingValues = PaddingValues(0.dp),
                            modifier = Modifier
                        ) { navController.navigateToDetails(it) }
                    }
                }
            }
        }
    ) { p1 ->
        var showBanner by remember { mutableStateOf(false) }
        OtakuBannerBox(
            showBanner = showBanner,
            placeholder = koinInject<AppLogo>().logoId,
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
                        AllScreen(
                            itemInfoChange = this@OtakuBannerBox::newItemModel,
                            state = state,
                            showBanner = { showBanner = it },
                            isRefreshing = allVm.isRefreshing,
                            sourceList = allVm.sourceList,
                            favoriteList = allVm.favoriteList,
                            onLoadMore = allVm::loadMore,
                            onReset = allVm::reset,
                            paddingValues = p1,
                            modifier = if (showBlur)
                                Modifier.haze(hazeState)
                            else
                                Modifier
                        )
                    }
                }
            }
        }
        ToasterItemsSetup(
            toastItems = allVm,
            alignment = Alignment.TopCenter,
            modifier = Modifier.padding(p1)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AllScreen(
    isRefreshing: Boolean,
    sourceList: List<ItemModel>,
    favoriteList: List<DbModel>,
    onLoadMore: (ApiService) -> Unit,
    onReset: (ApiService) -> Unit,
    itemInfoChange: (ItemModel?) -> Unit,
    state: LazyGridState,
    showBanner: (Boolean) -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val info = LocalGenericInfo.current
    val source by LocalCurrentSource.current.asFlow().collectAsState(initial = null)
    val navController = LocalNavController.current
    val pullRefreshState = rememberPullToRefreshState()
    NormalOtakuScaffold { p ->
        Box(
            modifier = modifier
                .padding(p)
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = { source?.let { onReset(it) } }
                )
        ) {
            if (sourceList.isEmpty()) {
                Box(Modifier.padding(paddingValues)) {
                    info.ComposeShimmerItem()
                }
            } else {
                info.AllListView(
                    list = sourceList,
                    listState = state,
                    favorites = favoriteList,
                    onLongPress = { item, c ->
                        itemInfoChange(if (c == ComponentState.Pressed) item else null)
                        showBanner(c == ComponentState.Pressed)
                    },
                    paddingValues = paddingValues,
                    modifier = Modifier
                ) { navController.navigateToDetails(it) }
            }
            OtakuPullToRefreshDefaults.ScalingIndicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (source?.canScrollAll == true && sourceList.isNotEmpty()) {
            InfiniteListHandler(listState = state, buffer = info.scrollBuffer) {
                source?.let { onLoadMore(it) }
            }
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
            onLoadMore = {},
            onReset = {},
            paddingValues = PaddingValues(0.dp)
        )
    }
}