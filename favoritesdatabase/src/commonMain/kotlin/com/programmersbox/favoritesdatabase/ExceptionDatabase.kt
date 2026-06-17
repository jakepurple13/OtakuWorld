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
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Database(
    entities = [ExceptionItem::class],
    version = 2,
)
abstract class ExceptionDatabase : RoomDatabase() {

    abstract fun exceptionDao(): ExceptionDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE `ExceptionTable` ADD COLUMN `supabase_id` TEXT NOT NULL DEFAULT ''")
                connection.execSQL("ALTER TABLE `ExceptionTable` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `ExceptionTable` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `ExceptionTable` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `ExceptionTable` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(databaseBuilder: DatabaseBuilder): ExceptionDatabase = databaseBuilder
            .build<ExceptionDatabase>("exceptions.db")
            .addMigrations(MIGRATION_1_2)
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
    @ColumnInfo(name = "supabase_id", defaultValue = "") val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1") val isDirty: Boolean = true,
)
