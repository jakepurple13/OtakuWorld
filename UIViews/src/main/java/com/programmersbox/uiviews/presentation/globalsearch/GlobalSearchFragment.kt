package com.programmersbox.uiviews.presentation.globalsearch

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
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
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryItem
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.components.DynamicSearchBar
import com.programmersbox.uiviews.presentation.components.LimitedBottomSheetScaffold
import com.programmersbox.uiviews.presentation.components.LimitedBottomSheetScaffoldDefaults
import com.programmersbox.uiviews.presentation.components.NormalOtakuScaffold
import com.programmersbox.uiviews.presentation.components.OtakuPullToRefreshBox
import com.programmersbox.uiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.uiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.uiviews.presentation.components.placeholder.shimmer
import com.programmersbox.uiviews.presentation.components.plus
import com.programmersbox.uiviews.presentation.navigateToDetails
import com.programmersbox.uiviews.theme.LocalHistoryDao
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.BannerScope
import com.programmersbox.uiviews.utils.ComponentState
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalNavHostPadding
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.M3PlaceHolderCoverCard
import com.programmersbox.uiviews.utils.MockApiService
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.OtakuBannerBox
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.adaptiveGridCell
import com.programmersbox.uiviews.utils.combineClickableWithIndication
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
fun GlobalSearchView(
    notificationLogo: NotificationLogo,
    isHorizontal: Boolean,
    dao: HistoryDao = LocalHistoryDao.current,
    viewModel: GlobalSearchViewModel = koinViewModel(),
) {
    val hazeState = remember { HazeState() }
    val showBlur by LocalSettingsHandling.current.rememberShowBlur()
    val navController = LocalNavController.current

    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullToRefreshState()
    val mainLogoDrawable = koinInject<AppLogo>()

    val networkState by viewModel.observeNetwork.collectAsStateWithLifecycle(true)

    val history by dao
        .searchHistory("%${viewModel.searchText}%")
        .collectAsStateWithLifecycle(emptyList())

    var showBanner by remember { mutableStateOf(false) }

    OtakuBannerBox(
        showBanner = showBanner,
        placeholder = mainLogoDrawable.logoId,
        modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        val bottomScaffold = rememberBottomSheetScaffoldState()
        var searchModelBottom by remember { mutableStateOf<SearchModel?>(null) }

        BackHandler(bottomScaffold.bottomSheetState.currentValue == SheetValue.Expanded) {
            scope.launch {
                try {
                    bottomScaffold.bottomSheetState.partialExpand()
                } catch (e: Exception) {
                    navController.popBackStack()
                }
            }
        }

        LimitedBottomSheetScaffold(
            scaffoldState = bottomScaffold,
            topBar = {
                var active by rememberSaveable { mutableStateOf(false) }

                fun closeSearchBar() {
                    focusManager.clearFocus()
                    active = false
                }
                Column {
                    DynamicSearchBar(
                        query = viewModel.searchText,
                        onQueryChange = { viewModel.searchText = it },
                        isDocked = isHorizontal,
                        onSearch = {
                            closeSearchBar()
                            if (viewModel.searchText.isNotEmpty()) {
                                scope.launch(Dispatchers.IO) {
                                    dao.insertHistory(HistoryItem(System.currentTimeMillis(), viewModel.searchText))
                                }
                            }
                            viewModel.searchForItems()
                        },
                        active = active,
                        onActiveChange = {
                            active = it
                            if (!active) focusManager.clearFocus()
                        },
                        placeholder = { Text(stringResource(R.string.global_search)) },
                        leadingIcon = { BackButton() },
                        trailingIcon = {
                            AnimatedVisibility(viewModel.searchText.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchText = "" }) {
                                    Icon(Icons.Default.Cancel, null)
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
                                    it.hazeChild(
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
                                ListItem(
                                    headlineContent = { Text(historyModel.searchText) },
                                    leadingContent = { Icon(Icons.Filled.Search, contentDescription = null) },
                                    trailingContent = {
                                        IconButton(
                                            onClick = { scope.launch { dao.deleteHistory(historyModel) } },
                                        ) { Icon(Icons.Default.Cancel, null) }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.searchText = historyModel.searchText
                                            closeSearchBar()
                                            if (viewModel.searchText.isNotEmpty()) {
                                                scope.launch(Dispatchers.IO) {
                                                    dao.insertHistory(HistoryItem(System.currentTimeMillis(), viewModel.searchText))
                                                }
                                            }
                                            viewModel.searchForItems()
                                        }
                                )
                                if (index != history.lastIndex) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                    HorizontalDivider()
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
                                actions = { Text(stringResource(id = R.string.search_found, s.data.size)) },
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
                                    placeHolder = mainLogoDrawable.logo,
                                    onLongPress = { c ->
                                        newItemModel(if (c == ComponentState.Pressed) m else null)
                                        showBanner = c == ComponentState.Pressed
                                    }
                                ) { navController.navigateToDetails(m) }
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
                        notificationLogo = notificationLogo,
                        mainLogoDrawable = mainLogoDrawable,
                        onSearchModel = { searchModelBottom = it },
                        onShowBanner = { showBanner = it },
                        bottomScaffold = bottomScaffold,
                    )
                } else {
                    NoNetwork(padding)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BannerScope.Content(
    viewModel: GlobalSearchViewModel,
    pullRefreshState: PullToRefreshState,
    padding: PaddingValues,
    listState: LazyListState,
    hazeState: HazeState,
    showBlur: Boolean,
    scope: CoroutineScope,
    navController: NavController,
    notificationLogo: NotificationLogo,
    mainLogoDrawable: AppLogo,
    onSearchModel: (SearchModel) -> Unit,
    onShowBanner: (Boolean) -> Unit,
    bottomScaffold: BottomSheetScaffoldState,
) {
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
                        it.haze(hazeState)
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
                        shape = M3MaterialTheme.shapes.medium
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
                            LazyRow { items(3) { M3PlaceHolderCoverCard(placeHolder = notificationLogo.notificationId) } }
                        }
                    }
                }
            } else if (viewModel.searchListPublisher.isNotEmpty()) {
                items(viewModel.searchListPublisher) { i ->
                    Surface(
                        tonalElevation = 4.dp,
                        shape = M3MaterialTheme.shapes.medium,
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
                                        placeHolder = mainLogoDrawable.logo,
                                        onLongPress = { c ->
                                            newItemModel(if (c == ComponentState.Pressed) m else null)
                                            onShowBanner(c == ComponentState.Pressed)
                                        }
                                    ) { navController.navigateToDetails(m) }
                                }
                            }
                        }
                    }
                }
                if (viewModel.isSearching) {
                    item { CircularProgressIndicator() }
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
            colorFilter = ColorFilter.tint(M3MaterialTheme.colorScheme.onBackground)
        )
        Text(
            stringResource(R.string.you_re_offline),
            style = M3MaterialTheme.typography.titleLarge,
            color = M3MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SearchCoverCard(
    model: ItemModel,
    placeHolder: Drawable?,
    onLongPress: (ComponentState) -> Unit,
    modifier: Modifier = Modifier,
    error: Drawable? = placeHolder,
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
            GlideImage(
                model = model.imageUrl,
                contentDescription = model.title,
                transition = CrossFade,
                contentScale = ContentScale.FillBounds,
                loading = placeholder(placeHolder),
                failure = placeholder(error),
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
                    style = M3MaterialTheme
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

@LightAndDarkPreviews
@Composable
private fun GlobalScreenPreview() {
    PreviewTheme {
        val dao = LocalHistoryDao.current
        GlobalSearchView(
            notificationLogo = NotificationLogo(R.drawable.ic_site_settings),
            dao = dao,
            isHorizontal = false,
            viewModel = viewModel {
                GlobalSearchViewModel(
                    sourceRepository = SourceRepository(),
                    savedStateHandle = createSavedStateHandle(),
                    dao = dao,
                )
            }
        )
    }
}

@LightAndDarkPreviews
@Composable
private fun GlobalSearchCoverPreview() {
    PreviewTheme {
        SearchCoverCard(
            model = ItemModel(
                title = "Title",
                description = "Description",
                url = "url",
                imageUrl = "imageUrl",
                source = MockApiService
            ),
            placeHolder = null,
            onLongPress = {}
        )
    }
}