package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.programmersbox.datastore.otakuDataStore
import com.programmersbox.kmpuiviews.utils.BackupSettings
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import kotlinx.coroutines.flow.firstOrNull
import okio.BufferedSink
import okio.BufferedSource

class BackupSettingsProcessor : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "backupsettings.json"

    override val key: String get() = fileName
    override val displayName: String get() = "General Preferences"
    override val description: String? get() = "Raw app preference key-value pairs"
    override val icon get() = Icons.Default.Settings

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val map = otakuDataStore.data.firstOrNull()?.asMap()!!

        BackupSettings(
            map
                .filter { it.value is String }
                .mapKeys { it.key.name }
                .mapValues { it.value.toString() },
            map
                .filter { it.value is Int }
                .mapKeys { it.key.name }
                .mapValues { it.value as Int },
            map
                .filter { it.value is Long }
                .mapKeys { it.key.name }
                .mapValues { it.value as Long },
            map
                .filter { it.value is Boolean }
                .mapKeys { it.key.name }
                .mapValues { it.value as Boolean },
            map
                .filter { it.value is Double }
                .mapKeys { it.key.name }
                .mapValues { it.value as Double },
            map
                .filter { it.value is ByteArray }
                .mapKeys { it.key.name }
                .mapValues { it.value as ByteArray },
        )
            .toJson()
            .let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = 1)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult {
        val backupSettings = json.fromJson<BackupSettings>()
        with(backupSettings) {
            otakuDataStore.edit { p ->
                stringSettings.forEach {
                    p[stringPreferencesKey(it.key)] = it.value
                }
                intSettings.forEach {
                    p[intPreferencesKey(it.key)] = it.value
                }
                longSettings.forEach {
                    p[longPreferencesKey(it.key)] = it.value
                }
                booleanSettings.forEach {
                    p[booleanPreferencesKey(it.key)] = it.value
                }
                doubleSettings.forEach {
                    p[doublePreferencesKey(it.key)] = it.value
                }
                byteArraySettings.forEach {
                    p[byteArrayPreferencesKey(it.key)] = it.value
                }
            }
        }
        return ProcessorResult(successCount = 1)
    }

    private fun BackupSettings.entryCount() =
        stringSettings.size + intSettings.size + longSettings.size +
            booleanSettings.size + doubleSettings.size + byteArraySettings.size

    override suspend fun currentSummary(): BackupDataSummary {
        val map = otakuDataStore.data.firstOrNull()?.asMap().orEmpty()
        return BackupDataSummary(details = listOf("Preferences" to "${map.size} entries"))
    }

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?): BackupDataSummary {
        val count = json?.let { runCatching { it.fromJson<BackupSettings>().entryCount() }.getOrNull() }
        return BackupDataSummary(
            sizeBytes = rawBytes?.size?.toLong(),
            details = listOf("Preferences" to "${count ?: 0} entries"),
        )
    }
}
