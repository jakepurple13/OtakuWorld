package com.programmersbox.kmpuiviews.receivers

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.utils.printLogs

/** Deletes the saved update row for [url], cancels notification [id], and cancels the summary (42) when it was the last one. */
suspend fun clearUpdateNotification(context: Context?, dao: ItemDao, url: String?, id: Int?) {
    val notificationManager = context?.getSystemService<NotificationManager>()
    runCatching {
        url?.let { dao.getNotificationItem(it) }
            .also { printLogs { it } }
            ?.let { dao.deleteNotification(it) }
    }.onFailure { recordFirebaseException(it) }
    id?.let { if (it != -1) notificationManager?.cancel(it) }
    val g = notificationManager
        ?.activeNotifications
        ?.map { it.notification }
        ?.filter { it.group == "otakuGroup" }
        .orEmpty()
    if (g.size == 1) notificationManager?.cancel(42)
}
