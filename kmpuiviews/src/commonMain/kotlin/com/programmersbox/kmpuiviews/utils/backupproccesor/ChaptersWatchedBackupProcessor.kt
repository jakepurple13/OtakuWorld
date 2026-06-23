package com.programmersbox.kmpuiviews.utils.backupproccesor

import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.sharedtools.BackupProcessor
import okio.BufferedSink
import okio.BufferedSource

class ChaptersWatchedBackupProcessor(
    private val itemDao: ItemDao,
) : BackupProcessor() {
    override val fileName: String
        get() = "chapters_watched.json"

    override suspend fun backup(sink: BufferedSink) {
        itemDao
            .getAllChaptersSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json.fromJson<List<ChapterWatched>>().forEach { itemDao.insertChapter(it) }
    }
}