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
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.core.component.KoinComponent
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val KEY_REQUEST_FILE = "episodeDownloadRequestFile"
private const val TAG = "EpisodeDownloadWorker"

/**
 * Writes [request] as JSON into [dir] (WorkManager `Data` is capped at 10 KB, too small for sources
 * with many subtitle tracks). The name is a UUID derived from [uniqueName], so re-enqueueing the same
 * episode (dropped by `ExistingWorkPolicy.KEEP`) overwrites the file instead of leaking a new one.
 */
internal fun writeRequestFile(dir: File, uniqueName: String, request: EpisodeDownloadRequest): File {
    dir.mkdirs()
    val file = File(dir, UUID.nameUUIDFromBytes(uniqueName.encodeToByteArray()).toString() + ".json")
    file.writeText(Json.encodeToString(request))
    return file
}

internal fun readRequestFile(file: File): EpisodeDownloadRequest? =
    runCatching { Json.decodeFromString<EpisodeDownloadRequest>(file.readText()) }.getOrNull()

fun enqueueEpisodeDownload(context: Context, request: EpisodeDownloadRequest) {
    val uniqueName = request.folder + request.title
    val requestFile = writeRequestFile(File(context.noBackupFilesDir, "episode-downloads"), uniqueName, request)
    WorkManager.getInstance(context)
        .enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
                .setInputData(workDataOf(KEY_REQUEST_FILE to requestFile.absolutePath))
                .addTag("episodeDownload")
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
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

    private val requestFile by lazy { inputData.getString(KEY_REQUEST_FILE)?.let(::File) }
    private val request by lazy { requestFile?.let(::readRequestFile) }
    private val notificationId get() = (request?.let { it.folder + it.title } ?: id.toString()).hashCode()
    private val smallIcon by lazy {
        getKoin().getOrNull<NotificationLogo>()?.notificationId ?: applicationContext.applicationInfo.icon
    }

    override suspend fun doWork(): Result {
        val request = request ?: return Result.failure().also { requestFile?.delete() }
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
        } catch (t: Throwable) {
            Log.e(TAG, "Episode download failed: ${request.title}", t)
            val message = t.message ?: t::class.java.simpleName
            notify(
                notificationId + 1,
                resultNotification(request.title, "Download failed: $message")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("Download failed: $message"))
                    .build(),
            )
            Result.failure(workDataOf("error" to message))
        } finally {
            NotificationManagerCompat.from(applicationContext).cancel(notificationId)
            // Keep the request when the system stopped us (constraints, quota): WorkManager reruns the work.
            if (!isStopped || stopReason == WorkInfo.STOP_REASON_CANCELLED_BY_APP) requestFile?.delete()
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
