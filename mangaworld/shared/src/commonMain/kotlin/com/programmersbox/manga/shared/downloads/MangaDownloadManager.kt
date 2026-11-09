package com.programmersbox.manga.shared.downloads

import com.programmersbox.kmpmodels.KmpChapterModel
import kotlinx.coroutines.flow.Flow


internal const val CONCURRENT_DOWNLOADS = 3
sealed interface DownloadState {
    data object Queued : DownloadState
    data class Downloading(val imagesDownloaded: Int, val totalImages: Int) : DownloadState
    data object Completed : DownloadState
    data class Failed(val reason: String) : DownloadState
    data object Cancelled : DownloadState
}

data class ChapterDownloadProgress(
    val chapterUrl: String,
    val chapterName: String,
    val mangaTitle: String,
    val state: DownloadState,
)

/**
 * Queued or downloading chapters, one per url. Re-queuing a chapter that is still running can
 * report it twice (WorkManager keeps the running and the appended work), and the Downloads list
 * keys rows by url.
 */
fun List<ChapterDownloadProgress>.inProgressRows(): List<ChapterDownloadProgress> =
    filter { it.state is DownloadState.Queued || it.state is DownloadState.Downloading }
        .distinctBy { it.chapterUrl }

expect class MangaDownloadManager {
    fun downloadChapter(chapter: KmpChapterModel, mangaTitle: String)
    fun downloadChapters(chapters: List<KmpChapterModel>, mangaTitle: String)
    fun cancelDownload(chapterUrl: String)
    fun cancelAll()
    fun getDownloadedChapterPath(chapter: KmpChapterModel, mangaTitle: String): String?
    fun observeDownloads(): Flow<List<ChapterDownloadProgress>>
    fun deleteChapter(chapter: KmpChapterModel, mangaTitle: String)
}
