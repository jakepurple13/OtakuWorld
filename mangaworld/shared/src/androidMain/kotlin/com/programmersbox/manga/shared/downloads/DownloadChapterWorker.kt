package com.programmersbox.manga.shared.downloads

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.abs

class DownloadChapterWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val notificationManager by lazy {
        NotificationManagerCompat.from(applicationContext)
    }

    override suspend fun doWork(): Result {
        println(tags)
        val mangaTitle = inputData.getString(KEY_MANGA_TITLE) ?: return Result.failure()
        val chapterName = inputData.getString(KEY_CHAPTER_NAME) ?: return Result.failure()
        val chapterUrl = inputData.getString(KEY_CHAPTER_URL) ?: return Result.failure()
        val imageUrls = inputData.getString(KEY_IMAGE_URLS)
            ?.let { Json.decodeFromString<List<String>>(it) }
            ?: return Result.failure()

        val headers = inputData.getString(KEY_HEADERS)
            ?.let { Json.decodeFromString<Map<String, String>>(it) }
            ?: emptyMap()

        val externalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ?: return Result.failure(workDataOf(KEY_ERROR to "External storage unavailable"))

        val destDir = File(externalDir, "MangaWorld/${mangaTitle.sanitize()}/${chapterName.sanitize()}")
            .also { it.mkdirs() }

        val request = DownloadRequest(
            chapterUrl = chapterUrl,
            chapterName = chapterName,
            mangaTitle = mangaTitle,
            imageUrls = imageUrls,
            headers = headers,
        )

        println(request)

        val notifId = abs(chapterUrl.hashCode())
        val notifCompleteId = notifId + 100_000
        val notifFailId = notifId + 200_000

        // Show indeterminate progress while the first image hasn't loaded yet
        postNotification(
            id = notifId,
            notification = buildProgressNotification(
                mangaTitle = mangaTitle,
                chapterName = chapterName,
                done = 0,
                total = 0,
                indeterminate = true,
            ),
        )

        val client = HttpClient()
        return try {
            executeDownload(
                client = client,
                request = request,
                onProgress = { done, total ->
                    setProgress(
                        workDataOf(
                            KEY_PROGRESS_DONE to done,
                            KEY_PROGRESS_TOTAL to total,
                            KEY_CHAPTER_NAME to chapterName,
                            KEY_MANGA_TITLE to mangaTitle,
                        )
                    )
                    postNotification(
                        id = notifId,
                        notification = buildProgressNotification(
                            mangaTitle = mangaTitle,
                            chapterName = chapterName,
                            done = done,
                            total = total,
                            indeterminate = false,
                        ),
                    )
                },
                writeBytes = { index, bytes ->
                    File(destDir, "%03d.png".format(index)).writeBytes(bytes)
                },
            )
            MediaScannerConnection.scanFile(
                applicationContext,
                destDir.listFiles()?.map { it.absolutePath }?.toTypedArray() ?: emptyArray(),
                null,
                null,
            )
            // Dismiss progress, show completion
            notificationManager.cancel(notifId)
            postNotification(
                id = notifCompleteId,
                notification = buildCompleteNotification(mangaTitle, chapterName),
            )
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            if (runAttemptCount < 3) {
                // Leave progress notification visible — next attempt will overwrite it
                Result.retry()
            } else {
                // All retries exhausted
                notificationManager.cancel(notifId)
                postNotification(
                    id = notifFailId,
                    notification = buildFailedNotification(chapterName, e.message ?: "Unknown error"),
                )
                destDir.deleteRecursively()
                Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
            }
        } finally {
            client.close()
        }
    }

    // ── Notification builders ──────────────────────────────────────────────

    private fun buildProgressNotification(
        mangaTitle: String,
        chapterName: String,
        done: Int,
        total: Int,
        indeterminate: Boolean,
    ) = NotificationCompat.Builder(applicationContext, NotificationChannels.Download.id)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(mangaTitle)
        .setContentText(chapterName)
        .setProgress(total, done, indeterminate)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .build()

    private fun buildCompleteNotification(
        mangaTitle: String,
        chapterName: String,
    ) = NotificationCompat.Builder(applicationContext, NotificationChannels.Download.id)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle("Downloaded")
        .setContentText("$mangaTitle — $chapterName")
        .setAutoCancel(true)
        .build()

    private fun buildFailedNotification(
        chapterName: String,
        reason: String,
    ) = NotificationCompat.Builder(applicationContext, NotificationChannels.Download.id)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("Download Failed")
        .setContentText("$chapterName: $reason")
        .setAutoCancel(true)
        .build()

    /**
     * Posts a notification only when POST_NOTIFICATIONS permission is granted (Android 13+)
     * or not yet required (Android 12 and below). Lint suppressed because the permission
     * is checked manually rather than via @RequiresPermission annotation.
     */
    @SuppressLint("MissingPermission")
    private fun postNotification(id: Int, notification: android.app.Notification) {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(id, notification)
        }
    }

    companion object {
        const val KEY_MANGA_TITLE = "mangaTitle"
        const val KEY_CHAPTER_NAME = "chapterName"
        const val KEY_CHAPTER_URL = "chapterUrl"
        const val KEY_IMAGE_URLS = "imageUrls"
        const val KEY_HEADERS = "headers"
        const val KEY_PROGRESS_DONE = "done"
        const val KEY_PROGRESS_TOTAL = "total"
        const val KEY_ERROR = "error"
        const val DOWNLOAD_TAG = "manga_chapter_download"
        const val DOWNLOAD_QUEUE = "manga_download_queue"
    }
}
