package com.programmersbox.kmpuiviews.screensaver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ActivityDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.seconds

class ScreensaverViewModel(
    db: ItemDao,
    activityDao: ActivityDao,
) : ViewModel() {

    val items: StateFlow<List<NotificationItem>>
        field = MutableStateFlow<List<NotificationItem>>(emptyList())

    var activity by mutableStateOf("")

    var favoritesCount by mutableIntStateOf(0)

    init {
        db
            .getAllNotificationsFlow()
            .onEach { list ->
                items.update { list }
            }
            .launchIn(viewModelScope)

        activityDao
            .observeActivity()
            .onEach {
                activity = it?.cumulativeSeconds?.seconds?.toString() ?: ""
            }
            .launchIn(viewModelScope)

        db
            .getAllFavoritesCount()
            .onEach { favoritesCount = it }
            .launchIn(viewModelScope)
    }
}