package com.programmersbox.uiviews.datastore

import com.google.firebase.remoteconfig.FirebaseRemoteConfig

enum class RemoteConfigKeys(val key: String) {
    ShowGemini("show_gemini")
    ;

    suspend fun setDataStoreValue(
        dataStoreHandling: DataStoreHandling,
        remoteConfig: FirebaseRemoteConfig,
    ) {
        when (this) {
            ShowGemini -> dataStoreHandling
                .showGemini
                .set(remoteConfig.getBoolean(key))
        }
    }
}