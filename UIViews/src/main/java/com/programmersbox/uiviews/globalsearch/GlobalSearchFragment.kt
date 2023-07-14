package com.programmersbox.uiviews.globalsearch

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.accompanist.placeholder.material.placeholder
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryItem
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.ComponentState
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalHistoryDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSourcesRepository
import com.programmersbox.uiviews.utils.M3PlaceHolderCoverCard
import com.programmersbox.uiviews.utils.MockApiService
import com.programmersbox.uiviews.utils.MockAppIcon
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.OtakuBannerBox
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.adaptiveGridCell
import com.programmersbox.uiviews.utils.combineClickableWithIndication
import com.programmersbox.uiviews.utils.navigateToDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.contentColorFor as m3ContentColorFor

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
)
@Composable
fun GlobalSearchView(
    mainLogo: MainLogo,
    notificationLogo: NotificationLogo,
    sourceRepository: SourceRepository = LocalSourcesRepository.current,
    dao: HistoryDao = LocalHistoryDao.current,
    viewModel: GlobalSearchViewModel = viewModel {
        GlobalSearchViewModel(
            sourceRepository = sourceRepository,
            initialSearch = createSavedStateHandle().get<String>("searchFor") ?: "",
            dao = dao,
        )
    }
) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullRefreshState(refreshing = viewModel.isRefreshing, onRefresh = {})
    val mainLogoDrawable = remember { AppCompatResources.getDrawable(context, mainLogo.logoId) }

    val networkState by viewModel.observeNetwork.collectAsState(initial = true)

    val history by dao
        .searchHistory("%${viewModel.searchText}%")
        .collectAsState(emptyList())

    var showBanner by remember { mutableStateOf(false) }

    OtakuBannerBox(
        showBanner = showBanner,
        placeholder = mainLogo.logoId,
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

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

        BottomSheetScaffold(
            scaffoldState = bottomScaffold,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                Column {
                    InsetSmallTopAppBar(
                        navigationIcon = { BackButton() },
                        title = { Text(stringResource(R.string.global_search)) },
                        scrollBehavior = scrollBehavior
                    )
                    var active by rememberSaveable { mutableStateOf(false) }

                    fun closeSearchBar() {
                        focusManager.clearFocus()
                        active = false
                    }
                    SearchBar(
                        windowInsets = WindowInsets(0.dp),
                        query = viewModel.searchText,
                        onQueryChange = { viewModel.searchText = it },
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
                        placeholder = { Text(stringResource(id = R.string.search)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.searchText = "" }) {
                                Icon(Icons.Default.Cancel, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
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
                                    Divider()
                                }
                            }
                        }
                    }
                }
            },
            sheetContent = searchModelBottom?.let { s ->
                {
                    val sheetScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
                    Scaffold(
                        modifier = Modifier.nestedScroll(sheetScrollBehavior.nestedScrollConnection),
                        topBar = {
                            TopAppBar(
                                scrollBehavior = sheetScrollBehavior,
                                title = { Text(s.apiName) },
                                actions = { Text(stringResource(id = R.string.search_found, s.data.size)) },
                                windowInsets = WindowInsets(0.dp)
                            )
                        }
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
                                    placeHolder = mainLogoDrawable,
                                    onLongPress = { c ->
                                        newItemModel(if (c == ComponentState.Pressed) m else null)
                                        showBanner = c == ComponentState.Pressed
                                    }
                                ) { Screen.GlobalSearchScreen.navigate(navController, m.title) }
                            }
                        }
                    }
                }
            } ?: {},
            sheetPeekHeight = 0.dp,
        ) { padding ->
            Crossfade(targetState = networkState, label = "") { network ->
                when (network) {
                    false -> {
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

                    true -> {
                        Box(
                            modifier = Modifier.pullRefresh(pullRefreshState, false)
                        ) {
                            LazyColumn(
                                state = listState,
                                contentPadding = padding,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 8.dp)
                            ) {
                                if (viewModel.isRefreshing) {
                                    items(3) {
                                        val placeholderColor =
                                            m3ContentColorFor(backgroundColor = M3MaterialTheme.colorScheme.surface)
                                                .copy(0.1f)
                                                .compositeOver(M3MaterialTheme.colorScheme.surface)
                                        Surface(
                                            modifier = Modifier.placeholder(true, color = placeholderColor),
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
                                            modifier = Modifier.clickable {
                                                searchModelBottom = i
                                                scope.launch { bottomScaffold.bottomSheetState.expand() }
                                            },
                                            tonalElevation = 4.dp,
                                            shape = M3MaterialTheme.shapes.medium
                                        ) {
                                            Column {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            searchModelBottom = i
                                                            scope.launch { bottomScaffold.bottomSheetState.expand() }
                                                        }
                                                ) {
                                                    Text(
                                                        i.apiName,
                                                        modifier = Modifier
                                                            .align(Alignment.CenterStart)
                                                            .padding(start = 4.dp)
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            searchModelBottom = i
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
                                                LazyRow(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier
                                                        .padding(horizontal = 4.dp)
                                                        .padding(bottom = 4.dp)
                                                ) {
                                                    items(i.data) { m ->
                                                        SearchCoverCard(
                                                            model = m,
                                                            placeHolder = mainLogoDrawable,
                                                            onLongPress = { c ->
                                                                newItemModel(if (c == ComponentState.Pressed) m else null)
                                                                showBanner = c == ComponentState.Pressed
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

                            PullRefreshIndicator(
                                refreshing = viewModel.isRefreshing,
                                state = pullRefreshState,
                                modifier = Modifier.align(Alignment.TopCenter),
                                backgroundColor = M3MaterialTheme.colorScheme.background,
                                contentColor = M3MaterialTheme.colorScheme.onBackground,
                                scale = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchCoverCard(
    model: ItemModel,
    placeHolder: Drawable?,
    onLongPress: (ComponentState) -> Unit,
    modifier: Modifier = Modifier,
    error: Drawable? = placeHolder,
    onClick: () -> Unit = {}
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
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(model.imageUrl)
                    .lifecycle(LocalLifecycleOwner.current)
                    .crossfade(true)
                    .build(),
                contentDescription = model.title,
                placeholder = rememberDrawablePainter(placeHolder),
                error = rememberDrawablePainter(error),
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
            mainLogo = MockAppIcon,
            notificationLogo = NotificationLogo(R.drawable.ic_site_settings),
            dao = dao,
            viewModel = viewModel {
                GlobalSearchViewModel(
                    sourceRepository = SourceRepository(),
                    initialSearch = "",
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