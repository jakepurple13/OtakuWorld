package com.programmersbox.datastore

import androidx.datastore.core.DataStore
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual fun platform() = "iOS"

actual fun getDataStore(
    serializer: SettingsSerializer,
    producePath: () -> Path,
): DataStore<Settings> {
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