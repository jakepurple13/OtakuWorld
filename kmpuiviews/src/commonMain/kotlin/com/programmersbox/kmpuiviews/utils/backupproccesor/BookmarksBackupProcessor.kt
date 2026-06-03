package com.programmersbox.kmpuiviews.utils.backupproccesor

import com.programmersbox.favoritesdatabase.BookmarkDao
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import okio.BufferedSink
import okio.BufferedSource

class BookmarksBackupProcessor(
    private val bookmarkDao: BookmarkDao,
) : BackupProcessor() {
    override val fileName: String
        get() = "bookmarks.json"

    override suspend fun backup(sink: BufferedSink) {
        bookmarkDao
            .getAllBookmarksSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json.fromJson<List<BookmarkedChapter>>().forEach { bookmarkDao.insertBookmark(it) }
    }
}