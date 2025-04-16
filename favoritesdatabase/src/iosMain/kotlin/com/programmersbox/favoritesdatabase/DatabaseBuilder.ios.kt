package com.programmersbox.favoritesdatabase

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class DatabaseBuilder {
    actual inline fun <reified T : RoomDatabase> build(name: String): RoomDatabase.Builder<T> {
        return getRoomDatabase(getDatabaseBuilder(name))
    }
}

inline fun <reified T : RoomDatabase> getDatabaseBuilder(
    name: String,
): RoomDatabase.Builder<T> {
    val dbFilePath = documentDirectory() + "/$name"
    return Room.databaseBuilder<T>(
        name = dbFilePath,
    )
}


@OptIn(ExperimentalForeignApi::class)
fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}