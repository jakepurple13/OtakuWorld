package com.programmersbox.datastore.encrypted

class EncryptedDataStoreHandling(
    dataStoreFactory: EncryptedDataStoreFactory,
) {
    val dataStore = dataStoreFactory.create()
}