package com.programmersbox.manga.shared.downloads

import com.programmersbox.kmpmodels.KmpChapterModel
import kotlinx.coroutines.flow.Flow

actual class MangaDownloadManager {
    actual fun downloadChapter(chapter: KmpChapterModel, mangaTitle: String) {
    }

    actual fun downloadChapters(chapters: List<KmpChapterModel>, mangaTitle: String) {
    }

    actual fun cancelDownload(chapterUrl: String) {
    }

    actual fun cancelAll() {
    }

    actual fun getDownloadedChapterPath(chapter: KmpChapterModel, mangaTitle: String): String? {
        TODO("Not yet implemented")
    }

    actual fun observeDownloads(): Flow<List<ChapterDownloadProgress>> {
        TODO("Not yet implemented")
    }

    actual fun deleteChapter(chapter: KmpChapterModel, mangaTitle: String) {
    }
}