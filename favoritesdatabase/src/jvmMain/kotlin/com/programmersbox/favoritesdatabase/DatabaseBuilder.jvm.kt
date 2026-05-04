package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import ca.gosyer.appdirs.AppDirs
import java.io.File

actual class DatabaseBuilder(
    val appDirs: AppDirs,
) {
    actual inline fun <reified T : RoomDatabase> build(name: String): RoomDatabase.Builder<T> {
        return getRoomDatabase(
            getDatabaseBuilder(
                File(appDirs.getUserDataDir(), name)
            )
        )
    }
}

inline fun <reified T : RoomDatabase> getDatabaseBuilder(
    dbFile: File,
): RoomDatabase.Builder<T> {
    return Room.databaseBuilder<T>(
        name = dbFile.absolutePath,
    ).setDriver(BundledSQLiteDriver())
}
