package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reorder
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.SourceOrder
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class SourceOrderBackupProcessor(
    private val itemDao: ItemDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "source_order.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Source Order"
    override val description: String? get() = "Custom source ordering"
    override val icon get() = Icons.Default.Reorder

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val order = itemDao.getSourceOrderSync()
        order.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = order.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<SourceOrder>>().restoreEachCatching(idOf = { it.name }) {
            itemDao.insertSourceOrder(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = itemDao.getSourceOrderSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<SourceOrder>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
