package com.programmersbox.kmpuiviews.presentation.all

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.createConnectivity
import com.programmersbox.kmpuiviews.repository.CurrentSourceRepository
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import com.programmersbox.kmpuiviews.utils.dispatchIoAndCatchList
import com.programmersbox.kmpuiviews.utils.fireListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

class AllViewModel(
    dao: ItemDao,
    private val currentSourceRepository: CurrentSourceRepository,
    favoritesRepository: FavoritesRepository,
    firebaseListenerImpl: KmpFirebaseConnection.KmpFirebaseListener,
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
    var favoriteList = mutableStateListOf<DbModel>()

    var count = 1

    private val itemListener = fireListener(itemListener = firebaseListenerImpl)

    init {
        favoritesRepository
            .getAllFavorites(itemListener)
            .onEach { favoriteList = it.toMutableStateList() }
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
        count++
        sourceLoadCompose(sources)
    }

    private fun sourceLoadCompose(sources: KmpApiService) {
        sources
            .getListFlow(count)
            .dispatchIoAndCatchList()
            .onStart { isRefreshing = true }
            .onEach { sourceList.addAll(it) }
            .onCompletion { isRefreshing = false }
            .launchIn(viewModelScope)
    }

    fun search() {
        currentSourceRepository.asFlow()
            .filterNotNull()
            .flatMapMerge { it.searchSourceList(searchText, 1, sourceList) }
            .onStart { isSearching = true }
            .onEach {
                searchList = it
                isSearching = false
            }
            .onCompletion { isSearching = false }
            .launchIn(viewModelScope)
    }
}