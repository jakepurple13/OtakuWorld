package com.programmersbox.kmpuiviews.receivers

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.utils.NotificationGroups
import com.programmersbox.kmpuiviews.utils.printLogs

/** Id of the update group's summary notification. */
const val UPDATE_GROUP_SUMMARY_ID = 42

/** Deletes the saved update row for [url], cancels notification [id], and cancels the summary when it was the last one. */
suspend fun clearUpdateNotification(context: Context?, dao: ItemDao, url: String?, id: Int?) {
    val notificationManager = context?.getSystemService<NotificationManager>()
    runCatching {
        url?.let { dao.getNotificationItem(it) }
            .also { printLogs { it } }
            ?.let { dao.deleteNotification(it) }
    }.onFailure { recordFirebaseException(it) }
    id?.let { if (it != -1) notificationManager?.cancel(it) }
    notificationManager?.cancelUpdateSummaryIfEmpty(cancelledId = id)
}

/**
 * Cancels the update group's summary when no child is left in the shade.
 * [cancelledId] is skipped because [NotificationManager.cancel] is async: the child can still be listed right after it.
 */
fun NotificationManager.cancelUpdateSummaryIfEmpty(cancelledId: Int? = null) {
    val childrenLeft = activeNotifications.any {
        it.notification.group == NotificationGroups.Otaku.id &&
            it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 &&
            it.id != cancelledId
    }
    if (!childrenLeft) cancel(UPDATE_GROUP_SUMMARY_ID)
}
