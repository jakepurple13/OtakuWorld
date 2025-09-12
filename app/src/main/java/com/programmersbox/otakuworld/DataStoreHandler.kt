package com.programmersbox.otakuworld

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


val Context.dataStore: DataStore<Preferences> by preferencesDataStore("otakuworld")

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


@Composable
fun <T> DataStoreHandler<T>.asState() = rememberPreference(key, defaultValue)

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by remember {
        context.dataStore.data.map { it[key] ?: defaultValue }
    }.collectAsStateWithLifecycle(initialValue = defaultValue)

    return remember(state) {
        object : MutableState<T> {
            override var value: T
                get() = state
                set(value) {
                    coroutineScope.launch {
                        context.dataStore.edit { it[key] = value }
                    }
                }

            override fun component1() = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}