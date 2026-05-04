package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.presentation.notifications.NotificationScreenInterface

class NotificationScreenRepository : NotificationScreenInterface {
    override suspend fun notifyItem(notificationItem: NotificationItem) {

    }

    override fun scheduleNotification(item: NotificationItem, time: Long) {

    }
}