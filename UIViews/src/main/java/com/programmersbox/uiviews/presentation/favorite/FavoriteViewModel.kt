package com.programmersbox.uiviews.presentation.favorite

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import com.programmersbox.kmpuiviews.utils.fireListener
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class FavoriteViewModel(
    private val sourceRepository: SourceRepository,
    favoritesRepository: FavoritesRepository,
    firebaseFavoriteListener: KmpFirebaseConnection.KmpFirebaseListener,
) : ViewModel() {

    private val fireListener = fireListener("favorite", firebaseFavoriteListener)

    private val favoriteList = mutableStateListOf<DbModel>()

    private var sourceList = sourceRepository.list.map { it.apiService.serviceName }

    private val fullSourceList get() = (sourceList + favoriteList.map { it.source }).distinct()

    init {
        favoritesRepository
            .getAllFavorites(fireListener)
            .onEach {
                favoriteList.clear()
                favoriteList.addAll(it)
                selectedSources.addAll(
                    sourceRepository.list.map { l -> l.apiService.serviceName } +
                            it.map { f -> f.source }
                )
            }
            .launchIn(viewModelScope)

        sourceRepository.sources
            .onEach { sourceList = it.map { s -> s.apiService.serviceName } }
            .launchIn(viewModelScope)
    }

    var searchText by mutableStateOf(TextFieldState())

    var sortedBy by mutableStateOf<SortFavoritesBy<*>>(SortFavoritesBy.TITLE)
    var reverse by mutableStateOf(false)

    val selectedSources = mutableStateListOf<String>()

    val listSources by derivedStateOf {
        favoriteList.filter { it.title.contains(searchText.text, true) && it.source in selectedSources }
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
    }

    val allSources by derivedStateOf {
        (fullSourceList + listSources.fastMap(DbModel::source))
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
        selectedSources.addAll(fullSourceList)
    }

    private fun clearAllSources() {
        selectedSources.clear()
    }

    fun allClick() {
        if (selectedSources.size == fullSourceList.size) {
            clearAllSources()
        } else {
            resetSources()
        }
    }
}

sealed class SortFavoritesBy<K>(val sort: (Map.Entry<String, List<DbModel>>) -> K) {
    data object TITLE : SortFavoritesBy<String>(Map.Entry<String, List<DbModel>>::key)
    data object COUNT : SortFavoritesBy<Int>({ it.value.size })
    data object CHAPTERS : SortFavoritesBy<Int>({ it.value.maxOf(DbModel::numChapters) })
}
