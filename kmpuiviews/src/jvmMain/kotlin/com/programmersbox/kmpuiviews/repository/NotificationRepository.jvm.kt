package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem

private const val GROUP_ID = 42

actual class NotificationRepository(
    private val itemDao: ItemDao,
) {

    actual suspend fun cancelById(id: Int) {

    }

    actual suspend fun cancelNotification(item: NotificationItem) {

    }

    actual fun cancelGroup() {

    }
}