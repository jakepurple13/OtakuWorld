package com.programmersbox.kmpuiviews.presentation.globalsearch

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.createConnectivity
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.utils.dispatchIoAndCatchList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class GlobalSearchViewModel(
    handle: Screen.GlobalSearchScreen,
    val sourceRepository: SourceRepository,
    val dao: HistoryDao,
) : ViewModel() {

    private val initialSearch: String = handle.title ?: ""

    val observeNetwork = createConnectivity()
        .statusUpdates
        .map { it.isConnected }
        .flowOn(Dispatchers.IO)

    var searchText by mutableStateOf(TextFieldState(initialSearch))
    var searchListPublisher by mutableStateOf<List<SearchModel>>(emptyList())
    var isRefreshing by mutableStateOf(false)
    var isSearching by mutableStateOf(false)

    private var searchJob: Job? = null

    init {
        if (initialSearch.isNotEmpty()) {
            searchForItems()
        }
    }

    fun searchForItems() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // this populates dynamically
            isRefreshing = true
            isSearching = true
            searchListPublisher = emptyList()
            //TODO: Add option to have live population or all at once
            searchFlow().collect()
            //searchAllFlow()
            isSearching = false
        }
    }

    private fun searchFlow() = channelFlow {
        sourceRepository
            .list
            .distinctBy { it.packageName }
            .map { a ->
                a
                    .apiService
                    .searchSourceList(searchText.text.toString(), list = emptyList())
                    .dispatchIoAndCatchList()
                    .map { SearchModel(a.apiService.serviceName, it) }
                    .filter { it.data.isNotEmpty() }
                    .onEach { searchListPublisher += it }
                    .onCompletion { isRefreshing = false }
            }
            .forEach { launch { send(it.firstOrNull()) } }
    }

    private suspend fun searchAllFlow() {
        // this populates after it all finishes
        val d = coroutineScope {
            sourceRepository
                .list
                .map { a ->
                    async {
                        a
                            .apiService
                            .searchSourceList(searchText.text.toString(), list = emptyList())
                            .dispatchIoAndCatchList()
                            .map { SearchModel(a.apiService.serviceName, it) }
                            .filter { it.data.isNotEmpty() }
                            .firstOrNull()
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
        searchListPublisher = d
        isRefreshing = false
    }

}

data class SearchModel(val apiName: String, val data: List<KmpItemModel>)