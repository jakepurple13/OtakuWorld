package com.programmersbox.kmpuiviews.presentation.globalsearch

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Pages through one source's search results, opened from Global Search. */
class SourceSearchViewModel(
    handle: Screen.SourceSearchScreen,
    sourceRepository: SourceRepository,
    private val exceptionDao: ExceptionDao,
) : ViewModel() {

    val query = handle.query

    var source by mutableStateOf<KmpApiService?>(null)
        private set

    val pages = mutableStateListOf<SearchPage>()
    val itemCount by derivedStateOf { pages.sumOf { it.items.size } }

    var isLoading by mutableStateOf(false)
        private set
    var endReached by mutableStateOf(false)
        private set
    var error by mutableStateOf<Throwable?>(null)
        private set

    private val loadedUrls = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            // Sources load asynchronously, so a restored back stack may get here before this one exists.
            source = sourceRepository
                .sources
                .map { list -> list.find { it.apiService.serviceName == handle.serviceName }?.apiService }
                .filterNotNull()
                .first()
            loadMore()
        }
    }

    fun loadMore() {
        val s = source ?: return
        if (isLoading || endReached || error != null) return
        isLoading = true
        val next = pages.size + 1
        viewModelScope.launch {
            try {
                // searchListFlow rather than searchSourceList, which turns failures into an empty list
                val newItems = withContext(Dispatchers.IO) {
                    s.searchListFlow(query, next, emptyList()).firstOrNull().orEmpty()
                }
                    .distinctBy { it.url }
                    .filter { loadedUrls.add(it.url) }
                if (newItems.isEmpty()) endReached = true
                else pages += SearchPage(next, newItems)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e
                exceptionDao.insertException(e)
            } finally {
                isLoading = false
            }
        }
    }

    fun retry() {
        error = null
        loadMore()
    }
}

data class SearchPage(val number: Int, val items: List<KmpItemModel>)
