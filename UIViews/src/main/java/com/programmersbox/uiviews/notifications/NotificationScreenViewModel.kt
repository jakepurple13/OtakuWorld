package com.programmersbox.uiviews.notifications

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
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
    sourceRepository: SourceRepository,
) : ViewModel() {

    private val notificationSortBy = settingsHandling.notificationSortBy

    val items = mutableStateListOf<NotificationItem>()

    var sortedBy by mutableStateOf(NotificationSortBy.Date)

    val groupedList by derivedStateOf {
        items.groupBy { it.source }
            .toList()
            .sortedByDescending { it.second.size }
    }

    val groupedListState = mutableStateMapOf(
        *sourceRepository.list.map { it.apiService.serviceName to mutableStateOf(false) }.toTypedArray()
    )

    init {
        db.getAllNotificationsFlow()
            .onEach {
                items.clear()
                items.addAll(it)
                val l = groupedListState.toMap()
                groupedListState.clear()
                groupedListState.putAll(
                    it
                        .groupBy { n -> n.source }
                        .mapValues { n -> l[n.key] ?: mutableStateOf(false) }
                )
            }
            .launchIn(viewModelScope)

        notificationSortBy
            .asFlow()
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
            notificationSortBy.set(
                when (sortedBy) {
                    NotificationSortBy.Date -> NotificationSortBy.Grouped
                    NotificationSortBy.Grouped -> NotificationSortBy.Date
                    NotificationSortBy.UNRECOGNIZED -> NotificationSortBy.Date
                }
            )
        }
    }
}