package com.programmersbox.uiviews.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.programmersbox.datastore.otakuDataStore


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    "otakuworld",
    //produceMigrations = { listOf(SharedPreferencesMigration(it, "HelpfulUtils")) }
)

suspend fun <T> Context.updatePref(key: Preferences.Key<T>, value: T) = otakuDataStore.edit { it[key] = value }