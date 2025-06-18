package com.programmersbox.kmpuiviews.presentation.settings.lists

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.ListDao
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class OtakuListViewModel(
    listDao: ListDao,
) : ViewModel() {
    val customLists = mutableStateListOf<CustomList>()

    init {
        listDao
            .getAllLists()
            .onEach {
                customLists.clear()
                customLists.addAll(it)
            }
            .launchIn(viewModelScope)
    }
}
