package com.programmersbox.uiviews.all

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.util.fastMaxBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokar.sonner.ToasterState
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.CurrentSourceRepository
import com.programmersbox.uiviews.utils.dispatchIoAndCatchList
import com.programmersbox.uiviews.utils.showErrorToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
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
) : ViewModel() {

    val toastState = ToasterState(viewModelScope)

    val observeNetwork = ReactiveNetwork()
        .observeInternetConnectivity()
        .flowOn(Dispatchers.IO)

    var searchText by mutableStateOf("")
    var searchList by mutableStateOf<List<ItemModel>>(emptyList())

    var isSearching by mutableStateOf(false)

    var isRefreshing by mutableStateOf(false)
    val sourceList = mutableStateListOf<ItemModel>()
    var favoriteList = mutableStateListOf<DbModel>()

    var count = 1

    private val itemListener = FirebaseDb.FirebaseListener()

    init {
        combine(
            itemListener.getAllShowsFlow(),
            dao.getAllFavorites()
        ) { f, d -> (f + d).groupBy(DbModel::url).map { it.value.fastMaxBy(DbModel::numChapters)!! } }
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

    fun reset(sources: ApiService) {
        count = 1
        sourceList.clear()
        sourceLoadCompose(sources)
    }

    fun loadMore(sources: ApiService) {
        count++
        sourceLoadCompose(sources)
    }

    private fun sourceLoadCompose(sources: ApiService) {
        sources
            .getListFlow(count)
            .dispatchIoAndCatchList { toastState.showErrorToast() }
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

    override fun onCleared() {
        super.onCleared()
        itemListener.unregister()
    }

}