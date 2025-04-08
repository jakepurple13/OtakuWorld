package com.programmersbox.uiviews.presentation.settings.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.uiviews.datastore.DataStoreHandling
import com.programmersbox.uiviews.utils.dispatchIo
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SettingsViewModel(
    dao: ItemDao,
    dataStoreHandling: DataStoreHandling,
) : ViewModel() {
    val showGemini = dataStoreHandling.showGemini.asFlow()

    var savedNotifications by mutableIntStateOf(0)
        private set

    init {
        dao.getAllNotificationCount()
            .dispatchIo()
            .onEach { savedNotifications = it }
            .launchIn(viewModelScope)
    }
}