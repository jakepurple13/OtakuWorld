package com.programmersbox.kmpuiviews.repository

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.receivers.UPDATE_GROUP_SUMMARY_ID
import com.programmersbox.kmpuiviews.receivers.cancelUpdateSummaryIfEmpty

actual class NotificationRepository(
    context: Context,
    private val itemDao: ItemDao,
) {
    private val notificationManager by lazy {
        context.getSystemService<NotificationManager>()
    }

    init {
        //TODO: Need to do something about when going the app updates with notifications
        /*val g = notificationManager
            ?.activeNotifications
            ?.map { it.notification }
            ?.filter { it.group == "otakuGroup" }
            .orEmpty()
        if (g.size == 1) notificationManager?.cancel(GROUP_ID)*/
    }

    actual suspend fun cancelById(id: Int) {
        notificationManager?.cancel(id)
        itemDao.updateNotification(id, false)
        notificationManager?.cancelUpdateSummaryIfEmpty(cancelledId = id)
    }

    actual suspend fun cancelNotification(item: NotificationItem) {
        notificationManager?.cancel(item.id)
        notificationManager?.cancelUpdateSummaryIfEmpty(cancelledId = item.id)
    }

    actual fun cancelGroup() {
        notificationManager?.cancel(UPDATE_GROUP_SUMMARY_ID)
    }
}