package com.programmersbox.uiviews.favorite

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.models.ApiService
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.GenericInfo
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FavoriteViewModel(dao: ItemDao, private val genericInfo: GenericInfo) : ViewModel() {

    private val fireListener = FirebaseDb.FirebaseListener()
    var favoriteList by mutableStateOf<List<DbModel>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            combine(
                fireListener.getAllShowsFlow(),
                dao.getAllFavoritesFlow()
            ) { f, d -> (f + d).groupBy(DbModel::url).map { it.value.fastMaxBy(DbModel::numChapters)!! } }
                .collect { favoriteList = it }
        }
    }

    override fun onCleared() {
        super.onCleared()
        fireListener.unregister()
    }

    var sortedBy by mutableStateOf<SortFavoritesBy<*>>(SortFavoritesBy.TITLE)
    var reverse by mutableStateOf(false)

    val selectedSources = mutableStateListOf(*genericInfo.sourceList().fastMap(ApiService::serviceName).toTypedArray())

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

}

sealed class SortFavoritesBy<K>(val sort: (Map.Entry<String, List<DbModel>>) -> K) {
    object TITLE : SortFavoritesBy<String>(Map.Entry<String, List<DbModel>>::key)
    object COUNT : SortFavoritesBy<Int>({ it.value.size })
    object CHAPTERS : SortFavoritesBy<Int>({ it.value.maxOf(DbModel::numChapters) })
}