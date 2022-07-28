package com.programmersbox.uiviews.globalsearch

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.HistoryItem
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.*
import com.programmersbox.uiviews.utils.components.AutoCompleteBox
import com.programmersbox.uiviews.utils.components.asAutoCompleteEntities
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.contentColorFor as m3ContentColorFor

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class
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
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = viewModel.isRefreshing)
    val mainLogoDrawable = remember { AppCompatResources.getDrawable(context, mainLogo.logoId) }

    val networkState by ReactiveNetwork.observeInternetConnectivity()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeAsState(initial = true)

    val history by dao
        .searchHistory("%${viewModel.searchText}%")
        .collectAsState(emptyList())

    CollapsingToolbarScaffold(
        modifier = Modifier,
        state = rememberCollapsingToolbarScaffoldState(),
        scrollStrategy = ScrollStrategy.EnterAlwaysCollapsed,
        toolbar = {
            Insets {
                Column(
                    modifier = Modifier
                        .background(
                            TopAppBarDefaults
                                .smallTopAppBarColors()
                                .containerColor(0f).value
                        )
                        .padding(5.dp)
                ) {
                    SmallTopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
                        },
                        title = { Text(stringResource(R.string.global_search)) }
                    )
                    AutoCompleteBox(
                        items = history.asAutoCompleteEntities { _, _ -> true },
                        trailingIcon = {
                            androidx.compose.material3.IconButton(
                                onClick = { scope.launch { dao.deleteHistory(it.value) } },
                                modifier = Modifier.weight(.1f)
                            ) { androidx.compose.material3.Icon(Icons.Default.Cancel, null) }
                        },
                        itemContent = {
                            androidx.compose.material3.Text(
                                text = it.value.searchText,
                                style = M3MaterialTheme.typography.titleSmall,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .weight(.9f)
                            )
                        },
                        content = {

                            boxWidthPercentage = 1f
                            boxBorderStroke = BorderStroke(2.dp, Color.Transparent)

                            onItemSelected {
                                viewModel.searchText = it.value.searchText
                                filter(viewModel.searchText)
                                focusManager.clearFocus()
                                viewModel.searchForItems()
                            }

                            androidx.compose.material3.OutlinedTextField(
                                value = viewModel.searchText,
                                onValueChange = {
                                    viewModel.searchText = it
                                    filter(it)
                                },
                                label = { Text(stringResource(id = R.string.search)) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            viewModel.searchText = ""
                                            filter("")
                                            viewModel.searchListPublisher = emptyList()
                                        }
                                    ) { Icon(Icons.Default.Cancel, null) }
                                },
                                modifier = Modifier
                                    .padding(5.dp)
                                    .fillMaxWidth()
                                    .onFocusChanged { isSearching = it.isFocused },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    focusManager.clearFocus()
                                    if (viewModel.searchText.isNotEmpty()) {
                                        scope.launch(Dispatchers.IO) {
                                            dao.insertHistory(HistoryItem(System.currentTimeMillis(), viewModel.searchText))
                                        }
                                    }
                                    viewModel.searchForItems()
                                })
                            )
                        }
                    )
                }
            }
        }
    ) {
        var showBanner by remember { mutableStateOf(false) }

        M3OtakuBannerBox(
            showBanner = showBanner,
            placeholder = mainLogo.logoId,
            modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues())
        ) { itemInfo ->
            val bottomScaffold = rememberBottomSheetScaffoldState()
            var searchModelBottom by remember { mutableStateOf<SearchModel?>(null) }

            BackHandler(bottomScaffold.bottomSheetState.isExpanded && navController.graph.id == currentScreen.value) {
                scope.launch {
                    try {
                        bottomScaffold.bottomSheetState.collapse()
                    } catch (e: Exception) {
                        navController.popBackStack()
                    }
                }
            }

            BottomSheetScaffold(
                backgroundColor = M3MaterialTheme.colorScheme.background,
                contentColor = m3ContentColorFor(M3MaterialTheme.colorScheme.background),
                scaffoldState = bottomScaffold,
                sheetContent = searchModelBottom?.let { s ->
                    {
                        val topAppBarScrollState = rememberTopAppBarState()
                        val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topAppBarScrollState) }
                        Scaffold(
                            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                            topBar = {
                                Insets {
                                    SmallTopAppBar(
                                        scrollBehavior = scrollBehavior,
                                        title = { Text(s.apiName) },
                                        actions = { Text(stringResource(id = R.string.search_found, s.data.size)) }
                                    )
                                }
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
            ) {
                Crossfade(targetState = networkState) { network ->
                    when (network) {
                        false -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(it),
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
                            SwipeRefresh(
                                state = swipeRefreshState,
                                onRefresh = {},
                                swipeEnabled = false,
                                modifier = Modifier.padding(it)
                            ) {
                                LazyColumn(
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (swipeRefreshState.isRefreshing) {
                                        items(3) {
                                            val placeholderColor =
                                                contentColorFor(backgroundColor = M3MaterialTheme.colorScheme.surface)
                                                    .copy(0.1f)
                                                    .compositeOver(M3MaterialTheme.colorScheme.surface)
                                            Surface(
                                                modifier = Modifier.placeholder(true, color = placeholderColor),
                                                tonalElevation = 5.dp,
                                                shape = MaterialTheme.shapes.medium
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
                                            androidx.compose.material3.Surface(
                                                modifier = Modifier.clickable {
                                                    searchModelBottom = i
                                                    scope.launch { bottomScaffold.bottomSheetState.expand() }
                                                },
                                                tonalElevation = 5.dp,
                                                shape = MaterialTheme.shapes.medium
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
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterialApi
@Composable
fun SearchCoverCard(
    modifier: Modifier = Modifier,
    model: ItemModel,
    placeHolder: Drawable?,
    error: Drawable? = placeHolder,
    onLongPress: (ComponentState) -> Unit,
    onClick: () -> Unit = {}
) {
    androidx.compose.material3.ElevatedCard(
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