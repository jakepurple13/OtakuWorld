package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.NotificationItem

expect class NotificationRepository {
    suspend fun cancelById(id: Int)
    suspend fun cancelNotification(item: NotificationItem)
    fun cancelGroup()
}
