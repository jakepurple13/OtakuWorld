package com.programmersbox.kmpuiviews.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.utils.dispatchIo
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SettingViewModel(
    dao: ItemDao,
) : ViewModel() {

    var savedNotifications by mutableIntStateOf(0)
        private set

    init {
        dao.getAllNotificationCount()
            .dispatchIo()
            .onEach { savedNotifications = it }
            .launchIn(viewModelScope)
    }
}