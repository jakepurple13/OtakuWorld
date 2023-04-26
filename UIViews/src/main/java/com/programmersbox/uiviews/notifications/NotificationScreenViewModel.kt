package com.programmersbox.uiviews.notifications

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.NotificationSortBy
import com.programmersbox.uiviews.utils.SettingsHandling
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationScreenViewModel(
    db: ItemDao,
    private val settingsHandling: SettingsHandling,
    genericInfo: GenericInfo,
) : ViewModel() {

    val items = mutableStateListOf<NotificationItem>()

    var sortedBy by mutableStateOf(NotificationSortBy.Date)

    val groupedList by derivedStateOf {
        items.groupBy { it.source }
    }

    val groupedListState = mutableStateMapOf(
        *genericInfo.sourceList().map { it.serviceName to mutableStateOf(false) }.toTypedArray()
    )

    init {
        db.getAllNotificationsFlow()
            .onEach {
                items.clear()
                items.addAll(it)
            }
            .launchIn(viewModelScope)

        settingsHandling.notificationSortBy
            .onEach { sortedBy = it }
            .launchIn(viewModelScope)
    }

    fun deleteNotification(db: ItemDao, item: NotificationItem, block: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) { db.deleteNotification(item) }
            block()
        }
    }

    fun toggleGroupedState(source: String) {
        groupedListState[source]?.let { it.value = !it.value }
    }

    fun toggleSort() {
        viewModelScope.launch {
            settingsHandling.setNotificationSortBy(
                when (sortedBy) {
                    NotificationSortBy.Date -> NotificationSortBy.Grouped
                    NotificationSortBy.Grouped -> NotificationSortBy.Date
                    NotificationSortBy.UNRECOGNIZED -> NotificationSortBy.Date
                }
            )
        }
    }
}