package com.programmersbox.favoritesdatabase

import androidx.room3.ColumnInfo
import androidx.room3.ConstructedBy
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
    version = 3,
)
@ConstructedBy(ExceptionDatabaseConstructor::class)
abstract class ExceptionDatabase : RoomDatabase() {

    abstract fun exceptionDao(): ExceptionDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE `ExceptionTable` ADD COLUMN `supabase_id` TEXT DEFAULT ''")
                connection.execSQL("ALTER TABLE `ExceptionTable` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `ExceptionTable` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `ExceptionTable` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `ExceptionTable` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
            }
        }

        // The primary key moves from `time` to a generated `id`; SQLite can't change a primary key in place.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ExceptionTable_new` (`time` INTEGER NOT NULL, `message` TEXT NOT NULL, " +
                        "`supabase_id` TEXT DEFAULT '', `created_at` INTEGER NOT NULL DEFAULT 0, `updated_at` INTEGER NOT NULL DEFAULT 0, " +
                        "`is_deleted` INTEGER NOT NULL DEFAULT 0, `is_dirty` INTEGER NOT NULL DEFAULT 1, " +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)"
                )
                connection.execSQL(
                    "INSERT INTO `ExceptionTable_new` (`time`, `message`, `supabase_id`, `created_at`, `updated_at`, `is_deleted`, `is_dirty`) " +
                        "SELECT `time`, `message`, `supabase_id`, `created_at`, `updated_at`, `is_deleted`, `is_dirty` FROM `ExceptionTable` ORDER BY `time`"
                )
                connection.execSQL("DROP TABLE `ExceptionTable`")
                connection.execSQL("ALTER TABLE `ExceptionTable_new` RENAME TO `ExceptionTable`")
            }
        }

        internal val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

        fun getInstance(databaseBuilder: DatabaseBuilder): ExceptionDatabase = databaseBuilder
            .build<ExceptionDatabase>("exceptions.db")
            .addMigrations(*MIGRATIONS)
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
    @ColumnInfo(name = "time")
    val time: Long,
    @ColumnInfo(name = "message")
    val message: String,
    @ColumnInfo(name = "supabase_id", defaultValue = "") val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1") val isDirty: Boolean = true,
    // Generated key, so exceptions logged in the same millisecond don't replace each other.
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
)
