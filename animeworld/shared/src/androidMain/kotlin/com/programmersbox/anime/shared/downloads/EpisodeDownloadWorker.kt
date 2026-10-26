package com.programmersbox.anime.shared.downloads

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.work.BackoffPolicy
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
import okhttp3.OkHttpClient
import org.koin.core.component.KoinComponent
import java.io.File
import java.util.concurrent.TimeUnit

const val DOWNLOAD_TAG = "episodeDownload"
const val TITLE_TAG_PREFIX = "episodeTitle:"
const val KEY_TAG_PREFIX = "episodeKey:"
const val PROGRESS_KEY = "progress"
const val BYTES_KEY = "bytes"
const val TOTAL_KEY = "total"

private const val KEY_REQUEST_FILE = "episodeDownloadRequestFile"
private const val TAG = "EpisodeDownloadWorker"
private const val MAX_FOREGROUND_ATTEMPTS = 5

fun episodeDownloadsDir(context: Context): File = File(context.noBackupFilesDir, "episode-downloads")

fun enqueueEpisodeDownload(
    context: Context,
    request: EpisodeDownloadRequest,
    policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
) {
    val uniqueName = request.folder + request.title
    val requestFile = writeRequestFile(episodeDownloadsDir(context), uniqueName, request)
    WorkManager.getInstance(context)
        .enqueueUniqueWork(
            uniqueName,
            policy,
            OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
                .setInputData(workDataOf(KEY_REQUEST_FILE to requestFile.absolutePath))
                .addTag(DOWNLOAD_TAG)
                .addTag(TITLE_TAG_PREFIX + request.title)
                .addTag(KEY_TAG_PREFIX + uniqueName)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
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
    private val key get() = request?.let { it.folder + it.title }.orEmpty()
    private val notificationId get() = (request?.let { it.folder + it.title } ?: id.toString()).hashCode()
    private val smallIcon by lazy {
        getKoin().getOrNull<NotificationLogo>()?.notificationId ?: applicationContext.applicationInfo.icon
    }

    override suspend fun doWork(): Result {
        val request = request ?: return Result.failure().also { requestFile?.delete() }
        val foregroundFailure = runCatching { setForeground(getForegroundInfo()) }.exceptionOrNull()
        if (foregroundFailure is CancellationException) throw foregroundFailure
        if (foregroundFailure != null) {
            // Without a foreground service a long HLS download is killed after ~10 minutes; retry instead of restarting silently.
            Log.w(TAG, "Could not start foreground download", foregroundFailure)
            if (runAttemptCount < MAX_FOREGROUND_ATTEMPTS) return Result.retry()
            val reason = "Could not start in the background"
            requestFile?.let { writeFailure(it, DownloadFailure(key, request.title, reason, System.currentTimeMillis())) }
            notify(
                notificationId + 1,
                resultNotification(request.title, "Download failed: could not start in the background").build(),
            )
            return Result.failure()
        }

        var lastPercent = -1
        var succeeded = false
        var failureMessage: String? = null
        return try {
            clearFailure(episodeDownloadsDir(applicationContext), key)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                throw IllegalStateException("All files access is needed to save downloads")
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                throw IllegalStateException("Storage permission is needed to save downloads")
            }
            val partsRecord = requestFile?.let(::partsRecordFor)
            val downloader = EpisodeDownloader(client, warn = { message, t -> Log.w("EpisodeDownloader", message, t) })
            val file = downloader.download(request, partsRecord) { progress ->
                val fraction = progress.fraction
                if (fraction == null) {
                    // Unknown length: the downloader already throttles these, publish each one.
                    setProgressAsync(workDataOf(PROGRESS_KEY to -1f, BYTES_KEY to progress.bytes, TOTAL_KEY to -1L))
                    notify(
                        notificationId,
                        progressNotification(request.title, 0, "Downloading… ${formatBytes(progress.bytes)}"),
                    )
                    return@download
                }
                val percent = (fraction * 100).toInt()
                if (percent != lastPercent) {
                    lastPercent = percent
                    setProgressAsync(
                        workDataOf(
                            PROGRESS_KEY to fraction,
                            BYTES_KEY to progress.bytes,
                            TOTAL_KEY to (progress.totalBytes ?: -1L),
                        )
                    )
                    notify(notificationId, progressNotification(request.title, percent))
                }
            }
            MediaScannerConnection.scanFile(applicationContext, arrayOf(file.absolutePath), null, null)
            notify(
                notificationId + 1,
                resultNotification(request.title, "Download complete").setTimeoutAfter(5000).build(),
            )
            succeeded = true
            Result.success(workDataOf("file" to file.absolutePath))
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "Episode download failed: ${request.title}", t)
            val message = t.message ?: t::class.java.simpleName
            failureMessage = message
            notify(
                notificationId + 1,
                resultNotification(request.title, "Download failed: $message")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("Download failed: $message"))
                    .build(),
            )
            Result.failure(workDataOf("error" to message))
        } finally {
            NotificationManagerCompat.from(applicationContext).cancel(notificationId)
            // Keep the request when the system stopped us (constraints, quota) or the download failed (Retry).
            val outcome = requestOutcome(
                succeeded = succeeded,
                failed = failureMessage != null,
                isStopped = isStopped,
                cancelledByApp = isStopped && stopReason == WorkInfo.STOP_REASON_CANCELLED_BY_APP,
            )
            if (outcome.writeFailure) {
                requestFile?.let {
                    writeFailure(it, DownloadFailure(key, request.title, failureMessage.orEmpty(), System.currentTimeMillis()))
                }
            }
            if (outcome.deleteRequest) requestFile?.delete()
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

    private fun progressNotification(title: String, percent: Int, text: String = "Downloading… $percent%"): Notification =
        NotificationCompat.Builder(applicationContext, NotificationChannels.Download.id)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(text)
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
