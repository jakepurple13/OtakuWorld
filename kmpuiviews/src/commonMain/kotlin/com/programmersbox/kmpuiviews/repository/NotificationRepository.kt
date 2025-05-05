package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.NotificationItem

expect class NotificationRepository {
    fun cancelById(id: Int)
    fun cancelNotification(item: NotificationItem)
    fun cancelGroup()
}
