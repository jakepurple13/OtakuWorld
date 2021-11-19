package com.programmersbox.uiviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMaxBy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
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
import org.koin.android.ext.android.inject
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

/**
 * A simple [Fragment] subclass.
 * Use the [RecentFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RecentFragment : BaseFragmentCompose() {

    private val info: GenericInfo by inject()

    private val disposable: CompositeDisposable = CompositeDisposable()
    private val sourceList = mutableStateListOf<ItemModel>()
    private val favoriteList = mutableStateListOf<DbModel>()

    private var count = 1

    private val dao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val itemListener = FirebaseDb.FirebaseListener()

    private val logo: MainLogo by inject()

    companion object {
        @JvmStatic
        fun newInstance() = RecentFragment()
    }

    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext())
        .apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent { M3MaterialTheme(currentColorScheme) { RecentView() } }
        }

    override fun viewCreated(view: View, savedInstanceState: Bundle?) {
        sourcePublish
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                count = 1
                sourceList.clear()
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

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
        itemListener.unregister()
    }

    private fun sourceLoadCompose(sources: ApiService, page: Int = 1, refreshState: SwipeRefreshState? = null) {
        sources
            .getRecent(page)
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

    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    @Composable
    private fun RecentView() {
        val state = rememberLazyListState()
        val source by sourcePublish.subscribeAsState(initial = null)
        val refresh = rememberSwipeRefreshState(isRefreshing = false)

        val isConnected by ReactiveNetwork.observeInternetConnectivity()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAsState(initial = true)

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
                        colorFilter = ColorFilter.tint(M3MaterialTheme.colorScheme.onBackground)
                    )
                    Text(stringResource(R.string.you_re_offline), style = M3MaterialTheme.typography.titleLarge)
                }
            }
            sourceList.isEmpty() -> info.ComposeShimmerItem()
            else -> {
                SwipeRefresh(
                    state = refresh,
                    onRefresh = {
                        source?.let {
                            count = 1
                            sourceList.clear()
                            sourceLoadCompose(it, count, refresh)
                        }
                    }
                ) {
                    var showBanner by remember { mutableStateOf(false) }
                    M3OtakuBannerBox(
                        showBanner = showBanner,
                        placeholder = logo.logoId
                    ) { itemInfo ->
                        info.ItemListView(
                            list = sourceList,
                            listState = state,
                            favorites = favoriteList,
                            onLongPress = { item, c ->
                                itemInfo.value = if (c == ComponentState.Pressed) item else null
                                showBanner = c == ComponentState.Pressed
                            }
                        ) { findNavController().navigate(RecentFragmentDirections.actionRecentFragment2ToDetailsFragment2(it)) }
                    }
                }

                if (source?.canScroll == true && sourceList.isNotEmpty()) {
                    InfiniteListHandler(listState = state, buffer = 2) {
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