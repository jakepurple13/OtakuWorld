package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import com.programmersbox.favoritesdatabase.IncognitoSource
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource

class IncognitoBackupProcessor(
    private val itemDao: ItemDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "incognito_sources.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Incognito Sources"
    override val description: String? get() = "Sources marked incognito"
    override val icon get() = Icons.Default.VisibilityOff

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val sources = itemDao.getAllIncognitoSourcesSync()
        sources.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = sources.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<IncognitoSource>>().restoreEachCatching(idOf = { it.name }) {
            itemDao.insertIncognitoSource(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = itemDao.getAllIncognitoSourcesSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<IncognitoSource>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
