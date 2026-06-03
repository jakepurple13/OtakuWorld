package com.programmersbox.kmpuiviews.utils.backupproccesor

import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.Settings
import kotlinx.coroutines.flow.firstOrNull
import okio.BufferedSink
import okio.BufferedSource

class NewSettingsBackupProcessor(
    private val newSettingsHandling: NewSettingsHandling,
) : BackupProcessor() {
    override val fileName: String
        get() = "settings"

    override suspend fun backup(sink: BufferedSink) {
        newSettingsHandling
            .preferences
            .data
            .firstOrNull()
            ?.encode(sink)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        newSettingsHandling
            .preferences
            .updateData { Settings.ADAPTER.decode(bufferedSource) }
    }
}