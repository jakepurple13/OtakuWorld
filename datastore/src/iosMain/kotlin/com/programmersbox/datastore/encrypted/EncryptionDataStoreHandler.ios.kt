package com.programmersbox.datastore.encrypted

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.programmersbox.datastore.otakuDataStore

actual class EncryptedDataStoreFactory {
    actual fun create(): DataStore<Preferences> = otakuDataStore
}