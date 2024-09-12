package com.programmersbox.uiviews.recent

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastMaxBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.SourceInformation
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.CurrentSourceRepository
import com.programmersbox.uiviews.utils.DefaultToastItems
import com.programmersbox.uiviews.utils.ToastItems
import com.programmersbox.uiviews.utils.dispatchIo
import com.programmersbox.uiviews.utils.recordFirebaseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import ru.beryukhov.reactivenetwork.ReactiveNetwork

class RecentViewModel(
    dao: ItemDao,
    sourceRepository: SourceRepository,
    currentSourceRepository: CurrentSourceRepository,
) : ViewModel(), ToastItems by DefaultToastItems() {

    var isRefreshing by mutableStateOf(false)
    private val sourceList = mutableStateListOf<ItemModel>()
    val favoriteList = mutableStateListOf<DbModel>()

    val filteredSourceList by derivedStateOf { sourceList.distinctBy { it.url } }

    val observeNetwork = ReactiveNetwork()
        .observeInternetConnectivity()
        .flowOn(Dispatchers.IO)

    var count = 1

    private val itemListener = FirebaseDb.FirebaseListener()

    var currentSource by mutableStateOf<ApiService?>(null)

    val gridState = LazyGridState(0, 0)

    val sources = mutableStateListOf<SourceInformation>()

    init {
        sourceRepository.sources
            .onEach {
                sources.clear()
                sources.addAll(it)
            }
            .launchIn(viewModelScope)

        combine(
            itemListener.getAllShowsFlow(),
            dao.getAllFavorites()
        ) { f, d -> (f + d).groupBy(DbModel::url).map { it.value.fastMaxBy(DbModel::numChapters)!! } }
            .onEach {
                favoriteList.clear()
                favoriteList.addAll(it)
            }
            .launchIn(viewModelScope)

        currentSourceRepository.asFlow()
            .filterNotNull()
            .onEach {
                currentSource = it
                count = 1
                sourceList.clear()
                sourceLoadCompose()
            }
            .launchIn(viewModelScope)

        snapshotFlow { currentSource }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { gridState.scrollToItem(0) }
            .launchIn(viewModelScope)

        observeNetwork
            .onEach { if (sourceList.isEmpty() && currentSource != null && it && count != 1) reset() }
            .launchIn(viewModelScope)
    }

    fun reset() {
        count = 1
        sourceList.clear()
        sourceLoadCompose()
    }

    fun loadMore() {
        count++
        sourceLoadCompose()
    }

    private fun sourceLoadCompose() {
        currentSource
            ?.getRecentFlow(count)
            ?.onStart { isRefreshing = true }
            ?.dispatchIo()
            ?.catch {
                it.printStackTrace()
                showError()
                emit(emptyList())
                recordFirebaseException(it)
            }
            ?.onCompletion { isRefreshing = false }
            ?.onEach { sourceList.addAll(it) }
            ?.launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        itemListener.unregister()
    }
}