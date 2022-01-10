package com.programmersbox.uiviews

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.HistoryItem
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.utils.*
import com.skydoves.landscapist.glide.GlideImage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.contentColorFor as m3ContentColorFor

class GlobalSearchFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = GlobalSearchFragment()
    }

    private val disposable: CompositeDisposable = CompositeDisposable()
    private val info: GenericInfo by inject()
    private val logo: NotificationLogo by inject()
    private val mainLogo: MainLogo by inject()
    private val dao by lazy { HistoryDatabase.getInstance(requireContext()).historyDao() }
    private val args: GlobalSearchFragmentArgs by navArgs()

    class GlobalSearchViewModel(
        val info: GenericInfo,
        initialSearch: String,
        disposable: CompositeDisposable,
        onSubscribe: () -> Unit,
        subscribe: () -> Unit
    ) : ViewModel() {

        var searchText by mutableStateOf(initialSearch)
        val searchListPublisher = mutableStateListOf<SearchModel>()

        init {
            if (initialSearch.isNotEmpty()) {
                searchForItems(
                    disposable = disposable,
                    onSubscribe = onSubscribe,
                    subscribe = subscribe
                )
            }
        }

        fun searchForItems(
            disposable: CompositeDisposable,
            onSubscribe: () -> Unit,
            subscribe: () -> Unit
        ) {
            Observable.combineLatest(
                info.searchList()
                    .fastMap { a ->
                        a
                            .searchList(searchText, list = emptyList())
                            .timeout(5, TimeUnit.SECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .onErrorReturnItem(emptyList())
                            .map { SearchModel(a.serviceName, it) }
                            .toObservable()
                    }
            ) { it.filterIsInstance<SearchModel>().filter { s -> s.data.isNotEmpty() } }
                .doOnSubscribe {
                    searchListPublisher.clear()
                    onSubscribe()
                }
                .onErrorReturnItem(emptyList())
                .subscribe {
                    searchListPublisher.addAll(it)
                    subscribe()
                }
                .addTo(disposable)
        }

    }

    data class SearchModel(val apiName: String, val data: List<ItemModel>)

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class
    )
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                M3MaterialTheme(currentColorScheme) {
                    var isRefreshing by remember { mutableStateOf(false) }
                    val focusManager = LocalFocusManager.current
                    val listState = rememberLazyListState()
                    val scope = rememberCoroutineScope()
                    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)
                    val context = LocalContext.current
                    val mainLogo = remember { AppCompatResources.getDrawable(context, mainLogo.logoId) }

                    val networkState by ReactiveNetwork.observeInternetConnectivity()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeAsState(initial = true)

                    val viewModel: GlobalSearchViewModel = viewModel(
                        factory = factoryCreate {
                            GlobalSearchViewModel(
                                info = info,
                                initialSearch = args.searchFor,
                                disposable = disposable,
                                onSubscribe = { isRefreshing = true },
                                subscribe = { isRefreshing = false }
                            )
                        }
                    )

                    val history by dao
                        .searchHistory("%${viewModel.searchText}%")
                        .collectAsState(emptyList())

                    CollapsingToolbarScaffold(
                        modifier = Modifier,
                        state = rememberCollapsingToolbarScaffoldState(),
                        scrollStrategy = ScrollStrategy.EnterAlwaysCollapsed,
                        toolbar = {
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
                                        IconButton(onClick = { findNavController().popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
                                    },
                                    title = { Text(stringResource(R.string.global_search)) }
                                )
                                MdcTheme {
                                    AutoCompleteBox(
                                        items = history.asAutoCompleteEntities { _, _ -> true },
                                        itemContent = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                androidx.compose.material.Text(
                                                    text = it.value.searchText,
                                                    style = MaterialTheme.typography.subtitle2,
                                                    modifier = Modifier
                                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                                        .weight(.9f)
                                                )
                                                androidx.compose.material.IconButton(
                                                    onClick = { scope.launch { dao.deleteHistory(it.value) } },
                                                    modifier = Modifier.weight(.1f)
                                                ) { androidx.compose.material.Icon(Icons.Default.Cancel, null) }
                                            }
                                        },
                                        content = {

                                            boxWidthPercentage = 1f
                                            boxBorderStroke = BorderStroke(2.dp, Color.Transparent)

                                            onItemSelected {
                                                viewModel.searchText = it.value.searchText
                                                filter(viewModel.searchText)
                                                focusManager.clearFocus()
                                                viewModel.searchForItems(
                                                    disposable = disposable,
                                                    onSubscribe = { isRefreshing = true },
                                                    subscribe = { isRefreshing = false }
                                                )
                                            }

                                            OutlinedTextField(
                                                value = viewModel.searchText,
                                                onValueChange = {
                                                    viewModel.searchText = it
                                                    filter(it)
                                                },
                                                label = { androidx.compose.material.Text(stringResource(id = R.string.search)) },
                                                trailingIcon = {
                                                    androidx.compose.material.IconButton(
                                                        onClick = {
                                                            viewModel.searchText = ""
                                                            filter("")
                                                            viewModel.searchListPublisher.clear()
                                                        }
                                                    ) { androidx.compose.material.Icon(Icons.Default.Cancel, null) }
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
                                                        lifecycleScope.launch(Dispatchers.IO) {
                                                            dao.insertHistory(HistoryItem(System.currentTimeMillis(), viewModel.searchText))
                                                        }
                                                    }
                                                    viewModel.searchForItems(
                                                        disposable = disposable,
                                                        onSubscribe = { isRefreshing = true },
                                                        subscribe = { isRefreshing = false }
                                                    )
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
                            placeholder = this@GlobalSearchFragment.mainLogo.logoId
                        ) { itemInfo ->
                            val bottomScaffold = rememberBottomSheetScaffoldState()
                            var searchModelBottom by remember { mutableStateOf<SearchModel?>(null) }

                            BackHandler(bottomScaffold.bottomSheetState.isExpanded && findNavController().graph.id == currentScreen.value) {
                                scope.launch {
                                    try {
                                        bottomScaffold.bottomSheetState.collapse()
                                    } catch (e: Exception) {
                                        findNavController().popBackStack()
                                    }
                                }
                            }

                            BottomSheetScaffold(
                                backgroundColor = M3MaterialTheme.colorScheme.background,
                                contentColor = m3ContentColorFor(M3MaterialTheme.colorScheme.background),
                                scaffoldState = bottomScaffold,
                                sheetContent = searchModelBottom?.let { s ->
                                    {
                                        val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }
                                        Scaffold(
                                            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                                            topBar = {
                                                SmallTopAppBar(
                                                    scrollBehavior = scrollBehavior,
                                                    title = { Text(s.apiName) },
                                                    actions = { Text(stringResource(id = R.string.search_found, s.data.size)) }
                                                )
                                            }
                                        ) { p ->
                                            LazyVerticalGrid(
                                                cells = GridCells.Adaptive(ComposableUtils.IMAGE_WIDTH),
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                contentPadding = p
                                            ) {
                                                items(s.data) { m ->
                                                    SearchCoverCard(
                                                        model = m,
                                                        placeHolder = mainLogo,
                                                        onLongPress = { c ->
                                                            itemInfo.value = if (c == ComponentState.Pressed) m else null
                                                            showBanner = c == ComponentState.Pressed
                                                        }
                                                    ) { findNavController().navigate(GlobalNavDirections.showDetails(m)) }
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
                                                                    LazyRow { items(3) { PlaceHolderCoverCard(placeHolder = logo.notificationId) } }
                                                                }
                                                            }
                                                        }
                                                    } else if (viewModel.searchListPublisher.isNotEmpty()) {
                                                        items(viewModel.searchListPublisher) { i ->
                                                            Surface(
                                                                onClick = {
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
                                                                        modifier = Modifier.padding(horizontal = 4.dp)
                                                                    ) {
                                                                        items(i.data) { m ->
                                                                            SearchCoverCard(
                                                                                modifier = Modifier.padding(bottom = 4.dp),
                                                                                model = m,
                                                                                placeHolder = mainLogo,
                                                                                onLongPress = { c ->
                                                                                    itemInfo.value = if (c == ComponentState.Pressed) m else null
                                                                                    showBanner = c == ComponentState.Pressed
                                                                                }
                                                                            ) { findNavController().navigate(GlobalNavDirections.showDetails(m)) }
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
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

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
        Surface(
            modifier = Modifier
                .size(
                    ComposableUtils.IMAGE_WIDTH,
                    ComposableUtils.IMAGE_HEIGHT
                )
                .combineClickableWithIndication(onLongPress, onClick)
                .then(modifier),
            tonalElevation = 5.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GlideImage(
                    imageModel = model.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    loading = {
                        Image(
                            painter = rememberDrawablePainter(drawable = placeHolder),
                            contentDescription = model.title
                        )
                    },
                    failure = {
                        Image(
                            painter = rememberDrawablePainter(drawable = error),
                            contentDescription = model.title
                        )
                    }
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
}
