package com.programmersbox.datastore.encrypted

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class EncryptionDataStoreHandler<T>(
    internal val key: Preferences.Key<T>,
    internal val defaultValue: T,
    internal val dataStore: DataStore<Preferences>,
) {
    fun asFlow(): Flow<T> = dataStore.data.map { it[key] ?: defaultValue }

    suspend fun get() = asFlow().firstOrNull() ?: defaultValue

    suspend fun getOrNull() = asFlow().firstOrNull()

    suspend fun set(value: T) {
        dataStore.edit { it[key] = value }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }
}

expect class EncryptedDataStoreFactory {
    fun create(): DataStore<Preferences>
}
