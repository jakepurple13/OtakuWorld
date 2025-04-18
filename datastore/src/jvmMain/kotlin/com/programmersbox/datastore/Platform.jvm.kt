package com.programmersbox.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioSerializer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

actual fun platform() = "Desktop"

actual fun <T> getDataStore(
    serializer: OkioSerializer<T>,
    producePath: () -> Path,
): DataStore<T> {
    return createDataStore(
        fileSystem = FileSystem.SYSTEM,
        producePath = { DATA_STORE_FILE_NAME.toPath() },
        serializer = serializer
    )
}