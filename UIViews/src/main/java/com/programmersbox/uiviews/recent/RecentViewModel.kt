package com.programmersbox.uiviews.recent

import android.content.Context
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.util.fastMaxBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.SourceInformation
import com.programmersbox.models.sourceFlow
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.utils.dispatchIoAndCatchList
import com.programmersbox.uiviews.utils.showErrorToast
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
    context: Context? = null,
    sourceRepository: SourceRepository
) : ViewModel() {

    var isRefreshing by mutableStateOf(false)
    val sourceList = mutableStateListOf<ItemModel>()
    var favoriteList = mutableStateListOf<DbModel>()

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
            .onEach { favoriteList = it.toMutableStateList() }
            .launchIn(viewModelScope)

        sourceFlow
            .filterNotNull()
            .onEach {
                currentSource = it
                count = 1
                sourceList.clear()
                sourceLoadCompose(context)
            }
            .launchIn(viewModelScope)

        snapshotFlow { currentSource }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { gridState.scrollToItem(0) }
            .launchIn(viewModelScope)
    }

    fun reset(context: Context?) {
        count = 1
        sourceList.clear()
        sourceLoadCompose(context)
    }

    fun loadMore(context: Context?) {
        count++
        sourceLoadCompose(context)
    }

    private fun sourceLoadCompose(context: Context?) {
        currentSource
            ?.getRecentFlow(count)
            ?.dispatchIoAndCatchList()
            ?.catch {
                context?.showErrorToast()
                emit(emptyList())
            }
            ?.onStart { isRefreshing = true }
            ?.onCompletion { isRefreshing = false }
            ?.onEach { sourceList.addAll(it) }
            ?.launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        itemListener.unregister()
    }

}