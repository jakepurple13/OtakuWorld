package com.programmersbox.uiviews.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationScreenViewModel : ViewModel() {
    fun deleteNotification(db: ItemDao, item: NotificationItem, block: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) { db.deleteNotificationFlow(item) }
            block()
        }
    }
}