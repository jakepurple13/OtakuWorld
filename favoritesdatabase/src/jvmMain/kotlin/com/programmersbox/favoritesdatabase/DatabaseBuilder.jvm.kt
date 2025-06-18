package com.programmersbox.favoritesdatabase

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

actual class DatabaseBuilder {
    actual inline fun <reified T : RoomDatabase> build(name: String): RoomDatabase.Builder<T> {
        return getRoomDatabase(getDatabaseBuilder(name))
    }
}

inline fun <reified T : RoomDatabase> getDatabaseBuilder(
    name: String,
): RoomDatabase.Builder<T> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), name)
    return Room.databaseBuilder<T>(
        name = dbFile.absolutePath,
    ).setDriver(BundledSQLiteDriver())
}
