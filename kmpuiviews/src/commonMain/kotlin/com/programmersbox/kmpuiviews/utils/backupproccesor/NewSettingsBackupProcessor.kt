package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.Settings
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import kotlinx.coroutines.flow.firstOrNull
import okio.BufferedSink
import okio.BufferedSource

class NewSettingsBackupProcessor(
    private val newSettingsHandling: NewSettingsHandling,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "settings"

    override val key: String get() = fileName
    override val displayName: String get() = "App Settings"
    override val description: String? get() = "Preferences and app configuration"
    override val icon get() = Icons.Default.Settings

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val settings = newSettingsHandling.preferences.data.firstOrNull()
        settings?.encode(sink)
        return ProcessorResult(successCount = if (settings != null) 1 else 0)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult {
        newSettingsHandling.preferences.updateData { Settings.ADAPTER.decode(bufferedSource) }
        return ProcessorResult(successCount = 1)
    }

    override suspend fun currentSummary() = BackupDataSummary(
        details = listOf("Type" to "App settings"),
    )

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        sizeBytes = rawBytes?.size?.toLong(),
        details = listOf("Type" to "App settings"),
    )
}
