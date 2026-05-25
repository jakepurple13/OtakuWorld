package com.programmersbox.manga.shared.downloads

import android.content.Context
import android.os.Environment
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.programmersbox.kmpmodels.KmpChapterModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

actual class MangaDownloadManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val rootDir: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "MangaWorld"
        )

    actual fun downloadChapter(chapter: KmpChapterModel, mangaTitle: String) {
        scope.launch {
            val storages = chapter.getChapterInfo().firstOrNull() ?: return@launch
            val urls = storages.mapNotNull { it.link }
            if (urls.isEmpty()) return@launch
            val headers = storages
                .flatMap { it.headers.entries }
                .associate { it.key to it.value }

            val inputData = workDataOf(
                DownloadChapterWorker.KEY_MANGA_TITLE to mangaTitle,
                DownloadChapterWorker.KEY_CHAPTER_NAME to chapter.name,
                DownloadChapterWorker.KEY_CHAPTER_URL to chapter.url,
                DownloadChapterWorker.KEY_IMAGE_URLS to Json.encodeToString<List<String>>(urls),
                DownloadChapterWorker.KEY_HEADERS to Json.encodeToString<Map<String, String>>(headers),
            )

            val workRequest = OneTimeWorkRequestBuilder<DownloadChapterWorker>()
                .setInputData(inputData)
                .addTag(DownloadChapterWorker.DOWNLOAD_TAG)
                .addTag(chapter.url)
                .build()

            workManager
                .beginUniqueWork(
                    DownloadChapterWorker.DOWNLOAD_QUEUE,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    workRequest,
                )
                .enqueue()
        }
    }

    actual fun downloadChapters(chapters: List<KmpChapterModel>, mangaTitle: String) {
        chapters.forEach { downloadChapter(it, mangaTitle) }
    }

    actual fun cancelDownload(chapterUrl: String) {
        workManager.cancelAllWorkByTag(chapterUrl)
    }

    actual fun cancelAll() {
        workManager.cancelAllWorkByTag(DownloadChapterWorker.DOWNLOAD_TAG)
    }

    actual fun getDownloadedChapterPath(chapter: KmpChapterModel, mangaTitle: String): String? {
        val dir = File(rootDir, "${mangaTitle.sanitize()}/${chapter.name.sanitize()}")
        return if (dir.exists() && dir.listFiles()?.isNotEmpty() == true) dir.absolutePath else null
    }

    actual fun observeDownloads(): Flow<List<ChapterDownloadProgress>> =
        workManager.getWorkInfosByTagFlow(DownloadChapterWorker.DOWNLOAD_TAG).map { infos ->
            infos.map { info ->
                // WorkManager 2.11+ does not expose inputData on WorkInfo.
                // The chapter URL is stored as a tag; name and title come from outputData
                // (set by DownloadChapterWorker on completion/failure) or are empty strings
                // when the work is still queued/running.
                val chapterUrl = info.tags
                    .firstOrNull { it != DownloadChapterWorker.DOWNLOAD_TAG } ?: ""
                val chapterName = info.outputData.getString(DownloadChapterWorker.KEY_CHAPTER_NAME)
                    ?: info.progress.getString(DownloadChapterWorker.KEY_CHAPTER_NAME)
                    ?: ""
                val mangaTitle = info.outputData.getString(DownloadChapterWorker.KEY_MANGA_TITLE)
                    ?: info.progress.getString(DownloadChapterWorker.KEY_MANGA_TITLE)
                    ?: ""
                ChapterDownloadProgress(
                    chapterUrl = chapterUrl,
                    chapterName = chapterName,
                    mangaTitle = mangaTitle,
                    state = when (info.state) {
                        WorkInfo.State.ENQUEUED,
                        WorkInfo.State.BLOCKED -> DownloadState.Queued
                        WorkInfo.State.RUNNING -> DownloadState.Downloading(
                            imagesDownloaded = info.progress.getInt(DownloadChapterWorker.KEY_PROGRESS_DONE, 0),
                            totalImages = info.progress.getInt(DownloadChapterWorker.KEY_PROGRESS_TOTAL, 0),
                        )
                        WorkInfo.State.SUCCEEDED -> DownloadState.Completed
                        WorkInfo.State.FAILED -> DownloadState.Failed(
                            info.outputData.getString(DownloadChapterWorker.KEY_ERROR) ?: "Unknown"
                        )
                        WorkInfo.State.CANCELLED -> DownloadState.Cancelled
                    },
                )
            }
        }

    actual fun deleteChapter(chapter: KmpChapterModel, mangaTitle: String) {
        File(rootDir, "${mangaTitle.sanitize()}/${chapter.name.sanitize()}").deleteRecursively()
    }
}
