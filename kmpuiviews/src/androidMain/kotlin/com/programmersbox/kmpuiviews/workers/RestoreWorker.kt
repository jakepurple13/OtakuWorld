package com.programmersbox.kmpuiviews.workers

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.programmersbox.kmpuiviews.readPlatformFile
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.utils.Backup
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationDslBuilder
import com.programmersbox.kmpuiviews.utils.NotificationLogo

private const val RESTORE_NOTIFICATION_ID = 201

class RestoreWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val backup: Backup,
    private val logo: NotificationLogo,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val uri = inputData.getString("uri") ?: return Result.failure()
        setForeground(getForegroundInfo())
        return runCatching {
            backup.restoreBackup(readPlatformFile(uri))
        }.fold(
            onSuccess = {
                postCompletionNotification("Restore complete", timeoutAfter = 3000L)
                Result.success()
            },
            onFailure = { e ->
                recordFirebaseException(e)
                postCompletionNotification("Restore failed", timeoutAfter = null)
                Result.failure()
            }
        )
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationDslBuilder.builder(
            applicationContext,
            NotificationChannels.Backup.id,
            logo.notificationId,
        ) {
            title = "Restoring…"
            onlyAlertOnce = true
            ongoing = true
            progress {
                max = 0
                progress = 0
                indeterminate = true
            }
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(RESTORE_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(RESTORE_NOTIFICATION_ID, notification)
        }
    }

    private fun postCompletionNotification(title: String, timeoutAfter: Long?) {
        val notification = NotificationDslBuilder.builder(
            applicationContext,
            NotificationChannels.Backup.id,
            logo.notificationId,
        ) {
            this.title = title
            if (timeoutAfter != null) this.timeoutAfter = timeoutAfter
        }
        applicationContext.getSystemService<NotificationManager>()
            ?.notify(RESTORE_NOTIFICATION_ID, notification)
    }
}
