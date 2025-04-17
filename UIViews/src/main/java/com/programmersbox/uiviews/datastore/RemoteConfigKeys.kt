package com.programmersbox.uiviews.datastore

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.programmersbox.datastore.DataStoreHandling
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json

enum class RemoteConfigKeys(val key: String) {
    ShowGemini("show_gemini"),
    ExternalBridge("bridge"),
    ;

    suspend fun setDataStoreValue(
        dataStoreHandling: DataStoreHandling,
        otakuDataStoreHandling: OtakuDataStoreHandling,
        settingsHandling: SettingsHandling,
        remoteConfig: FirebaseRemoteConfig,
    ) {
        when (this) {
            ShowGemini -> otakuDataStoreHandling
                .showGemini
                .set(remoteConfig.getBoolean(key))

            ExternalBridge -> {
                runCatching {
                    val list = settingsHandling
                        .customUrls
                        .firstOrNull()
                        .orEmpty()

                    val json = Json.decodeFromString<List<String>>(remoteConfig.getString(key))

                    list
                        .filter { it !in json }
                        .forEach { settingsHandling.addCustomUrl(it) }
                }.onFailure { it.printStackTrace() }
            }
        }
    }
}