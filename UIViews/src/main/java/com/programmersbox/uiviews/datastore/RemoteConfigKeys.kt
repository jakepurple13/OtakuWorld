package com.programmersbox.uiviews.datastore

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.recordFirebaseException
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
        newSettingsHandling: NewSettingsHandling,
        remoteConfig: FirebaseRemoteConfig,
    ) {
        when (this) {
            ShowGemini -> otakuDataStoreHandling
                .showGemini
                .set(remoteConfig.getBoolean(key))

            ExternalBridge -> {
                runCatching {
                    val list = newSettingsHandling
                        .customUrls
                        .firstOrNull()
                        .orEmpty()

                    val json = Json.decodeFromString<List<String>>(remoteConfig.getString(key))

                    json
                        .filter { it !in list }
                        .forEach { newSettingsHandling.addCustomUrl(it) }
                }.onFailure {
                    it.printStackTrace()
                    recordFirebaseException(it)
                }
            }
        }
    }
}