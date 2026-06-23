package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.programmersbox.datastore.otakuDataStore
import com.programmersbox.kmpuiviews.utils.BackupSettings
import com.programmersbox.sharedtools.BackupProcessor
import kotlinx.coroutines.flow.firstOrNull
import okio.BufferedSink
import okio.BufferedSource

class BackupSettingsProcessor : BackupProcessor() {
    override val fileName: String
        get() = "backupsettings.json"

    override suspend fun backup(sink: BufferedSink) {
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
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
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
    }
}