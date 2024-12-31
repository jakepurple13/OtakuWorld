package com.programmersbox.uiviews.datastore

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.PaletteStyle
import com.programmersbox.uiviews.presentation.details.PaletteSwatchType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    "otakuworld",
    //produceMigrations = { listOf(SharedPreferencesMigration(it, "HelpfulUtils")) }
)

@Composable
fun rememberSwatchType() = rememberPreference(
    key = stringPreferencesKey("swatchType"),
    mapToType = { runCatching { PaletteSwatchType.valueOf(it) }.getOrDefault(PaletteSwatchType.Vibrant) },
    mapToKey = { it.name },
    defaultValue = PaletteSwatchType.Vibrant
)

@Composable
fun rememberSwatchStyle() = rememberPreference(
    key = stringPreferencesKey("swatchStyle"),
    mapToType = { runCatching { PaletteStyle.valueOf(it) }.getOrDefault(PaletteStyle.TonalSpot) },
    mapToKey = { it.name },
    defaultValue = PaletteStyle.TonalSpot
)

suspend fun <T> Context.updatePref(key: Preferences.Key<T>, value: T) = dataStore.edit { it[key] = value }

@Composable
fun rememberHistorySave() = rememberPreference(
    key = intPreferencesKey("history_save"),
    defaultValue = 50
)

@Composable
fun rememberFloatingNavigation() = rememberPreference(
    key = booleanPreferencesKey("floatingNavigation"),
    defaultValue = true
)

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): MutableState<T> {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val state by remember {
        context.dataStore.data
            .map { it[key] ?: defaultValue }
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

@Composable
fun <T, R> rememberPreference(
    key: Preferences.Key<T>,
    mapToType: (T) -> R?,
    mapToKey: (R) -> T,
    defaultValue: R,
): MutableState<R> {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val state by remember {
        context.dataStore
            .data
            .mapNotNull { it[key]?.let(mapToType) ?: defaultValue }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(defaultValue)

    return remember(state) {
        object : MutableState<R> {
            override var value: R
                get() = state
                set(value) {
                    coroutineScope.launch {
                        context.dataStore.edit { it[key] = value.let(mapToKey) }
                    }
                }

            override fun component1() = value
            override fun component2(): (R) -> Unit = { value = it }
        }
    }
}