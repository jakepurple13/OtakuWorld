package com.programmersbox.uiviews.recent

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.util.fastMaxBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.sourceFlow
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.utils.dispatchIoAndCatchList
import com.programmersbox.uiviews.utils.showErrorToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import ru.beryukhov.reactivenetwork.ReactiveNetwork

class RecentViewModel(dao: ItemDao, context: Context? = null) : ViewModel() {

    var isRefreshing by mutableStateOf(false)
    val sourceList = mutableStateListOf<ItemModel>()
    var favoriteList = mutableStateListOf<DbModel>()

    val observeNetwork = ReactiveNetwork()
        .observeInternetConnectivity()
        .flowOn(Dispatchers.IO)

    var count = 1

    private val itemListener = FirebaseDb.FirebaseListener()

    init {
        combine(
            itemListener.getAllShowsFlow(),
            dao.getAllFavorites()
        ) { f, d -> (f + d).groupBy(DbModel::url).map { it.value.fastMaxBy(DbModel::numChapters)!! } }
            .onEach { favoriteList = it.toMutableStateList() }
            .launchIn(viewModelScope)

        sourceFlow
            .filterNotNull()
            .onEach {
                count = 1
                sourceList.clear()
                sourceLoadCompose(context, it)
            }
            .launchIn(viewModelScope)
    }

    fun reset(context: Context?, sources: ApiService) {
        count = 1
        sourceList.clear()
        sourceLoadCompose(context, sources)
    }

    fun loadMore(context: Context?, sources: ApiService) {
        count++
        sourceLoadCompose(context, sources)
    }

    private fun sourceLoadCompose(context: Context?, sources: ApiService) {
        sources
            .getRecentFlow(count)
            .dispatchIoAndCatchList()
            .catch {
                context?.showErrorToast()
                emit(emptyList())
            }
            .onStart { isRefreshing = true }
            .onCompletion { isRefreshing = false }
            .onEach { sourceList.addAll(it) }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        itemListener.unregister()
    }

}