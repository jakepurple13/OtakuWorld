package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reorder
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.SourceOrder
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
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

    override suspend fun backup(sink: BufferedSink) {
        itemDao
            .getSourceOrderSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json
            .fromJson<List<SourceOrder>>()
            .forEach { itemDao.insertSourceOrder(it) }
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = itemDao.getSourceOrderSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<SourceOrder>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
