package com.programmersbox.manga.shared.downloads

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File

class DownloadChapterWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val httpClient = HttpClient()

    override suspend fun doWork(): Result {
        val mangaTitle = inputData.getString(KEY_MANGA_TITLE) ?: return Result.failure()
        val chapterName = inputData.getString(KEY_CHAPTER_NAME) ?: return Result.failure()
        val chapterUrl = inputData.getString(KEY_CHAPTER_URL) ?: return Result.failure()
        val imageUrls = inputData.getString(KEY_IMAGE_URLS)
            ?.let { Json.decodeFromString<List<String>>(it) }
            ?: return Result.failure()
        val headers = inputData.getString(KEY_HEADERS)
            ?.let { Json.decodeFromString<Map<String, String>>(it) }
            ?: emptyMap()

        val destDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "MangaWorld/${mangaTitle.sanitize()}/${chapterName.sanitize()}"
        ).also { it.mkdirs() }

        val request = DownloadRequest(
            chapterUrl = chapterUrl,
            chapterName = chapterName,
            mangaTitle = mangaTitle,
            imageUrls = imageUrls,
            headers = headers,
        )

        return try {
            executeDownload(
                client = httpClient,
                request = request,
                onProgress = { done, total ->
                    runBlocking { setProgress(workDataOf(KEY_PROGRESS_DONE to done, KEY_PROGRESS_TOTAL to total)) }
                },
                writeBytes = { index, bytes ->
                    File(destDir, "%03d.png".format(index)).writeBytes(bytes)
                },
            )
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry()
            else Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
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
