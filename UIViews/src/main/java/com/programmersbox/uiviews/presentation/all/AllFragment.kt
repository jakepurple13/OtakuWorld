package com.programmersbox.uiviews.presentation.all

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.presentation.components.NormalOtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.repository.CurrentSourceRepository
import com.programmersbox.kmpuiviews.utils.LocalCurrentSource
import com.programmersbox.kmpuiviews.utils.LocalNavController
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.components.DynamicSearchBar
import com.programmersbox.uiviews.presentation.components.InfiniteListHandler
import com.programmersbox.uiviews.presentation.components.OtakuPullToRefreshBox
import com.programmersbox.uiviews.presentation.components.OtakuPullToRefreshDefaults
import com.programmersbox.uiviews.presentation.components.optionsKmpSheet
import com.programmersbox.uiviews.presentation.navigateToDetails
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.ToasterItemsSetup
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun AllView(
    currentSourceRepository: CurrentSourceRepository = LocalCurrentSource.current,
    allVm: AllViewModel = koinViewModel(),
    isHorizontal: Boolean = false,
) {
    val isConnected by allVm.observeNetwork.collectAsStateWithLifecycle(true)
    val source by currentSourceRepository
        .asFlow()
        .collectAsStateWithLifecycle(null)

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

    var optionsSheet by optionsKmpSheet()

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
                    inputFieldColors = if (showBlur)
                        SearchBarDefaults.inputFieldColors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        )
                    else
                        SearchBarDefaults.inputFieldColors()
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .let {
                        if (showBlur) {
                            val surface = MaterialTheme.colorScheme.surface
                            it.hazeEffect(hazeState) {
                                backgroundColor = surface
                                progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f, preferPerformance = true)
                            }
                        } else it
                    }
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
                        onLongPress = { item, c -> optionsSheet = item },
                        paddingValues = LocalNavHostPadding.current,
                        modifier = Modifier
                    ) { navController.navigateToDetails(it) }
                }
            }
        }
    ) { p1 ->

        Crossfade(targetState = isConnected, label = "") { connected ->
            if (!connected) {
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
            } else {
                AllScreen(
                    itemInfoChange = { optionsSheet = it },
                    state = state,
                    isRefreshing = allVm.isRefreshing,
                    sourceList = allVm.sourceList,
                    favoriteList = allVm.favoriteList,
                    onLoadMore = allVm::loadMore,
                    onReset = allVm::reset,
                    paddingValues = p1,
                    modifier = if (showBlur)
                        Modifier.hazeSource(hazeState)
                    else
                        Modifier
                )
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
    sourceList: List<KmpItemModel>,
    favoriteList: List<DbModel>,
    onLoadMore: (KmpApiService) -> Unit,
    onReset: (KmpApiService) -> Unit,
    itemInfoChange: (KmpItemModel?) -> Unit,
    state: LazyGridState,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val info = LocalGenericInfo.current
    val source by LocalCurrentSource.current.asFlow().collectAsStateWithLifecycle(null)
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
                    onLongPress = { item, c -> itemInfoChange(item) },
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
            isRefreshing = true,
            sourceList = emptyList(),
            favoriteList = emptyList(),
            onLoadMore = {},
            onReset = {},
            paddingValues = PaddingValues(0.dp)
        )
    }
}