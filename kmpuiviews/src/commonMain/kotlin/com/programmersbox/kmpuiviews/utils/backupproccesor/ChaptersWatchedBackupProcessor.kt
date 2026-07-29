package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class ChaptersWatchedBackupProcessor(
    private val itemDao: ItemDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "chapters_watched.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Chapters Watched"
    override val description: String? get() = "Read/watched chapter markers"
    override val icon get() = Icons.Default.CheckCircle

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val chapters = itemDao.getAllChaptersSync()
        chapters.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = chapters.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<ChapterWatched>>().restoreEachCatching(idOf = { it.name }) {
            itemDao.insertChapter(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = itemDao.getAllChaptersSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<ChapterWatched>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
