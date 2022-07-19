package com.programmersbox.uiviews.globalsearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.models.ItemModel
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.dispatchIoAndCatchList
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GlobalSearchViewModel(
    val info: GenericInfo,
    val dao: HistoryDao,
    initialSearch: String
) : ViewModel() {

    private val disposable: CompositeDisposable = CompositeDisposable()

    var searchText by mutableStateOf(initialSearch)
    var searchListPublisher by mutableStateOf<List<SearchModel>>(emptyList())
    var isRefreshing by mutableStateOf(false)

    init {
        if (initialSearch.isNotEmpty()) {
            searchForItems()
        }
    }

    fun searchForItems() {
        viewModelScope.launch {
            combine(
                info.searchList()
                    .fastMap { a ->
                        a
                            .searchSourceList(searchText, list = emptyList())
                            .dispatchIoAndCatchList()
                            .map { SearchModel(a.serviceName, it) }
                    }
            ) { it.filterIsInstance<SearchModel>().filter { s -> s.data.isNotEmpty() } }
                .onCompletion { isRefreshing = false }
                .onStart { isRefreshing = true }
                .onEach { searchListPublisher = it }
                .collect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }

}

data class SearchModel(val apiName: String, val data: List<ItemModel>)