package com.programmersbox.uiviews

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMaxBy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ItemDatabase
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

/**
 * A simple [Fragment] subclass.
 * Use the [RecentFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RecentFragment : BaseFragmentCompose() {

    companion object {
        @JvmStatic
        fun newInstance() = RecentFragment()
    }

    private val info: GenericInfo by inject()
    private val dao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val logo: MainLogo by inject()

    class RecentViewModel(dao: ItemDao, context: Context? = null) : ViewModel() {

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
                .getRecent(count)
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

        override fun onCleared() {
            super.onCleared()
            itemListener.unregister()
            disposable.dispose()
        }

    }

    @OptIn(
        ExperimentalMaterialApi::class,
        ExperimentalFoundationApi::class,
        ExperimentalMaterial3Api::class
    )
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext())
        .apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent { M3MaterialTheme(currentColorScheme) { RecentView() } }
        }

    override fun viewCreated(view: View, savedInstanceState: Bundle?) {

    }

    @ExperimentalMaterial3Api
    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    @Composable
    private fun RecentView(recentVm: RecentViewModel = viewModel(factory = factoryCreate { RecentViewModel(dao, context) })) {
        val context = LocalContext.current
        val state = rememberLazyGridState()
        val scope = rememberCoroutineScope()
        val source by sourcePublish.subscribeAsState(initial = null)
        val refresh = rememberSwipeRefreshState(isRefreshing = recentVm.isRefreshing)

        val isConnected by ReactiveNetwork.observeInternetConnectivity()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAsState(initial = true)

        LaunchedEffect(isConnected) {
            if (recentVm.sourceList.isEmpty() && source != null && isConnected && recentVm.count != 1) recentVm.reset(context, source!!)
        }

        val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }
        val showButton by remember { derivedStateOf { state.firstVisibleItemIndex > 0 } }
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                SmallTopAppBar(
                    title = { Text(stringResource(R.string.currentSource, source?.serviceName.orEmpty())) },
                    actions = {
                        AnimatedVisibility(visible = showButton) {
                            IconButton(onClick = { scope.launch { state.animateScrollToItem(0) } }) {
                                Icon(Icons.Default.ArrowUpward, null)
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { p ->
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
                                    .padding(p),
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
                            when {
                                recentVm.sourceList.isEmpty() -> info.ComposeShimmerItem()
                                else -> {
                                    SwipeRefresh(
                                        modifier = Modifier.padding(p),
                                        state = refresh,
                                        onRefresh = { source?.let { recentVm.reset(context, it) } }
                                    ) {
                                        info.ItemListView(
                                            list = recentVm.sourceList,
                                            listState = state,
                                            favorites = recentVm.favoriteList,
                                            onLongPress = { item, c ->
                                                itemInfo.value = if (c == ComponentState.Pressed) item else null
                                                showBanner = c == ComponentState.Pressed
                                            }
                                        ) { findNavController().navigate(RecentFragmentDirections.actionRecentFragment2ToDetailsFragment2(it)) }
                                    }

                                    if (source?.canScroll == true && recentVm.sourceList.isNotEmpty()) {
                                        InfiniteListHandler(listState = state, buffer = info.scrollBuffer) {
                                            source?.let { recentVm.loadMore(context, it) }
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