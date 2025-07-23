package com.programmersbox.favoritesdatabase

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Database(
    entities = [ExceptionItem::class],
    version = 1,
)
abstract class ExceptionDatabase : RoomDatabase() {

    abstract fun exceptionDao(): ExceptionDao

    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): ExceptionDatabase = databaseBuilder
            .build<ExceptionDatabase>("exceptions.db")
            .build()
    }

}

@Dao
interface ExceptionDao {

    @Query("SELECT * FROM ExceptionTable ORDER BY time DESC")
    fun getAllExceptions(): Flow<List<ExceptionItem>>

    @Query("SELECT COUNT(*) FROM ExceptionTable")
    fun getExceptionCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertException(model: ExceptionItem)

    @Delete
    suspend fun deleteException(model: ExceptionItem)

    @Query("DELETE FROM ExceptionTable")
    suspend fun deleteAll()

    @OptIn(ExperimentalTime::class)
    suspend fun insertException(message: Throwable) {
        insertException(
            ExceptionItem(
                time = Clock.System.now().toEpochMilliseconds(),
                message = message.stackTraceToString()
            )
        )
    }
}

@Entity(tableName = "ExceptionTable")
data class ExceptionItem(
    @PrimaryKey
    @ColumnInfo(name = "time")
    val time: Long,
    @ColumnInfo(name = "message")
    val message: String,
)
