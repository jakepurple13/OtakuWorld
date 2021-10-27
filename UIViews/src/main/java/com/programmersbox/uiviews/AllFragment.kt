package com.programmersbox.uiviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMaxBy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.sourcePublish
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.utils.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * A simple [Fragment] subclass.
 * Use the [AllFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AllFragment : BaseFragmentCompose() {

    private val disposable: CompositeDisposable = CompositeDisposable()
    private var count = 1

    private val info: GenericInfo by inject()

    private val searchPublisher = BehaviorSubject.createDefault<List<ItemModel>>(emptyList())

    private val sourceList = mutableStateListOf<ItemModel>()
    private val favoriteList = mutableStateListOf<DbModel>()

    private val dao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val itemListener = FirebaseDb.FirebaseListener()

    private val logo: MainLogo by inject()

    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext())
        .apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent { MdcTheme { AllView() } }
        }

    override fun viewCreated(view: View, savedInstanceState: Bundle?) {
        sourcePublish
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                count = 1
                sourceList.clear()
                searchPublisher.onNext(emptyList())
                sourceLoadCompose(it)
            }
            .addTo(disposable)

        Flowables.combineLatest(
            itemListener.getAllShowsFlowable(),
            dao.getAllFavorites()
        ) { f, d -> (f + d).groupBy(DbModel::url).map { it.value.fastMaxBy(DbModel::numChapters)!! } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                favoriteList.clear()
                favoriteList.addAll(it)
            }
            .addTo(disposable)
    }

    private fun sourceLoadCompose(sources: ApiService, page: Int = 1, refreshState: SwipeRefreshState? = null) {
        sources
            .getList(page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { context?.showErrorToast() }
            .onErrorReturnItem(emptyList())
            .doOnSubscribe { refreshState?.isRefreshing = true }
            .subscribeBy {
                sourceList.addAll(it)
                refreshState?.isRefreshing = false
            }
            .addTo(disposable)
    }

    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    @Composable
    private fun AllView() {
        val isConnected by ReactiveNetwork.observeInternetConnectivity()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAsState(initial = true)

        val scaffoldState = rememberBottomSheetScaffoldState()
        val scope = rememberCoroutineScope()

        BackHandler(scaffoldState.bottomSheetState.isExpanded && isConnected && currentScreen.value == R.id.all_nav) {
            scope.launch { scaffoldState.bottomSheetState.collapse() }
        }

        when {
            !isConnected -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
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
            else -> {
                val state = rememberLazyListState()
                val refresh = rememberSwipeRefreshState(isRefreshing = false)
                val source by sourcePublish.subscribeAsState(initial = null)
                val focusManager = LocalFocusManager.current
                val searchList by searchPublisher.subscribeAsState(initial = emptyList())
                var searchText by rememberSaveable { mutableStateOf("") }
                val showButton by remember { derivedStateOf { state.firstVisibleItemIndex > 0 } }
                var showBanner by remember { mutableStateOf(false) }
                OtakuBannerBox(
                    showBanner = showBanner,
                    placeholder = logo.logoId
                ) { itemInfo ->
                    BottomSheetScaffold(
                        scaffoldState = scaffoldState,
                        sheetPeekHeight = ButtonDefaults.MinHeight + 4.dp,
                        sheetContent = {
                            var isSearching by remember { mutableStateOf(false) }
                            Scaffold(
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
                                        ) {
                                            Text(
                                                stringResource(R.string.search),
                                                style = MaterialTheme.typography.button
                                            )
                                        }

                                        OutlinedTextField(
                                            value = searchText,
                                            onValueChange = { searchText = it },
                                            label = { Text(stringResource(R.string.searchFor, source?.serviceName.orEmpty())) },
                                            trailingIcon = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(searchList.size.toString())
                                                    IconButton(onClick = { searchText = "" }) { Icon(Icons.Default.Cancel, null) }
                                                }
                                            },
                                            modifier = Modifier
                                                .padding(5.dp)
                                                .fillMaxWidth(),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(onSearch = {
                                                focusManager.clearFocus()
                                                sourcePublish.value!!.searchList(searchText, 1, sourceList)
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .doOnSubscribe { isSearching = true }
                                                    .onErrorReturnItem(sourceList)
                                                    .subscribeBy {
                                                        searchPublisher.onNext(it)
                                                        isSearching = false
                                                    }
                                                    .addTo(disposable)
                                            })
                                        )
                                    }
                                }
                            ) { p ->
                                Box(modifier = Modifier.padding(p)) {
                                    SwipeRefresh(
                                        state = rememberSwipeRefreshState(isRefreshing = isSearching),
                                        onRefresh = {},
                                        swipeEnabled = false
                                    ) {
                                        info.ItemListView(
                                            list = searchList,
                                            listState = rememberLazyListState(),
                                            favorites = favoriteList,
                                            onLongPress = { item, c ->
                                                itemInfo.value = if (c == ComponentState.Pressed) item else null
                                                showBanner = c == ComponentState.Pressed
                                            }
                                        ) { findNavController().navigate(AllFragmentDirections.actionAllFragment2ToDetailsFragment3(it)) }
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            AnimatedVisibility(
                                visible = showButton && scaffoldState.bottomSheetState.isCollapsed,
                                enter = slideInVertically(initialOffsetY = { it / 2 }),
                                exit = slideOutVertically(targetOffsetY = { it / 2 })
                            ) {
                                FloatingActionButton(
                                    onClick = { scope.launch { state.animateScrollToItem(0) } },
                                    backgroundColor = MaterialTheme.colors.primary
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = null,
                                        modifier = Modifier.padding(5.dp),
                                    )
                                }
                            }
                        },
                        floatingActionButtonPosition = FabPosition.End
                    ) { p ->
                        SwipeRefresh(
                            modifier = Modifier.padding(p),
                            state = refresh,
                            onRefresh = {
                                source?.let {
                                    count = 1
                                    sourceList.clear()
                                    sourceLoadCompose(it, count, refresh)
                                }
                            }
                        ) {
                            if (sourceList.isEmpty()) {
                                info.ComposeShimmerItem()
                            } else {
                                info.ItemListView(
                                    list = sourceList,
                                    listState = state,
                                    favorites = favoriteList,
                                    onLongPress = { item, c ->
                                        itemInfo.value = if (c == ComponentState.Pressed) item else null
                                        showBanner = c == ComponentState.Pressed
                                    }
                                ) { findNavController().navigate(AllFragmentDirections.actionAllFragment2ToDetailsFragment3(it)) }
                            }
                        }

                        if (source?.canScrollAll == true) {
                            InfiniteListHandler(listState = state, buffer = 1) {
                                source?.let {
                                    count++
                                    sourceLoadCompose(it, count, refresh)
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
        itemListener.unregister()
    }

    companion object {
        @JvmStatic
        fun newInstance() = AllFragment()
    }
}