package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem

private const val GROUP_ID = 42

actual class NotificationRepository(
    private val itemDao: ItemDao,
) {

    actual fun cancelById(id: Int) {

    }

    actual fun cancelNotification(item: NotificationItem) {

    }

    actual fun cancelGroup() {

    }
}