package com.programmersbox.uiviews.lists

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.ListDao
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

class OtakuCustomListViewModel(
    private val listDao: ListDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val uuid = savedStateHandle.get<String>("uuid")?.let(UUID::fromString)

    var customItem: CustomList? by mutableStateOf(null)
        private set

    var searchBarActive by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    val items by derivedStateOf {
        customItem?.list.orEmpty().filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    init {
        uuid?.let(listDao::getCustomListItemFlow)
            ?.onEach { customItem = it }
            ?.launchIn(viewModelScope)
    }

    fun removeItem(item: CustomListInfo) {
        viewModelScope.launch {
            listDao.removeItem(item)
            viewModelScope.launch { customItem?.item?.let { listDao.updateFullList(it) } }
        }
    }

    fun rename(newName: String) {
        viewModelScope.launch { customItem?.item?.copy(name = newName)?.let { listDao.updateFullList(it) } }
    }

    fun deleteAll() {
        viewModelScope.launch { customItem?.let { item -> listDao.removeList(item) } }
    }

    fun setQuery(query: String) {
        searchQuery = query
    }

}