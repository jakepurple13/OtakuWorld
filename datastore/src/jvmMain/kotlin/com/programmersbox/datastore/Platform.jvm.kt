package com.programmersbox.datastore

import androidx.datastore.core.DataStore
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

actual fun platform() = "Desktop"

actual fun getDataStore(
    producePath: () -> Path,
): DataStore<Settings> {
    return createDataStore(fileSystem = FileSystem.SYSTEM, producePath = { DATA_STORE_FILE_NAME.toPath() })
}