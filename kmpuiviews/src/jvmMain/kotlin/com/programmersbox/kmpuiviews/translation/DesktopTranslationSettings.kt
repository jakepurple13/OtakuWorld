package com.programmersbox.kmpuiviews.translation

import androidx.datastore.preferences.core.stringPreferencesKey
import com.programmersbox.datastore.DataStoreHandler

class DesktopTranslationSettings {
    /** [EngineKind] name. An unknown stored value (e.g. the removed "Bergamot") reads as Ollama. */
    val engine = DataStoreHandler(
        key = stringPreferencesKey("translationEngine"),
        defaultValue = EngineKind.Ollama.name
    )

    val ollamaUrl = DataStoreHandler(
        key = stringPreferencesKey("translationOllamaUrl"),
        defaultValue = "http://localhost:11434"
    )

    val ollamaModel = DataStoreHandler(
        key = stringPreferencesKey("translationOllamaModel"),
        defaultValue = ""
    )

    val customServerUrl = DataStoreHandler(
        key = stringPreferencesKey("translationCustomServerUrl"),
        defaultValue = "http://localhost:1234/v1"
    )

    val customServerModel = DataStoreHandler(
        key = stringPreferencesKey("translationCustomServerModel"),
        defaultValue = ""
    )

    val customServerKey = DataStoreHandler(
        key = stringPreferencesKey("translationCustomServerKey"),
        defaultValue = ""
    )
}
