package com.programmersbox.datastore.encrypted

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.programmersbox.datastore.InternalDataStoreHandler

class EncryptionDataStoreHandler<T>(
    key: Preferences.Key<T>,
    defaultValue: T,
    dataStore: DataStore<Preferences>,
) : InternalDataStoreHandler<T>(key, defaultValue, dataStore)

expect class EncryptedDataStoreFactory {
    fun create(): DataStore<Preferences>
}
