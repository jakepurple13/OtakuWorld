package com.programmersbox.kmpuiviews.translation

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.DataStoreHandler

class DesktopTranslationSettings(
    @Suppress("unused") appDirs: AppDirs,
) {
    /** [EngineKind] name. */
    val engine = DataStoreHandler(
        key = stringPreferencesKey("translationEngine"),
        defaultValue = EngineKind.Bergamot.name
    )

    val ollamaUrl = DataStoreHandler(
        key = stringPreferencesKey("translationOllamaUrl"),
        defaultValue = "http://localhost:11434"
    )

    val ollamaModel = DataStoreHandler(
        key = stringPreferencesKey("translationOllamaModel"),
        defaultValue = ""
    )

    /** "folder" or "server". */
    val customMode = DataStoreHandler(
        key = stringPreferencesKey("translationCustomMode"),
        defaultValue = "folder"
    )

    /** Absolute paths. */
    val customFolders = DataStoreHandler(
        key = stringSetPreferencesKey("translationCustomFolders"),
        defaultValue = emptySet<String>()
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
