package com.programmersbox.favoritesdatabase

import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

fun <T : RoomDatabase> getRoomDatabase(
    builder: RoomDatabase.Builder<T>,
): RoomDatabase.Builder<T> {
    return builder.setQueryCoroutineContext(Dispatchers.IO)
}

fun <T : RoomDatabase> createDatabase(builder: RoomDatabase.Builder<T>) = getRoomDatabase(builder)

expect class DatabaseBuilder {
    inline fun <reified T : RoomDatabase> build(name: String): RoomDatabase.Builder<T>
}
