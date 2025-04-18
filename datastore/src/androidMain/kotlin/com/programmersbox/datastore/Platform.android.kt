package com.programmersbox.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioSerializer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

actual fun platform() = "Android"

actual fun <T> getDataStore(
    serializer: OkioSerializer<T>,
    producePath: () -> Path,
): DataStore<T> {
    //val producePath = { content.filesDir.resolve("Settings").absolutePath.toPath() }

    return createDataStore(
        fileSystem = FileSystem.SYSTEM,
        producePath = producePath,
        serializer = serializer
    )
}

fun <T> createProtobuf(
    context: Context,
    serializer: OkioSerializer<T>,
    fileName: String = DATA_STORE_FILE_NAME,
) = getDataStore(
    serializer = serializer,
    producePath = { context.filesDir.resolve(fileName).absolutePath.toPath() }
)