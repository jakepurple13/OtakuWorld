package com.programmersbox.favoritesdatabase

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
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
