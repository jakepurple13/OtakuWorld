package com.programmersbox.anime.shared.downloads

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.core.component.KoinComponent
import java.util.concurrent.TimeUnit

private const val KEY_REQUEST = "episodeDownloadRequest"
private const val TAG = "EpisodeDownloadWorker"

fun enqueueEpisodeDownload(context: Context, request: EpisodeDownloadRequest) {
    WorkManager.getInstance(context)
        .enqueueUniqueWork(
            request.folder + request.title,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
                .setInputData(workDataOf(KEY_REQUEST to Json.encodeToString(request)))
                .addTag("episodeDownload")
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        )
}

class EpisodeDownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val request by lazy {
        inputData.getString(KEY_REQUEST)
            ?.let { runCatching { Json.decodeFromString<EpisodeDownloadRequest>(it) }.getOrNull() }
    }
    private val notificationId get() = (request?.let { it.folder + it.title } ?: id.toString()).hashCode()
    private val smallIcon by lazy {
        getKoin().getOrNull<NotificationLogo>()?.notificationId ?: applicationContext.applicationInfo.icon
    }

    override suspend fun doWork(): Result {
        val request = request ?: return Result.failure()
        runCatching { setForeground(getForegroundInfo()) }
            .onFailure { Log.w(TAG, "Could not start foreground download", it) }

        var lastPercent = -1
        return try {
            val file = EpisodeDownloader(client).download(request) { progress ->
                val percent = (progress * 100).toInt()
                if (percent != lastPercent) {
                    lastPercent = percent
                    setProgressAsync(workDataOf("progress" to progress))
                    notify(notificationId, progressNotification(request.title, percent))
                }
            }
            MediaScannerConnection.scanFile(applicationContext, arrayOf(file.absolutePath), null, null)
            notify(
                notificationId + 1,
                resultNotification(request.title, "Download complete").setTimeoutAfter(5000).build(),
            )
            Result.success(workDataOf("file" to file.absolutePath))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Episode download failed: ${request.title}", e)
            val message = e.message ?: e::class.java.simpleName
            notify(
                notificationId + 1,
                resultNotification(request.title, "Download failed: $message")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("Download failed: $message"))
                    .build(),
            )
            Result.failure(workDataOf("error" to message))
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = progressNotification(request?.title.orEmpty(), 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun progressNotification(title: String, percent: Int): Notification =
        NotificationCompat.Builder(applicationContext, NotificationChannels.Download.id)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText("Downloading… $percent%")
            .setProgress(100, percent, percent <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_delete,
                "Cancel",
                WorkManager.getInstance(applicationContext).createCancelPendingIntent(id),
            )
            .build()

    private fun resultNotification(title: String, text: String): NotificationCompat.Builder =
        NotificationCompat.Builder(applicationContext, NotificationChannels.Download.id)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)

    @SuppressLint("MissingPermission")
    private fun notify(id: Int, notification: Notification) {
        if (
            PermissionChecker.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PermissionChecker.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(applicationContext).notify(id, notification)
    }

    private companion object {
        val client: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        }
    }
}
