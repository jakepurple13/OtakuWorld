package com.programmersbox.anime.shared.subtitles

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreSubtitleSettingsRepository(context: Context) : SubtitleSettingsRepository {
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile("subtitle_settings") }
    )
    private val key = stringPreferencesKey("subtitle_style")

    override val style: Flow<SubtitleStyle> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { decodeSubtitleStyle(it[key]) }

    override suspend fun update(transform: (SubtitleStyle) -> SubtitleStyle) {
        dataStore.edit { prefs ->
            prefs[key] = encodeSubtitleStyle(transform(decodeSubtitleStyle(prefs[key])).coerced())
        }
    }

    override suspend fun reset() {
        dataStore.edit { it[key] = encodeSubtitleStyle(SubtitleStyle()) }
    }
}
