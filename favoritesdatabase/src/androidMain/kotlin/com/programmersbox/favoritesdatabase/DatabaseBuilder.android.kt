package com.programmersbox.favoritesdatabase

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

actual class DatabaseBuilder(
    val context: Context,
) {
    actual inline fun <reified T : RoomDatabase> build(name: String): RoomDatabase.Builder<T> {
        return getRoomDatabase(getDatabaseBuilder(context, name))
    }
}

inline fun <reified T : RoomDatabase> getDatabaseBuilder(
    context: Context,
    name: String,
): RoomDatabase.Builder<T> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(name)
    return Room.databaseBuilder<T>(
        context = appContext,
        name = dbFile.absolutePath
    )
}
