package com.programmersbox.kmpuiviews.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.presentation.notifications.NotificationScreenInterface
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.workers.NotifySingleWorker
import com.programmersbox.kmpuiviews.workers.SavedNotifications
import com.programmersbox.kmpuiviews.workers.UpdateNotification
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class NotificationScreenRepository(
    private val updateNotification: UpdateNotification,
    private val genericInfo: PlatformGenericInfo,
    private val sourceRepository: SourceRepository,
    private val itemDao: ItemDao,
    private val context: Context,
    private val notificationLogo: NotificationLogo,
) : NotificationScreenInterface {
    override suspend fun notifyItem(notificationItem: NotificationItem) {
        SavedNotifications.viewNotificationFromDb(
            context = context,
            n = notificationItem,
            notificationLogo = notificationLogo,
            info = genericInfo,
            sourceRepository = sourceRepository,
            itemDao = itemDao,
            update = updateNotification
        )
    }

    override fun scheduleNotification(item: NotificationItem, time: Long) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                item.notiTitle,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<NotifySingleWorker>()
                    .setInputData(
                        workDataOf(
                            "notiData" to Json.encodeToString(item)
                        )
                    )
                    .setInitialDelay(
                        duration = time,
                        timeUnit = TimeUnit.MILLISECONDS
                    )
                    .build()
            )
    }
}