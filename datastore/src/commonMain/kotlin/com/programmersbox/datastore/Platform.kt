package com.programmersbox.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import okio.FileSystem
import okio.Path

expect fun platform(): String

internal const val DATA_STORE_FILE_NAME = "Settings.preferences_pb"

expect fun getDataStore(
    producePath: () -> Path,
): DataStore<Settings>

fun createDataStore(
    fileSystem: FileSystem,
    producePath: () -> Path,
): DataStore<Settings> = DataStoreFactory.create(
    storage = OkioStorage(
        fileSystem = fileSystem,
        producePath = producePath,
        serializer = SettingsSerializer,
    ),
)