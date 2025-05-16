package com.programmersbox.uiviews.checkers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.presentation.notifications.NotificationScreenInterface
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent

class NotifySingleWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val notificationScreenInterface: NotificationScreenInterface,
) : CoroutineWorker(context, workerParams), KoinComponent {
    override suspend fun doWork(): Result {
        inputData.getString("notiData")
            ?.let { Json.decodeFromString<NotificationItem>(it) }
            ?.let { d -> notificationScreenInterface.notifyItem(d) }
        return Result.success()
    }
}