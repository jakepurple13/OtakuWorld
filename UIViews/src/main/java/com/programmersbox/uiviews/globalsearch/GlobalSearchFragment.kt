package com.programmersbox.uiviews.globalsearch

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.HistoryItem
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.contentColorFor as m3ContentColorFor

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun GlobalSearchView(
    mainLogo: MainLogo,
    notificationLogo: NotificationLogo
) {
    val navController = LocalNavController.current
    val info = LocalGenericInfo.current
    val context = LocalContext.current

    val dao = remember { HistoryDatabase.getInstance(context).historyDao() }

    val viewModel: GlobalSearchViewModel = viewModel {
        GlobalSearchViewModel(
            info = info,
            initialSearch = createSavedStateHandle().get<String>("searchFor") ?: "",
            dao = dao,
        )
    }

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

    M3OtakuBannerBox(
        showBanner = showBanner,
        placeholder = mainLogo.logoId,
        modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues())
    ) { itemInfo ->
        val bottomScaffold = rememberBottomSheetScaffoldState()
        var searchModelBottom by remember { mutableStateOf<SearchModel?>(null) }

        BackHandler(bottomScaffold.bottomSheetState.isExpanded) {
            scope.launch {
                try {
                    bottomScaffold.bottomSheetState.collapse()
                } catch (e: Exception) {
                    navController.popBackStack()
                }
            }
        }

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

        BottomSheetScaffold(
            backgroundColor = M3MaterialTheme.colorScheme.background,
            contentColor = m3ContentColorFor(M3MaterialTheme.colorScheme.background),
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
                        modifier = Modifier.fillMaxWidth(),
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
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            history.take(5).forEachIndexed { index, historyModel ->
                                ListItem(
                                    headlineText = { Text(historyModel.searchText) },
                                    leadingContent = { Icon(Icons.Filled.Search, contentDescription = null) },
                                    trailingContent = {
                                        IconButton(
                                            onClick = { scope.launch { dao.deleteHistory(historyModel) } },
                                            modifier = Modifier.weight(.1f)
                                        ) { Icon(Icons.Default.Cancel, null) }
                                    },
                                    modifier = Modifier.clickable {
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
                                actions = { Text(stringResource(id = R.string.search_found, s.data.size)) }
                            )
                        }
                    ) { p ->
                        LazyVerticalGrid(
                            columns = adaptiveGridCell(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = p
                        ) {
                            items(s.data) { m ->
                                SearchCoverCard(
                                    model = m,
                                    placeHolder = mainLogoDrawable,
                                    onLongPress = { c ->
                                        itemInfo.value = if (c == ComponentState.Pressed) m else null
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
            Crossfade(targetState = networkState) { network ->
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
                                modifier = Modifier.size(50.dp, 50.dp),
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
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                if (viewModel.isRefreshing) {
                                    items(3) {
                                        val placeholderColor =
                                            m3ContentColorFor(backgroundColor = M3MaterialTheme.colorScheme.surface)
                                                .copy(0.1f)
                                                .compositeOver(M3MaterialTheme.colorScheme.surface)
                                        Surface(
                                            modifier = Modifier.placeholder(true, color = placeholderColor),
                                            tonalElevation = 5.dp,
                                            shape = androidx.compose.material3.MaterialTheme.shapes.medium
                                        ) {
                                            Column {
                                                Box(modifier = Modifier.fillMaxWidth()) {
                                                    Text(
                                                        "Otaku",
                                                        modifier = Modifier
                                                            .align(Alignment.CenterStart)
                                                            .padding(start = 5.dp)
                                                    )
                                                    IconButton(
                                                        onClick = {},
                                                        modifier = Modifier.align(Alignment.CenterEnd)
                                                    ) { Icon(Icons.Default.ChevronRight, null) }
                                                }
                                                LazyRow { items(3) { PlaceHolderCoverCard(placeHolder = notificationLogo.notificationId) } }
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
                                            tonalElevation = 5.dp,
                                            shape = androidx.compose.material3.MaterialTheme.shapes.medium
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
                                                            .padding(start = 5.dp)
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
                                                                itemInfo.value = if (c == ComponentState.Pressed) m else null
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
    modifier: Modifier = Modifier,
    model: ItemModel,
    placeHolder: Drawable?,
    error: Drawable? = placeHolder,
    onLongPress: (ComponentState) -> Unit,
    onClick: () -> Unit = {}
) {
    ElevatedCard(
        modifier = Modifier
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            )
            .combineClickableWithIndication(onLongPress, onClick)
            .then(modifier)
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
                    .size(ComposableUtils.IMAGE_WIDTH_PX, ComposableUtils.IMAGE_HEIGHT_PX)
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