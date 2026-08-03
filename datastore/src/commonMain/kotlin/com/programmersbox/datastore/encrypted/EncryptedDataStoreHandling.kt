package com.programmersbox.datastore.encrypted

class EncryptedDataStoreHandling(
    dataStoreFactory: EncryptedDataStoreFactory,
) {
    val dataStore by lazy {
        dataStoreFactory.create()
    }
}