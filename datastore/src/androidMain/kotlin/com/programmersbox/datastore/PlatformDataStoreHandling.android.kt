package com.programmersbox.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey

actual class PlatformDataStoreHandling {

    val useStrongSecurity = DataStoreHandler(
        key = booleanPreferencesKey("useStrongSecurity"),
        defaultValue = true
    )

    val useDeviceCredentials = DataStoreHandler(
        key = booleanPreferencesKey("useDeviceCredentials"),
        defaultValue = true
    )

}