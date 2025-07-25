package com.programmersbox.kmpuiviews.presentation.globalsearch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryItem
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.DynamicSearchBar
import com.programmersbox.kmpuiviews.presentation.components.LimitedBottomSheetScaffold
import com.programmersbox.kmpuiviews.presentation.components.LimitedBottomSheetScaffoldDefaults
import com.programmersbox.kmpuiviews.presentation.components.NormalOtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.OtakuPullToRefreshBox
import com.programmersbox.kmpuiviews.presentation.components.optionsKmpSheet
import com.programmersbox.kmpuiviews.presentation.components.placeholder.M3PlaceHolderCoverCard
import com.programmersbox.kmpuiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.kmpuiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.kmpuiviews.presentation.components.placeholder.shimmer
import com.programmersbox.kmpuiviews.presentation.components.plus
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.LocalHistoryDao
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.LocalSettingsHandling
import com.programmersbox.kmpuiviews.utils.adaptiveGridCell
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import com.programmersbox.kmpuiviews.utils.composables.modifiers.combineClickableWithIndication
import com.programmersbox.kmpuiviews.utils.rememberBiometricOpening
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.global_search
import otakuworld.kmpuiviews.generated.resources.search_found
import otakuworld.kmpuiviews.generated.resources.you_re_offline
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalTime::class,
)
@Composable
fun GlobalSearchScreen(
    screen: Screen.GlobalSearchScreen,
    isHorizontal: Boolean,
    dao: HistoryDao = LocalHistoryDao.current,
    viewModel: GlobalSearchViewModel = koinViewModel { parametersOf(screen) },
) {
    val hazeState = remember { HazeState() }
    val showBlur by LocalSettingsHandling.current.rememberShowBlur()
    val navController = LocalNavActions.current

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullToRefreshState()

    val networkState by viewModel.observeNetwork.collectAsStateWithLifecycle(true)

    val history by dao
        .searchHistory("%${viewModel.searchText.text}%")
        .collectAsStateWithLifecycle(emptyList())

    val bottomScaffold = rememberBottomSheetScaffoldState()
    var optionsSheet by optionsKmpSheet()
    var searchModelBottom by remember { mutableStateOf<SearchModel?>(null) }

    BackHandler(bottomScaffold.bottomSheetState.currentValue == SheetValue.Expanded) {
        scope.launch {
            runCatching {
                bottomScaffold.bottomSheetState.partialExpand()
            }.onFailure { navController.popBackStack() }
        }
    }

    val biometric = rememberBiometricOpening()

    LimitedBottomSheetScaffold(
        scaffoldState = bottomScaffold,
        topBar = {
            val searchBarState = rememberSearchBarState()

            fun closeSearchBar() {
                scope.launch { searchBarState.animateToCollapsed() }
            }
            DynamicSearchBar(
                textFieldState = viewModel.searchText,
                searchBarState = searchBarState,
                isDocked = isHorizontal,
                onSearch = {
                    closeSearchBar()
                    if (viewModel.searchText.text.isNotEmpty()) {
                        scope.launch(Dispatchers.IO) {
                            dao.insertHistory(
                                HistoryItem(
                                    time = Clock.System.now().toEpochMilliseconds(),
                                    searchText = viewModel.searchText.text.toString()
                                )
                            )
                        }
                    }
                    viewModel.searchForItems()
                },
                placeholder = { Text(stringResource(Res.string.global_search)) },
                leadingIcon = {
                    if (searchBarState.currentValue == SearchBarValue.Expanded) {
                        IconButton(
                            onClick = { closeSearchBar() }
                        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }

                    } else {
                        BackButton()
                    }
                },
                trailingIcon = {
                    AnimatedVisibility(viewModel.searchText.text.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchText = TextFieldState() }) {
                            Icon(Icons.Default.Cancel, null)
                        }
                    }
                },
                colors = SearchBarDefaults.colors(
                    inputFieldColors = if (showBlur)
                        SearchBarDefaults.inputFieldColors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        )
                    else
                        SearchBarDefaults.inputFieldColors()
                ),
                modifier = Modifier.let {
                    if (showBlur) {
                        val surface = MaterialTheme.colorScheme.surface
                        it.hazeEffect(
                            hazeState,
                            HazeMaterials.thin(surface)
                        ) {
                            backgroundColor = surface
                            progressive =
                                HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f, preferPerformance = true)
                        }
                    } else it
                }
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(history) { index, historyModel ->
                        Card(
                            onClick = {
                                viewModel.searchText = TextFieldState(historyModel.searchText)
                                closeSearchBar()
                                if (viewModel.searchText.text.isNotEmpty()) {
                                    scope.launch(Dispatchers.IO) {
                                        dao.insertHistory(
                                            HistoryItem(
                                                time = Clock.System.now().toEpochMilliseconds(),
                                                searchText = viewModel.searchText.text.toString()
                                            )
                                        )
                                    }
                                }
                                viewModel.searchForItems()
                            }
                        ) {
                            ListItem(
                                headlineContent = { Text(historyModel.searchText) },
                                leadingContent = { Icon(Icons.Filled.Search, contentDescription = null) },
                                trailingContent = {
                                    IconButton(
                                        onClick = { scope.launch { dao.deleteHistory(historyModel) } },
                                    ) { Icon(Icons.Default.Cancel, null) }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        sheetContent = searchModelBottom?.let { s ->
            {
                val sheetScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
                NormalOtakuScaffold(
                    topBar = {
                        TopAppBar(
                            scrollBehavior = sheetScrollBehavior,
                            title = { Text(s.apiName) },
                            actions = { Text(stringResource(Res.string.search_found, s.data.size)) },
                            windowInsets = WindowInsets(0.dp)
                        )
                    },
                    modifier = Modifier
                        .padding(bottom = LocalNavHostPadding.current.calculateBottomPadding())
                        .nestedScroll(sheetScrollBehavior.nestedScrollConnection),
                ) { p ->
                    LazyVerticalGrid(
                        columns = adaptiveGridCell(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = p,
                    ) {
                        items(s.data) { m ->
                            SearchCoverCard(
                                model = m,
                                onLongPress = { c -> optionsSheet = m }
                            ) { scope.launch { biometric.openIfNotIncognito(m) } }
                        }
                    }
                }
            }
        } ?: {},
        bottomSheet = LimitedBottomSheetScaffoldDefaults.bottomSheet(
            sheetPeekHeight = 0.dp
        ),
        colors = LimitedBottomSheetScaffoldDefaults.colors(
            topAppBarContainerColor = Color.Transparent
        )
    ) { padding ->
        Crossfade(targetState = networkState, label = "") { network ->
            if (network) {
                Content(
                    viewModel = viewModel,
                    pullRefreshState = pullRefreshState,
                    padding = padding,
                    listState = listState,
                    hazeState = hazeState,
                    showBlur = showBlur,
                    scope = scope,
                    navController = navController,
                    onSearchModel = { searchModelBottom = it },
                    bottomScaffold = bottomScaffold,
                    onLongPress = { optionsSheet = it }
                )
            } else {
                NoNetwork(padding)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    viewModel: GlobalSearchViewModel,
    pullRefreshState: PullToRefreshState,
    padding: PaddingValues,
    listState: LazyListState,
    hazeState: HazeState,
    showBlur: Boolean,
    scope: CoroutineScope,
    navController: NavigationActions,
    onSearchModel: (SearchModel) -> Unit,
    onLongPress: (KmpItemModel) -> Unit,
    bottomScaffold: BottomSheetScaffoldState,
) {
    val biometrics = rememberBiometricOpening()
    OtakuPullToRefreshBox(
        isRefreshing = viewModel.isRefreshing,
        state = pullRefreshState,
        onRefresh = {},
        enabled = { false },
        paddingValues = padding + LocalNavHostPadding.current
    ) {
        LazyColumn(
            state = listState,
            contentPadding = padding + LocalNavHostPadding.current,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .let {
                    if (showBlur)
                        it.hazeSource(hazeState)
                    else
                        it
                }
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            if (viewModel.isRefreshing) {
                items(3) {
                    Surface(
                        modifier = Modifier.m3placeholder(
                            true,
                            highlight = PlaceholderHighlight.shimmer()
                        ),
                        tonalElevation = 4.dp,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Otaku",
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 4.dp)
                                )
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                ) { Icon(Icons.Default.ChevronRight, null) }
                            }
                            LazyRow {
                                items(3) {
                                    M3PlaceHolderCoverCard(placeHolder = painterLogo())
                                }
                            }
                        }
                    }
                }
            } else if (viewModel.searchListPublisher.isNotEmpty()) {
                items(viewModel.searchListPublisher) { i ->
                    Surface(
                        tonalElevation = 4.dp,
                        shape = MaterialTheme.shapes.medium,
                        onClick = {
                            onSearchModel(i)
                            scope.launch { bottomScaffold.bottomSheetState.expand() }
                        }
                    ) {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    i.apiName,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 4.dp)
                                )
                                IconButton(
                                    onClick = {
                                        onSearchModel(i)
                                        scope.launch { bottomScaffold.bottomSheetState.expand() }
                                    },
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                ) {
                                    Row {
                                        Text(i.data.size.toString())
                                        Icon(Icons.Default.ChevronRight, null)
                                    }
                                }
                            }

                            val carouselState = rememberCarouselState { i.data.size }

                            HorizontalMultiBrowseCarousel(
                                state = carouselState,
                                preferredItemWidth = ComposableUtils.IMAGE_WIDTH,
                                flingBehavior = CarouselDefaults.multiBrowseFlingBehavior(state = carouselState),
                                itemSpacing = 4.dp,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .padding(bottom = 4.dp)
                            ) {
                                i.data.getOrNull(it)?.let { m ->
                                    SearchCoverCard(
                                        model = m,
                                        onLongPress = { c -> onLongPress(m) }
                                    ) { scope.launch { biometrics.openIfNotIncognito(m) } }
                                }
                            }
                        }
                    }
                }
                if (viewModel.isSearching) {
                    item { CircularProgressIndicator() }
                }
            } else if (viewModel.searchListPublisher.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            null,
                            modifier = Modifier.size(50.dp)
                        )
                        Text("Search for something!")
                    }
                }
            }
        }
    }
}

@Composable
private fun NoNetwork(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            Icons.Default.CloudOff,
            null,
            modifier = Modifier.size(50.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
        )
        Text(
            stringResource(Res.string.you_re_offline),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun SearchCoverCard(
    model: KmpItemModel,
    onLongPress: (ComponentState) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    ElevatedCard(
        modifier = modifier
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            )
            .combineClickableWithIndication(onLongPress, onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ImageLoaderChoice(
                imageUrl = model.imageUrl,
                name = model.title,
                placeHolder = { painterLogo() },
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black
                            ),
                            startY = 50f
                        )
                    )
            ) {
                Text(
                    model.title,
                    style = MaterialTheme
                        .typography
                        .bodyLarge
                        .copy(textAlign = TextAlign.Center, color = Color.White),
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}