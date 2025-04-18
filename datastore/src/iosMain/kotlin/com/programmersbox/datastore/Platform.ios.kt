package com.programmersbox.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual fun platform() = "iOS"

actual fun <T> getDataStore(
    serializer: OkioSerializer<T>,
    producePath: () -> Path,
): DataStore<T> {
    @OptIn(ExperimentalForeignApi::class)
    val producePath = {
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        requireNotNull(documentDirectory).path + "/Settings"
    }

    return createDataStore(
        fileSystem = FileSystem.SYSTEM,
        producePath = { producePath().toPath() },
        serializer = serializer
    )
}