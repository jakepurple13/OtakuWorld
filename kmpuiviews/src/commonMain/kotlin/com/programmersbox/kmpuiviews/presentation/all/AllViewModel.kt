package com.programmersbox.kmpuiviews.presentation.all

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.createConnectivity
import com.programmersbox.kmpuiviews.repository.CurrentSourceRepository
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.utils.dispatchIoAndCatchList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlin.coroutines.cancellation.CancellationException

class AllViewModel(
    dao: ItemDao,
    private val currentSourceRepository: CurrentSourceRepository,
    favoritesRepository: FavoritesRepository,
) : ViewModel() {

    val observeNetwork = createConnectivity()
        .statusUpdates
        .map { it.isConnected }
        .flowOn(Dispatchers.IO)

    var searchText by mutableStateOf("")
    var searchList by mutableStateOf<List<KmpItemModel>>(emptyList())

    var isSearching by mutableStateOf(false)

    var isRefreshing by mutableStateOf(false)
    val sourceList = mutableStateListOf<KmpItemModel>()
    val favoriteList = mutableStateListOf<DbModel>()

    var count = 1

    /** True when the last page load failed, so reconnecting should retry it. */
    var lastLoadFailed = false
        private set

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        favoritesRepository
            .getAllFavoritesFlow()
            .onEach {
                favoriteList.clear()
                favoriteList.addAll(it)
            }
            .launchIn(viewModelScope)

        currentSourceRepository.asFlow()
            .filterNotNull()
            .onEach {
                count = 1
                sourceList.clear()
                sourceLoadCompose(it)
            }
            .launchIn(viewModelScope)
    }

    fun reset(sources: KmpApiService) {
        count = 1
        sourceList.clear()
        sourceLoadCompose(sources)
    }

    fun loadMore(sources: KmpApiService) {
        if (loadJob?.isActive == true) return
        count++
        sourceLoadCompose(sources)
    }

    private fun sourceLoadCompose(sources: KmpApiService) {
        loadJob?.cancel()
        loadJob = sources
            .getListFlow(count)
            .dispatchIoAndCatchList { lastLoadFailed = true }
            .onStart {
                lastLoadFailed = false
                isRefreshing = true
            }
            .onEach { sourceList.addAll(it) }
            // A cancelled load was replaced by a newer one, which owns the refreshing flag
            .onCompletion { if (it !is CancellationException) isRefreshing = false }
            .launchIn(viewModelScope)
    }

    fun search() {
        searchJob?.cancel()
        searchJob = flow {
            val source = currentSourceRepository.asFlow().filterNotNull().first()
            emitAll(source.searchSourceList(searchText, 1, sourceList))
        }
            .onStart { isSearching = true }
            .onEach {
                searchList = it
                isSearching = false
            }
            .onCompletion { if (it !is CancellationException) isSearching = false }
            .launchIn(viewModelScope)
    }
}