package com.programmersbox.uiviews.utils.datastore

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.programmersbox.uiviews.utils.dataStore
import com.programmersbox.uiviews.utils.rememberPreference
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class DataStoreHandler<T>(
    internal val key: Preferences.Key<T>,
    internal val defaultValue: T,
    private val context: Context,
) {
    fun asFlow() = context.dataStore.data.map { it[key] ?: defaultValue }

    suspend fun get() = asFlow().firstOrNull() ?: defaultValue

    suspend fun getOrNull() = asFlow().firstOrNull()

    suspend fun set(value: T) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(key) }
    }
}

class DataStoreHandlerObject<T, R>(
    internal val key: Preferences.Key<T>,
    internal val defaultValue: R,
    private val context: Context,
    internal val mapToType: (T) -> R?,
    internal val mapToKey: (R) -> T,
) {
    fun asFlow() = context.dataStore
        .data
        .map { it[key]?.let(mapToType) ?: defaultValue }

    suspend fun get() = asFlow().firstOrNull() ?: defaultValue

    suspend fun getOrNull() = asFlow().firstOrNull()

    suspend fun set(value: R) {
        context.dataStore.edit { it[key] = value.let(mapToKey) }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(key) }
    }
}

@Composable
fun <T> DataStoreHandler<T>.asState() = rememberPreference(key, defaultValue)

@Composable
fun <T, R> DataStoreHandlerObject<T, R>.asState() = rememberPreference(key, mapToType, mapToKey, defaultValue)