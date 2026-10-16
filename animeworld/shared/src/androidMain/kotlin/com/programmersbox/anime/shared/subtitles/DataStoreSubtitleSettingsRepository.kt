package com.programmersbox.anime.shared.subtitles

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSubtitleSettingsRepository(context: Context) : SubtitleSettingsRepository {
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile("subtitle_settings") }
    )
    private val key = stringPreferencesKey("subtitle_style")

    override val style: Flow<SubtitleStyle> = dataStore.data.map { decodeSubtitleStyle(it[key]) }

    override suspend fun update(transform: (SubtitleStyle) -> SubtitleStyle) {
        dataStore.edit { prefs ->
            prefs[key] = encodeSubtitleStyle(transform(decodeSubtitleStyle(prefs[key])).coerced())
        }
    }

    override suspend fun reset() {
        dataStore.edit { it[key] = encodeSubtitleStyle(SubtitleStyle()) }
    }
}
