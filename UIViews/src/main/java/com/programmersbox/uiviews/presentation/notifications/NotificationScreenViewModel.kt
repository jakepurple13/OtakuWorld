package com.programmersbox.uiviews.presentation.notifications

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.NotificationSortBy
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.uiviews.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationScreenViewModel(
    private val db: ItemDao,
    settingsHandling: NewSettingsHandling,
    sourceRepository: SourceRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val notificationSortBy = settingsHandling.notificationSortBy

    val items = mutableStateListOf<NotificationItem>()

    var sortedBy by mutableStateOf(NotificationSortBy.Date)

    val groupedList by derivedStateOf {
        items.groupBy { it.source }
            .toList()
            .sortedByDescending { it.second.size }
    }

    //TODO: Maybe make the groupedList empty if it's false?
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

        db.getAllNotificationCount()
            .filter { it == 0 }
            .onEach { notificationRepository.cancelGroup() }
            .launchIn(viewModelScope)

        notificationSortBy
            .asFlow()
            .onEach { sortedBy = it }
            .launchIn(viewModelScope)
    }

    fun deleteNotification(item: NotificationItem, block: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) { db.deleteNotification(item) }
            notificationRepository.cancelNotification(item)
            block()
        }
    }

    fun cancelNotification(notificationItem: NotificationItem) = notificationRepository.cancelNotification(notificationItem)
    fun cancelNotificationById(id: Int) = notificationRepository.cancelById(id)

    suspend fun deleteAllNotifications(): Int {
        db.getAllNotifications().forEach { notificationRepository.cancelNotification(it) }
        notificationRepository.cancelGroup()
        return db.deleteAllNotifications()
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
                }
            )
        }
    }
}