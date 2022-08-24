package com.programmersbox.uiviews.all

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.material3.contentColorFor as m3ContentColorFor

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun AllView(
    allVm: AllViewModel,
    logo: MainLogo,
    info: GenericInfo,
    navController: NavController
) {
    val context = LocalContext.current

    val isConnected by ReactiveNetwork()
        .observeInternetConnectivity()
        .flowOn(Dispatchers.IO)
        .collectAsState(initial = true)

    val source by sourceFlow.collectAsState(initial = null)

    LaunchedEffect(isConnected) {
        if (allVm.sourceList.isEmpty() && source != null && isConnected && allVm.count != 1) allVm.reset(context, source!!)
    }

    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    BackHandler(scaffoldState.bottomSheetState.isExpanded) {
        scope.launch { scaffoldState.bottomSheetState.collapse() }
    }

    val state = rememberLazyGridState()
    val showButton by remember { derivedStateOf { state.firstVisibleItemIndex > 0 } }
    val scrollBehaviorTop = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    OtakuScaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorTop.nestedScrollConnection),
        topBar = {
            InsetSmallTopAppBar(
                title = { Text(stringResource(R.string.currentSource, source?.serviceName.orEmpty())) },
                actions = {
                    AnimatedVisibility(visible = showButton && scaffoldState.bottomSheetState.isCollapsed) {
                        androidx.compose.material3.IconButton(onClick = { scope.launch { state.animateScrollToItem(0) } }) {
                            Icon(Icons.Default.ArrowUpward, null)
                        }
                    }
                },
                scrollBehavior = scrollBehaviorTop
            )
        }
    ) { p1 ->
        var showBanner by remember { mutableStateOf(false) }
        M3OtakuBannerBox(
            showBanner = showBanner,
            placeholder = logo.logoId,
            modifier = Modifier.padding(p1)
        ) { itemInfo ->
            Crossfade(targetState = isConnected) { connected ->
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
                        BottomSheetScaffold(
                            modifier = Modifier.padding(p1),
                            backgroundColor = M3MaterialTheme.colorScheme.background,
                            contentColor = m3ContentColorFor(M3MaterialTheme.colorScheme.background),
                            scaffoldState = scaffoldState,
                            sheetPeekHeight = ButtonDefaults.MinHeight + 4.dp,
                            sheetContent = {
                                val focusManager = LocalFocusManager.current
                                val searchList = allVm.searchList
                                val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
                                OtakuScaffold(
                                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                                    topBar = {
                                        Column {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        if (scaffoldState.bottomSheetState.isCollapsed) scaffoldState.bottomSheetState.expand()
                                                        else scaffoldState.bottomSheetState.collapse()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(ButtonDefaults.MinHeight + 4.dp),
                                                shape = RoundedCornerShape(0f)
                                            ) { Text(stringResource(R.string.search)) }

                                            androidx.compose.material3.OutlinedTextField(
                                                value = allVm.searchText,
                                                onValueChange = { allVm.searchText = it },
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
                                                        IconButton(onClick = { allVm.searchText = "" }) {
                                                            Icon(Icons.Default.Cancel, null)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .padding(5.dp)
                                                    .fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                                keyboardActions = KeyboardActions(onSearch = {
                                                    focusManager.clearFocus()
                                                    allVm.search()
                                                })
                                            )
                                        }
                                    }
                                ) { p ->
                                    Box(modifier = Modifier.padding(p)) {
                                        SwipeRefresh(
                                            state = rememberSwipeRefreshState(isRefreshing = allVm.isSearching),
                                            onRefresh = {},
                                            swipeEnabled = false
                                        ) {
                                            info.SearchListView(
                                                list = searchList,
                                                listState = rememberLazyGridState(),
                                                favorites = allVm.favoriteList,
                                                onLongPress = { item, c ->
                                                    itemInfo.value = if (c == ComponentState.Pressed) item else null
                                                    showBanner = c == ComponentState.Pressed
                                                }
                                            ) {
                                                //findNavController().navigate(AllFragmentDirections.actionAllFragment2ToDetailsFragment3(it))
                                                navController.navigateToDetails(it)
                                            }
                                        }
                                    }
                                }
                            }
                        ) { p ->
                            if (allVm.sourceList.isEmpty()) {
                                info.ComposeShimmerItem()
                            } else {
                                val refresh = rememberSwipeRefreshState(isRefreshing = allVm.isRefreshing)
                                SwipeRefresh(
                                    modifier = Modifier.padding(p),
                                    state = refresh,
                                    onRefresh = { source?.let { allVm.reset(context, it) } }
                                ) {
                                    info.AllListView(
                                        list = allVm.sourceList,
                                        listState = state,
                                        favorites = allVm.favoriteList,
                                        onLongPress = { item, c ->
                                            itemInfo.value = if (c == ComponentState.Pressed) item else null
                                            showBanner = c == ComponentState.Pressed
                                        }
                                    ) {
                                        //findNavController().navigate(AllFragmentDirections.actionAllFragment2ToDetailsFragment3(it))
                                        navController.navigateToDetails(it)
                                    }
                                }
                            }

                            if (source?.canScrollAll == true && allVm.sourceList.isNotEmpty()) {
                                InfiniteListHandler(listState = state, buffer = info.scrollBuffer) {
                                    source?.let { allVm.loadMore(context, it) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}