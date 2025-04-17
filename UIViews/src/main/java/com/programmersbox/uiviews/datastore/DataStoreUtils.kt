package com.programmersbox.uiviews.datastore

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.materialkolor.PaletteStyle
import com.programmersbox.datastore.otakuDataStore
import com.programmersbox.datastore.rememberPreference
import com.programmersbox.uiviews.presentation.details.PaletteSwatchType


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

suspend fun <T> Context.updatePref(key: Preferences.Key<T>, value: T) = otakuDataStore.edit { it[key] = value }