package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UpdateDisabled
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.SkippedUpdateSource
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class SkippedUpdateSourcesBackupProcessor(
    private val itemDao: ItemDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "skipped_update_sources.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Skipped Update Sources"
    override val description: String? get() = "Extensions the update checker skips"
    override val icon get() = Icons.Default.UpdateDisabled

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val sources = itemDao.getAllSkippedUpdateSourcesSync()
        sources.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = sources.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<SkippedUpdateSource>>().restoreEachCatching(idOf = { it.source }) {
            itemDao.insertSkippedUpdateSources(listOf(it))
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = itemDao.getAllSkippedUpdateSourcesSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<SkippedUpdateSource>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
