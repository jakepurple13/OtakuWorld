package com.programmersbox.manga.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.programmersbox.datastore.mangasettings.MangaSettings
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import kotlinx.coroutines.flow.firstOrNull
import okio.BufferedSink
import okio.BufferedSource

class MangaNewSettingsBackupProcessor(
    private val mangaNewSettingsHandling: MangaNewSettingsHandling,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "manga_settings"

    override val key: String get() = fileName
    override val displayName: String get() = "Manga Settings"
    override val description: String? get() = "MangaWorld-specific preferences"
    override val icon get() = Icons.Default.Settings

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val settings = mangaNewSettingsHandling.preferences.data.firstOrNull()
        settings?.encode(sink)
        return ProcessorResult(successCount = if (settings != null) 1 else 0)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult {
        mangaNewSettingsHandling.preferences.updateData { MangaSettings.ADAPTER.decode(bufferedSource) }
        return ProcessorResult(successCount = 1)
    }

    override suspend fun currentSummary() = BackupDataSummary(
        details = listOf("Type" to "Manga settings"),
    )

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        sizeBytes = rawBytes?.size?.toLong(),
        details = listOf("Type" to "Manga settings"),
    )
}