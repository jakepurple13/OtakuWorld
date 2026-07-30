package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryItem
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class HistoryBackupProcessor(
    private val historyDao: HistoryDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "history.json"

    override val key: String get() = fileName
    override val displayName: String get() = "History"
    override val description: String? get() = "Viewing/reading history"
    override val icon get() = Icons.Default.History

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val history = historyDao.getAllHistorySync()
        history.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = history.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<HistoryItem>>().restoreEachCatching(idOf = { it.searchText }) {
            historyDao.insertHistory(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = historyDao.getAllHistorySync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<HistoryItem>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
