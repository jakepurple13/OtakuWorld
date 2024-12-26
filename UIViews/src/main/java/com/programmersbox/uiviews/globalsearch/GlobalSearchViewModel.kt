package com.programmersbox.uiviews.globalsearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.models.ItemModel
import com.programmersbox.uiviews.utils.Screen
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
    savedStateHandle: SavedStateHandle,
    val sourceRepository: SourceRepository,
    val dao: HistoryDao,
) : ViewModel() {

    private val initialSearch: String = savedStateHandle.toRoute<Screen.GlobalSearchScreen>().title ?: ""

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
            //TODO: Add option to have live population or all at once
            async {
                sourceRepository.list
                    .apmap { a ->
                        a
                            .apiService
                            .searchSourceList(searchText, list = emptyList())
                            .dispatchIoAndCatchList()
                            .map { SearchModel(a.apiService.serviceName, it) }
                            .filter { it.data.isNotEmpty() }
                            .onEach { searchListPublisher += it }
                            .onCompletion { isRefreshing = false }
                    }
                    .forEach { launch { it.collect() } }

                // this populates after it all finishes
                /*val d = awaitAll(
                    *sourceRepository
                        .list
                        .mapNotNull { a ->
                            async {
                                a
                                    .apiService
                                    .searchSourceList(searchText, list = emptyList())
                                    .dispatchIoAndCatchList()
                                    .map { SearchModel(a.apiService.serviceName, it) }
                                    .filter { it.data.isNotEmpty() }
                                    .firstOrNull()
                            }
                        }
                        .toTypedArray()
                ).filterNotNull()
                searchListPublisher = d
                isRefreshing = false*/
            }.await()
            isSearching = false
        }
    }

    private fun <A, B> List<A>.apmap(f: suspend (A) -> B): List<B> = runBlocking {
        map { async { f(it) } }.map { it.await() }
    }

}

data class SearchModel(val apiName: String, val data: List<ItemModel>)