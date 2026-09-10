package com.programmersbox.kmpuiviews.screensaver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ActivityDao
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.seconds

class StatsDashboardViewModel(
    db: ItemDao,
    activityDao: ActivityDao,
    historyDao: HistoryDao,
    listDao: ListDao,
) : ViewModel() {

    var activity by mutableStateOf("")

    var favoritesCount by mutableIntStateOf(0)

    var recentHistoryCount by mutableIntStateOf(0)

    var customListsCount by mutableIntStateOf(0)

    init {
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

        historyDao
            .getAllRecentHistoryCount()
            .onEach { recentHistoryCount = it }
            .launchIn(viewModelScope)

        listDao
            .getAllListsCount()
            .onEach { customListsCount = it }
            .launchIn(viewModelScope)
    }
}
