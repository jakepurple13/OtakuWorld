package com.programmersbox.uiviews.lists

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.ListDao
import kotlinx.coroutines.launch
import java.util.UUID

class OtakuCustomListViewModel(
    private val listDao: ListDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val uuid = savedStateHandle.get<String>("uuid")?.let(UUID::fromString)

    var customItem: CustomList? by mutableStateOf(null)
        private set

    init {
        viewModelScope.launch {
            uuid?.let { customItem = listDao.getCustomListItem(it) }
        }
    }

}