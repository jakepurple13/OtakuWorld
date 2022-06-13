package com.programmersbox.uiviews

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMaxBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.sourcePublish
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.utils.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.contentColorFor as m3ContentColorFor

class AllViewModel(dao: ItemDao, context: Context? = null) : ViewModel() {

    val searchPublisher = BehaviorSubject.createDefault<List<ItemModel>>(emptyList())

    var isSearching by mutableStateOf(false)

    var isRefreshing by mutableStateOf(false)
    val sourceList = mutableStateListOf<ItemModel>()
    val favoriteList = mutableStateListOf<DbModel>()

    var count = 1

    private val disposable: CompositeDisposable = CompositeDisposable()
    private val itemListener = FirebaseDb.FirebaseListener()

    init {
        viewModelScope.launch {
            combine(
                itemListener.getAllShowsFlow(),
                dao.getAllFavoritesFlow()
            ) { f, d -> (f + d).groupBy(DbModel::url).map { it.value.fastMaxBy(DbModel::numChapters)!! } }
                .collect {
                    favoriteList.clear()
                    favoriteList.addAll(it)
                }
        }

        sourcePublish
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                count = 1
                sourceList.clear()
                sourceLoadCompose(context, it)
            }
            .addTo(disposable)
    }

    fun reset(context: Context?, sources: ApiService) {
        count = 1
        sourceList.clear()
        sourceLoadCompose(context, sources)
    }

    fun loadMore(context: Context?, sources: ApiService) {
        count++
        sourceLoadCompose(context, sources)
    }

    private fun sourceLoadCompose(context: Context?, sources: ApiService) {
        sources
            .getList(count)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { context?.showErrorToast() }
            .onErrorReturnItem(emptyList())
            .doOnSubscribe { isRefreshing = true }
            .subscribeBy {
                sourceList.addAll(it)
                isRefreshing = false
            }
            .addTo(disposable)
    }

    fun search(searchText: String) {
        sourcePublish.value
            ?.searchList(searchText, 1, sourceList)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.doOnSubscribe { isSearching = true }
            ?.onErrorReturnItem(sourceList)
            ?.subscribeBy {
                searchPublisher.onNext(it)
                isSearching = false
            }
            ?.addTo(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        itemListener.unregister()
        disposable.dispose()
    }

}

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun AllView(
    allVm: AllViewModel,// = viewModel(factory = factoryCreate { AllFragment.AllViewModel(dao, context) })
    logo: MainLogo,
    info: GenericInfo,
    navController: NavController
) {
    //TODO: MAYBE have an option to show or hide the All screen.
    // maybe if possible, have it show for certain sources that the user can choose
    val context = LocalContext.current

    val isConnected by ReactiveNetwork.observeInternetConnectivity()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeAsState(initial = true)

    val source by sourcePublish.subscribeAsState(initial = null)

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
    val topAppBarScrollState = rememberTopAppBarScrollState()
    val scrollBehaviorTop = remember { TopAppBarDefaults.pinnedScrollBehavior(topAppBarScrollState) }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorTop.nestedScrollConnection),
        topBar = {
            SmallTopAppBar(
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
            placeholder = logo.logoId
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
                                val searchList by allVm.searchPublisher.subscribeAsState(initial = emptyList())
                                var searchText by rememberSaveable { mutableStateOf("") }
                                val searchTopAppBarScrollState = rememberTopAppBarScrollState()
                                val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(searchTopAppBarScrollState) }
                                Scaffold(
                                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                                    topBar = {
                                        Column(
                                            modifier = Modifier
                                                .background(
                                                    TopAppBarDefaults.smallTopAppBarColors()
                                                        .containerColor(scrollBehavior.scrollFraction).value
                                                )
                                        ) {
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
                                                value = searchText,
                                                onValueChange = { searchText = it },
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
                                                        IconButton(onClick = { searchText = "" }) {
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
                                                    allVm.search(searchText)
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