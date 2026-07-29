package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import com.programmersbox.favoritesdatabase.DictionaryDao
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class DictionaryBackupProcessor(
    private val dictionaryDao: DictionaryDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String = "dictionary.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Dictionary"
    override val description: String? get() = "Dictionary Entries"
    override val icon get() = Icons.Default.MenuBook

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val entries = dictionaryDao.getAllSync()
        entries.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = entries.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<DictionaryEntry>>().restoreEachCatching(idOf = { it.term }) {
            dictionaryDao.insert(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = dictionaryDao.getAllSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<DictionaryEntry>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
