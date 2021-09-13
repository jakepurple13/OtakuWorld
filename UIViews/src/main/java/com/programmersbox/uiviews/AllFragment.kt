package com.programmersbox.uiviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.programmersbox.uiviews.utils.InfiniteListHandler
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

    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext())
        .apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent { AllView() }
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
            .doOnSubscribe { refreshState?.isRefreshing = true }
            .subscribeBy {
                sourceList.addAll(it)
                refreshState?.isRefreshing = false
            }
            .addTo(disposable)
    }

    //TODO: After converting this to compose, don't forget to go remove the item list adapters that aren't needed anymore

    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    @Composable
    private fun AllView() {
        MdcTheme {
            val state = rememberLazyListState()
            val source by sourcePublish.subscribeAsState(initial = null)
            val refresh = rememberSwipeRefreshState(isRefreshing = false)

            val isConnected by ReactiveNetwork.observeInternetConnectivity()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeAsState(initial = true)

            val focusManager = LocalFocusManager.current

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
                sourceList.isEmpty() -> info.ComposeShimmerItem()
                else -> {
                    val scaffoldState = rememberBottomSheetScaffoldState()
                    val scope = rememberCoroutineScope()
                    val searchList by searchPublisher.subscribeAsState(initial = emptyList())
                    var searchText by rememberSaveable { mutableStateOf("") }

                    BottomSheetScaffold(
                        scaffoldState = scaffoldState,
                        sheetPeekHeight = ButtonDefaults.MinHeight + 4.dp,
                        sheetContent = {
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
                                                    .onErrorReturnItem(sourceList)
                                                    .subscribe(searchPublisher::onNext)
                                                    .addTo(disposable)
                                            })
                                        )

                                    }
                                }
                            ) { p ->
                                Box(modifier = Modifier.padding(p)) {
                                    info.ItemListView(list = searchList, listState = rememberLazyListState(), favorites = favoriteList) {
                                        findNavController().navigate(AllFragmentDirections.actionAllFragment2ToDetailsFragment3(it))
                                    }
                                }
                            }
                        }
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
                            info.ItemListView(list = sourceList, listState = state, favorites = favoriteList) {
                                findNavController().navigate(AllFragmentDirections.actionAllFragment2ToDetailsFragment3(it))
                            }
                        }

                        if (source?.canScroll == true) {
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