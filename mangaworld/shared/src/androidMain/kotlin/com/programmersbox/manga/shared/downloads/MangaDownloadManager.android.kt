package com.programmersbox.manga.shared.downloads

import com.programmersbox.kmpmodels.KmpChapterModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class MangaDownloadManager {
    private val _downloads = MutableStateFlow<List<ChapterDownloadProgress>>(emptyList())

    actual fun downloadChapter(chapter: KmpChapterModel, mangaTitle: String) {
        // TODO: implement Android download logic
    }

    actual fun downloadChapters(chapters: List<KmpChapterModel>, mangaTitle: String) {
        chapters.forEach { downloadChapter(it, mangaTitle) }
    }

    actual fun cancelDownload(chapterUrl: String) {
        // TODO: implement Android cancel logic
    }

    actual fun cancelAll() {
        // TODO: implement Android cancel-all logic
    }

    actual fun getDownloadedChapterPath(chapter: KmpChapterModel, mangaTitle: String): String? = null

    actual fun observeDownloads(): Flow<List<ChapterDownloadProgress>> = _downloads.asStateFlow()

    actual fun deleteChapter(chapter: KmpChapterModel, mangaTitle: String) {
        // TODO: implement Android delete logic
    }
}
