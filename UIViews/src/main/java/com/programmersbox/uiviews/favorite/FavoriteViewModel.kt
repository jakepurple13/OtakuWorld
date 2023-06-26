package com.programmersbox.uiviews.favorite

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.models.ApiService
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.Screen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class FavoriteViewModel(dao: ItemDao, private val genericInfo: GenericInfo) : ViewModel() {

    private val fireListener = FirebaseDb.FirebaseListener()
    var favoriteList by mutableStateOf<List<DbModel>>(emptyList())
        private set

    init {
        combine(
            fireListener.getAllShowsFlow(),
            dao.getAllFavorites()
        ) { f, d -> (f + d).groupBy(DbModel::url).map { it.value.fastMaxBy(DbModel::numChapters)!! } }
            .onEach { favoriteList = it }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        fireListener.unregister()
    }

    var searchText by mutableStateOf("")

    var sortedBy by mutableStateOf<SortFavoritesBy<*>>(SortFavoritesBy.TITLE)
    var reverse by mutableStateOf(false)

    val selectedSources = mutableStateListOf(*genericInfo.sourceList().fastMap(ApiService::serviceName).toTypedArray())

    val listSources by derivedStateOf {
        favoriteList.filter { it.title.contains(searchText, true) && it.source in selectedSources }
    }

    val groupedSources by derivedStateOf {
        listSources
            .groupBy(DbModel::title)
            .entries
            .let {
                when (val s = sortedBy) {
                    is SortFavoritesBy.TITLE -> it.sortedBy(s.sort)
                    is SortFavoritesBy.COUNT -> it.sortedByDescending(s.sort)
                    is SortFavoritesBy.CHAPTERS -> it.sortedByDescending(s.sort)
                }
            }
            .let { if (reverse) it.reversed() else it }
            .toTypedArray()
    }

    val allSources by derivedStateOf {
        (genericInfo.sourceList().fastMap(ApiService::serviceName) + listSources.fastMap(DbModel::source))
            .groupBy { it }
            .toList()
            .sortedBy { it.first }
    }

    fun newSource(item: String) {
        if (item in selectedSources) selectedSources.remove(item) else selectedSources.add(item)
    }

    fun singleSource(item: String) {
        selectedSources.clear()
        selectedSources.add(item)
    }

    fun resetSources() {
        selectedSources.clear()
        selectedSources.addAll(genericInfo.sourceList().fastMap(ApiService::serviceName))
    }

    private fun clearAllSources() {
        selectedSources.clear()
    }

    fun allClick() {
        if (selectedSources.size == genericInfo.sourceList().size) {
            clearAllSources()
        } else {
            resetSources()
        }
    }

}

sealed class SortFavoritesBy<K>(val sort: (Map.Entry<String, List<DbModel>>) -> K) {
    object TITLE : SortFavoritesBy<String>(Map.Entry<String, List<DbModel>>::key)
    object COUNT : SortFavoritesBy<Int>({ it.value.size })
    object CHAPTERS : SortFavoritesBy<Int>({ it.value.maxOf(DbModel::numChapters) })
}

class FavoriteChoiceViewModel(
    handle: SavedStateHandle,
) : ViewModel() {
    val items = handle.get<String>(Screen.FavoriteChoiceScreen.dbitemsArgument)
        .fromJson<List<DbModel>>()
        .orEmpty()
}