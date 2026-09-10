package com.programmersbox.kmpuiviews.screensaver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.RecentModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class HistoryFeedViewModel(
    historyDao: HistoryDao,
) : ViewModel() {

    val recentlyViewed: StateFlow<List<RecentModel>>
        field = MutableStateFlow<List<RecentModel>>(emptyList())

    init {
        historyDao
            .getRecentlyViewed()
            .onEach { list ->
                recentlyViewed.update { list.sortedByDescending { item -> item.timestamp } }
            }
            .launchIn(viewModelScope)
    }
}
