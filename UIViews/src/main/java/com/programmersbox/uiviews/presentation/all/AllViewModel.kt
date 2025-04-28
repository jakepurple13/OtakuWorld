package com.programmersbox.uiviews.presentation.all

import android.annotation.SuppressLint
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
import com.programmersbox.kmpuiviews.repository.CurrentSourceRepository
import com.programmersbox.uiviews.repository.FavoritesRepository
import com.programmersbox.uiviews.utils.DefaultToastItems
import com.programmersbox.uiviews.utils.ToastItems
import com.programmersbox.uiviews.utils.dispatchIoAndCatchList
import com.programmersbox.uiviews.utils.fireListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import ru.beryukhov.reactivenetwork.ReactiveNetwork

class AllViewModel(
    dao: ItemDao,
    private val currentSourceRepository: CurrentSourceRepository,
    favoritesRepository: FavoritesRepository,
) : ViewModel(), ToastItems by DefaultToastItems() {

    @SuppressLint("MissingPermission")
    val observeNetwork = ReactiveNetwork()
        .observeInternetConnectivity()
        .flowOn(Dispatchers.IO)

    var searchText by mutableStateOf("")
    var searchList by mutableStateOf<List<KmpItemModel>>(emptyList())

    var isSearching by mutableStateOf(false)

    var isRefreshing by mutableStateOf(false)
    val sourceList = mutableStateListOf<KmpItemModel>()
    var favoriteList = mutableStateListOf<DbModel>()

    var count = 1

    private val itemListener = fireListener()

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
            .dispatchIoAndCatchList { showError() }
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