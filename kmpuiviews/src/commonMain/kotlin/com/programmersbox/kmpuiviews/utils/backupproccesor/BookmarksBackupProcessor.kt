package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import com.programmersbox.favoritesdatabase.BookmarkDao
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import okio.BufferedSink
import okio.BufferedSource

class BookmarksBackupProcessor(
    private val bookmarkDao: BookmarkDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "bookmarks.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Bookmarks"
    override val description: String? get() = "Bookmarked chapters"
    override val icon get() = Icons.Default.Bookmark

    override suspend fun backup(sink: BufferedSink) {
        bookmarkDao
            .getAllBookmarksSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json.fromJson<List<BookmarkedChapter>>().forEach { bookmarkDao.insertBookmark(it) }
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = bookmarkDao.getAllBookmarksSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<BookmarkedChapter>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
