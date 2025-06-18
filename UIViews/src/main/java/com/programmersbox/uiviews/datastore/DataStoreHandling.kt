package com.programmersbox.uiviews.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.programmersbox.datastore.DataStoreHandler

class OtakuDataStoreHandling {
    val showGemini = DataStoreHandler(
        key = booleanPreferencesKey(RemoteConfigKeys.ShowGemini.key),
        defaultValue = false
    )
}