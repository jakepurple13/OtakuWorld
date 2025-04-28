package com.programmersbox.uiviews.checkers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.NotificationLogo
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent

class NotifySingleWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val logo: NotificationLogo,
    private val genericInfo: GenericInfo,
    private val sourceRepository: SourceRepository,
    private val itemDao: ItemDao,
) : CoroutineWorker(context, workerParams), KoinComponent {
    override suspend fun doWork(): Result {
        inputData.getString("notiData")
            ?.let { Json.decodeFromString<NotificationItem>(it) }
            ?.let { d ->
                SavedNotifications.viewNotificationFromDb(
                    context = applicationContext,
                    n = d,
                    notificationLogo = logo,
                    info = genericInfo,
                    sourceRepository = sourceRepository,
                    itemDao = itemDao
                )
            }
        return Result.success()
    }
}