package com.programmersbox.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

actual fun platform() = "Android"

actual fun getDataStore(
    producePath: () -> Path,
): DataStore<Settings> {
    //val producePath = { content.filesDir.resolve("Settings").absolutePath.toPath() }

    return createDataStore(fileSystem = FileSystem.SYSTEM, producePath = producePath)
}

fun createProtobuf(context: Context) =
    getDataStore { context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath.toPath() }