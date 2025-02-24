package com.programmersbox.uiviews.checkers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.NotificationLogo
import org.koin.core.component.KoinComponent

class NotifySingleWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val logo: NotificationLogo,
    private val genericInfo: GenericInfo,
    private val sourceRepository: SourceRepository,
) : CoroutineWorker(context, workerParams), KoinComponent {
    override suspend fun doWork(): Result {
        inputData.getString("notiData")
            ?.fromJson<NotificationItem>()
            ?.let { d -> SavedNotifications.viewNotificationFromDb(applicationContext, d, logo, genericInfo, sourceRepository) }
        return Result.success()
    }
}