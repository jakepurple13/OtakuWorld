package com.programmersbox.uiviews.globalsearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.models.ItemModel
import com.programmersbox.uiviews.utils.dispatchIoAndCatchList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.beryukhov.reactivenetwork.ReactiveNetwork

class GlobalSearchViewModel(
    val sourceRepository: SourceRepository,
    val dao: HistoryDao,
    initialSearch: String
) : ViewModel() {

    val observeNetwork = ReactiveNetwork()
        .observeInternetConnectivity()
        .flowOn(Dispatchers.IO)

    var searchText by mutableStateOf(initialSearch)
    var searchListPublisher by mutableStateOf<List<SearchModel>>(emptyList())
    var isRefreshing by mutableStateOf(false)
    var isSearching by mutableStateOf(false)

    init {
        if (initialSearch.isNotEmpty()) {
            searchForItems()
        }
    }

    fun searchForItems() {
        viewModelScope.launch {
            // this populates dynamically
            isRefreshing = true
            isSearching = true
            searchListPublisher = emptyList()
            async {
                sourceRepository.list
                    .apmap { a ->
                        a
                            .apiService
                            .searchSourceList(searchText, list = emptyList())
                            .dispatchIoAndCatchList()
                            .map { SearchModel(a.apiService.serviceName, it) }
                            .filter { it.data.isNotEmpty() }
                            .onEach { searchListPublisher = searchListPublisher + it }
                            .onCompletion { isRefreshing = false }
                    }
                    .forEach { launch { it.collect() } }
                // this populates after it all finishes
                /*combine(
                info.searchList()
                        .apmap { a ->
                            a
                                .searchSourceList(searchText, list = emptyList())
                                .dispatchIoAndCatchList()
                                .map { SearchModel(a.serviceName, it) }
                        }
                ) { it.toList().filter { s -> s.data.isNotEmpty() } }
                    .onStart { isRefreshing = true }
                    .onCompletion { isRefreshing = false }
                    .onEach { searchListPublisher = it }
                    .collect()
                */
            }.await()
            isSearching = false
        }
    }

    private fun <A, B> List<A>.apmap(f: suspend (A) -> B): List<B> = runBlocking {
        map { async { f(it) } }.map { it.await() }
    }

}

data class SearchModel(val apiName: String, val data: List<ItemModel>)