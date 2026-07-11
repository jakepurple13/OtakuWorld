package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryItem
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
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

    override suspend fun backup(sink: BufferedSink) {
        historyDao
            .getAllHistorySync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json.fromJson<List<HistoryItem>>().forEach { historyDao.insertHistory(it) }
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = historyDao.getAllHistorySync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<HistoryItem>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
