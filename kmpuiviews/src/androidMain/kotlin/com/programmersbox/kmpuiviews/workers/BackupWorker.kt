package com.programmersbox.kmpuiviews.workers

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.programmersbox.kmpuiviews.readPlatformFile
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.utils.Backup
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationDslBuilder
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.sharedcomponents.backup.ItemResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val BACKUP_NOTIFICATION_ID = 200

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val backup: Backup,
    private val logo: NotificationLogo,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val uri = inputData.getString("uri") ?: return Result.failure()
        val selectedKeys = inputData.getStringArray("selectedKeys")?.toSet() ?: return Result.failure()
        setForeground(getForegroundInfo())
        val results = mutableListOf<ItemResult>()
        return runCatching {
            backup.createBackup(readPlatformFile(uri), selectedKeys) { result ->
                results += result
                setProgress(workDataOf("results" to Json.encodeToString(results.toList())))
            }
        }.fold(
            onSuccess = { finalResults ->
                postCompletionNotification("Backup complete", timeoutAfter = 3000L)
                Result.success(workDataOf("results" to Json.encodeToString(finalResults)))
            },
            onFailure = { e ->
                recordFirebaseException(e)
                postCompletionNotification("Backup failed", timeoutAfter = null)
                Result.failure(workDataOf("results" to Json.encodeToString(results.toList())))
            }
        )
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationDslBuilder.builder(
            applicationContext,
            NotificationChannels.Backup.id,
            logo.notificationId,
        ) {
            title = "Backing up…"
            onlyAlertOnce = true
            ongoing = true
            progress {
                max = 0
                progress = 0
                indeterminate = true
            }
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(BACKUP_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(BACKUP_NOTIFICATION_ID, notification)
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
            ?.notify(BACKUP_NOTIFICATION_ID, notification)
    }
}
