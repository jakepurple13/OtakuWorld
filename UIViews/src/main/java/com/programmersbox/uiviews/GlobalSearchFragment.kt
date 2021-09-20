package com.programmersbox.uiviews

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.accompanist.drawablepainter.rememberDrawablePainter
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class GlobalSearchFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = GlobalSearchFragment()
    }

    private val disposable: CompositeDisposable = CompositeDisposable()
    private val info: GenericInfo by inject()
    private val logo: NotificationLogo by inject()
    private val mainLogo: MainLogo by inject()
    private val searchListPublisher = mutableStateListOf<SearchModel>()
    private val dao by lazy { HistoryDatabase.getInstance(requireContext()).historyDao() }
    private val args: GlobalSearchFragmentArgs by navArgs()

    data class SearchModel(val apiName: String, val data: List<ItemModel>)

    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                MdcTheme {

                    var searchText by rememberSaveable { mutableStateOf(args.searchFor) }
                    val focusManager = LocalFocusManager.current
                    val listState = rememberLazyListState()
                    val scope = rememberCoroutineScope()
                    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = false)

                    val networkState by ReactiveNetwork.observeInternetConnectivity()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeAsState(initial = true)

                    val history by dao
                        .searchHistory("%$searchText%")
                        .flowOn(Dispatchers.IO)
                        .flowWithLifecycle(lifecycle)
                        .collectAsState(emptyList())

                    LaunchedEffect(Unit) {
                        if (args.searchFor.isNotEmpty()) {
                            searchForItems(
                                searchText = args.searchFor,
                                onSubscribe = { swipeRefreshState.isRefreshing = true },
                                subscribe = { swipeRefreshState.isRefreshing = false }
                            )
                        }
                    }

                    CollapsingToolbarScaffold(
                        modifier = Modifier,
                        state = rememberCollapsingToolbarScaffoldState(),
                        scrollStrategy = ScrollStrategy.EnterAlwaysCollapsed,
                        toolbar = {
                            Column(modifier = Modifier.padding(5.dp)) {
                                AutoCompleteBox(
                                    items = history.asAutoCompleteEntities { _, _ -> true },
                                    itemContent = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = it.value.searchText,
                                                style = MaterialTheme.typography.subtitle2,
                                                modifier = Modifier
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                                    .weight(.9f)
                                            )
                                            IconButton(
                                                onClick = { scope.launch { dao.deleteHistory(it.value) } },
                                                modifier = Modifier.weight(.1f)
                                            ) { Icon(Icons.Default.Cancel, null) }
                                        }
                                    },
                                    content = {

                                        boxWidthPercentage = 1f
                                        boxBorderStroke = BorderStroke(2.dp, Color.Transparent)

                                        onItemSelected {
                                            searchText = it.value.searchText
                                            filter(searchText)
                                            focusManager.clearFocus()
                                            searchForItems(
                                                searchText = searchText,
                                                onSubscribe = { swipeRefreshState.isRefreshing = true },
                                                subscribe = { swipeRefreshState.isRefreshing = false }
                                            )
                                        }

                                        OutlinedTextField(
                                            value = searchText,
                                            onValueChange = {
                                                searchText = it
                                                filter(it)
                                            },
                                            label = { Text(stringResource(id = R.string.search)) },
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = {
                                                        searchText = ""
                                                        filter("")
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
                                                if (searchText.isNotEmpty()) {
                                                    lifecycleScope.launch(Dispatchers.IO) {
                                                        dao.insertHistory(HistoryItem(System.currentTimeMillis(), searchText))
                                                    }
                                                }
                                                searchForItems(
                                                    searchText = searchText,
                                                    onSubscribe = { swipeRefreshState.isRefreshing = true },
                                                    subscribe = { swipeRefreshState.isRefreshing = false }
                                                )
                                            })
                                        )
                                    }
                                )
                            }
                        }
                    ) {

                        val bottomScaffold = rememberBottomSheetScaffoldState()
                        var searchModelBottom by remember { mutableStateOf<SearchModel?>(null) }

                        BottomSheetScaffold(
                            scaffoldState = bottomScaffold,
                            sheetContent = searchModelBottom?.let { s ->
                                {
                                    Scaffold(
                                        topBar = {
                                            TopAppBar(
                                                title = { Text(s.apiName) },
                                                actions = { Text(stringResource(id = R.string.search_found, s.data.size)) }
                                            )
                                        }
                                    ) { p ->
                                        LazyVerticalGrid(
                                            cells = GridCells.Adaptive(ComposableUtils.IMAGE_WIDTH),
                                            contentPadding = p
                                        ) {
                                            items(s.data) { m ->
                                                SearchCoverCard(
                                                    model = m,
                                                    placeHolder = AppCompatResources.getDrawable(LocalContext.current, mainLogo.logoId)
                                                ) { findNavController().navigate(GlobalNavDirections.showDetails(m)) }
                                            }
                                        }
                                    }
                                }
                            } ?: {},
                            sheetPeekHeight = 0.dp,
                        ) {
                            if (networkState) {
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
                                            items(9) { PlaceHolderCoverCard(placeHolder = logo.notificationId) }
                                        } else if (searchListPublisher.isNotEmpty()) {
                                            items(searchListPublisher) { i ->
                                                Card(
                                                    onClick = {
                                                        searchModelBottom = i
                                                        scope.launch { bottomScaffold.bottomSheetState.expand() }
                                                    }
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
                                                            ) { Icon(Icons.Default.ChevronRight, null) }
                                                        }
                                                        LazyRow {
                                                            items(i.data) { m ->
                                                                SearchCoverCard(
                                                                    model = m,
                                                                    placeHolder = AppCompatResources.getDrawable(
                                                                        LocalContext.current,
                                                                        mainLogo.logoId
                                                                    )
                                                                ) { findNavController().navigate(GlobalNavDirections.showDetails(m)) }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                }
                            } else {
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
                                        colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground)
                                    )
                                    Text(stringResource(R.string.you_re_offline), style = MaterialTheme.typography.h5)
                                }
                            }
                        }
                    }
                }
            }
        }

    private fun searchForItems(searchText: String, onSubscribe: () -> Unit, subscribe: () -> Unit) {
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
        ) { (it as Array<SearchModel>).toList().filter { s -> s.data.isNotEmpty() } }
            .doOnSubscribe { onSubscribe() }
            .onErrorReturnItem(emptyList())
            .subscribe {
                searchListPublisher.clear()
                searchListPublisher.addAll(it)
                subscribe()
            }
            .addTo(disposable)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    @ExperimentalMaterialApi
    @Composable
    fun SearchCoverCard(model: ItemModel, placeHolder: Drawable?, error: Drawable? = placeHolder, onClick: () -> Unit = {}) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .padding(5.dp)
                .size(
                    ComposableUtils.IMAGE_WIDTH,
                    ComposableUtils.IMAGE_HEIGHT
                ),
            indication = rememberRipple(),
            onClickLabel = model.title,
        ) {
            Box {
                GlideImage(
                    imageModel = model.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    requestBuilder = Glide.with(LocalView.current)
                        .asDrawable()
                        //.override(360, 480)
                        .placeholder(placeHolder)
                        .error(error)
                        .fallback(placeHolder)
                        .transform(RoundedCorners(5)),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                    loading = {
                        Image(
                            painter = rememberDrawablePainter(drawable = placeHolder),
                            contentDescription = model.title,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                        )
                    },
                    failure = {
                        Image(
                            painter = rememberDrawablePainter(drawable = error),
                            contentDescription = model.title,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
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
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        model.title,
                        style = MaterialTheme
                            .typography
                            .body1
                            .copy(textAlign = TextAlign.Center, color = Color.White),
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
