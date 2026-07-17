package com.programmersbox.jsextensionloader

import com.programmersbox.datastore.DataStoreHandling

enum class ExtensionUpdateMode { AUTOMATIC, NOTIFY, DISABLED }

class JsExtensionUpdateSettings(
    private val dataStoreHandling: DataStoreHandling = DataStoreHandling(),
) {
    suspend fun getMode(): ExtensionUpdateMode {
        val ordinal = dataStoreHandling.jsExtensionUpdateMode.get()
        return ExtensionUpdateMode.entries.getOrElse(ordinal) { ExtensionUpdateMode.NOTIFY }
    }

    suspend fun setMode(mode: ExtensionUpdateMode) {
        dataStoreHandling.jsExtensionUpdateMode.set(mode.ordinal)
    }
}
