package com.programmersbox.datastore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

lateinit var otakuDataStore: DataStore<Preferences>

class DataStoreSettings(
    producePath: (String) -> String,
) {
    init {
        if (!::otakuDataStore.isInitialized)
            otakuDataStore = PreferenceDataStoreFactory.createWithPath(
                produceFile = { producePath(DATASTORE_FILE_NAME).toPath() },
            )
    }

    companion object {
        const val DATASTORE_FILE_NAME = "otaku.preferences_pb"
    }
}

class DataStoreHandler<T>(
    internal val key: Preferences.Key<T>,
    internal val defaultValue: T,
) {
    fun asFlow() = otakuDataStore.data.map { it[key] ?: defaultValue }

    suspend fun get() = asFlow().firstOrNull() ?: defaultValue

    suspend fun getOrNull() = asFlow().firstOrNull()

    suspend fun set(value: T) {
        otakuDataStore.edit { it[key] = value }
    }

    suspend fun clear() {
        otakuDataStore.edit { it.remove(key) }
    }
}

class DataStoreHandlerObject<T, R>(
    internal val key: Preferences.Key<T>,
    internal val defaultValue: R,
    internal val mapToType: (T) -> R?,
    internal val mapToKey: (R) -> T,
) {
    fun asFlow() = otakuDataStore
        .data
        .map { it[key]?.let(mapToType) ?: defaultValue }

    suspend fun get() = asFlow().firstOrNull() ?: defaultValue

    suspend fun getOrNull() = asFlow().firstOrNull()

    suspend fun set(value: R) {
        otakuDataStore.edit { it[key] = value.let(mapToKey) }
    }

    suspend fun clear() {
        otakuDataStore.edit { it.remove(key) }
    }
}

class DataStoreHandlerNullable<T>(
    internal val key: Preferences.Key<T>,
) {
    fun asFlow() = otakuDataStore.data.map { it[key] }

    suspend fun getOrNull() = asFlow().firstOrNull()

    suspend fun set(value: T?) {
        otakuDataStore.edit {
            if (value == null) {
                it.remove(key)
            } else {
                it[key] = value
            }
        }
    }

    suspend fun clear() {
        otakuDataStore.edit { it.remove(key) }
    }
}

@Composable
fun <T> DataStoreHandler<T>.asState() = rememberPreference(key, defaultValue)

@Composable
fun <T, R> DataStoreHandlerObject<T, R>.asState() = rememberPreference(key, mapToType, mapToKey, defaultValue)

@Composable
fun <T> DataStoreHandlerNullable<T>.asState(): MutableState<T?> = rememberPreferenceNullable(key, null)