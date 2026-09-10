package com.programmersbox.kmpuiviews.screensaver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class CoverCarouselViewModel(
    db: ItemDao,
) : ViewModel() {

    val items: StateFlow<List<NotificationItem>>
        field = MutableStateFlow<List<NotificationItem>>(emptyList())

    init {
        db
            .getAllNotificationsFlow()
            .onEach { list ->
                items.update { list }
            }
            .launchIn(viewModelScope)
    }
}
