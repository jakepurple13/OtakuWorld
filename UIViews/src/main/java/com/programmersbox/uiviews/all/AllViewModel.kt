package com.programmersbox.uiviews.all

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

class AllViewModel(dao: ItemDao, context: Context? = null) : ViewModel() {

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
            .getListFlow(count)
            .dispatchIoAndCatchList { context?.showErrorToast() }
            .onStart { isRefreshing = true }
            .onEach { sourceList.addAll(it) }
            .onCompletion { isRefreshing = false }
            .launchIn(viewModelScope)
    }

    fun search() {
        sourceFlow
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