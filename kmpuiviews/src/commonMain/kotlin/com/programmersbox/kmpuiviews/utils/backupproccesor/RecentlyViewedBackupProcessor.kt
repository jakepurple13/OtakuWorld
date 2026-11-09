package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class RecentlyViewedBackupProcessor(
    private val historyDao: HistoryDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "recently_viewed.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Recently Viewed"
    override val description: String? get() = "Recently opened items"
    override val icon get() = Icons.Default.Visibility

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val recent = historyDao.getAllRecentlyViewedSync()
        recent.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = recent.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<RecentModel>>().restoreEachCatching(idOf = { it.title }) {
            historyDao.insertRecentlyViewed(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = historyDao.getAllRecentlyViewedSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<RecentModel>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
