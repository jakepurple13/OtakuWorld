package com.programmersbox.favoritesdatabase

import androidx.paging.PagingSource
import androidx.room3.AutoMigration
import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Update
import androidx.room3.migration.Migration
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Database(
    entities = [HistoryItem::class, RecentModel::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
abstract class HistoryDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao

    companion object {

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                listOf("History", "RecentlyViewed").forEach { table ->
                    connection.execSQL("ALTER TABLE `$table` ADD COLUMN `supabase_id` TEXT DEFAULT ''")
                    connection.execSQL("ALTER TABLE `$table` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
                    connection.execSQL("ALTER TABLE `$table` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
                    connection.execSQL("ALTER TABLE `$table` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
                    connection.execSQL("ALTER TABLE `$table` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
                }
            }
        }

        fun getInstance(databaseBuilder: DatabaseBuilder): HistoryDatabase = databaseBuilder
            .build<HistoryDatabase>("history.db")
            .addMigrations(MIGRATION_2_3)
            .build()
    }

}

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface HistoryDao {

    @Query("SELECT * FROM History WHERE is_deleted = 0 ORDER BY time DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Query("SELECT * FROM History ORDER BY time DESC")
    suspend fun getAllHistorySync(): List<HistoryItem>

    @Query("SELECT COUNT(search_text) FROM History WHERE is_deleted = 0")
    fun getAllHistoryCount(): Flow<Int>

    @Query("SELECT * FROM History WHERE search_text LIKE :searchText AND is_deleted = 0 ORDER BY time DESC")
    fun searchHistory(searchText: String): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(model: HistoryItem)

    @Delete
    suspend fun deleteHistory(model: HistoryItem)

    @Query("SELECT COUNT(url) FROM RecentlyViewed")
    fun getAllRecentHistoryCount(): Flow<Int>

    @Query("SELECT * FROM RecentlyViewed ORDER BY timestamp ASC")
    fun getRecentlyViewed(): Flow<List<RecentModel>>

    @Query("SELECT * FROM RecentlyViewed ORDER BY timestamp DESC")
    fun getRecentlyViewedPaging(): PagingSource<Int, RecentModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyViewed(model: RecentModel)

    @Delete
    suspend fun deleteRecent(model: RecentModel)

    @Query("DELETE FROM RecentlyViewed WHERE url IN (SELECT url FROM RecentlyViewed ORDER BY timestamp DESC LIMIT 1 OFFSET :limit)")
    suspend fun removeOldData(limit: Int)

    @Query("DELETE FROM RecentlyViewed")
    suspend fun deleteAllRecentHistory(): Int

    // Dirty queries for sync
    @Query("SELECT * FROM History WHERE is_dirty = 1")
    suspend fun getDirtyHistory(): List<HistoryItem>

    @Query("SELECT * FROM RecentlyViewed WHERE is_dirty = 1")
    suspend fun getDirtyRecentlyViewed(): List<RecentModel>

    // By-key lookups for conflict resolution
    @Query("SELECT * FROM History WHERE search_text = :searchText")
    suspend fun getHistoryByKey(searchText: String): HistoryItem?

    @Query("SELECT * FROM RecentlyViewed WHERE url = :url")
    suspend fun getRecentByUrl(url: String): RecentModel?

    // Update (for clearing is_dirty after push)
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateHistory(model: HistoryItem)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateRecentlyViewed(model: RecentModel)

    // Soft-delete
    @Query("UPDATE History SET is_deleted = 1, is_dirty = 1, updated_at = :timestamp WHERE search_text = :searchText")
    suspend fun softDeleteHistory(searchText: String, timestamp: Long)

    @Query("UPDATE RecentlyViewed SET is_deleted = 1, is_dirty = 1, updated_at = :timestamp WHERE url = :url")
    suspend fun softDeleteRecentlyViewed(url: String, timestamp: Long)

    // Mark synced
    @Query("UPDATE History SET updated_at = :timestamp, is_dirty = 0 WHERE search_text = :key")
    suspend fun markHistorySynced(key: String, timestamp: Long)

    @Query("UPDATE RecentlyViewed SET updated_at = :timestamp, is_dirty = 0 WHERE url = :url")
    suspend fun markRecentSynced(url: String, timestamp: Long)

}

@Serializable
@Entity(tableName = "History")
data class HistoryItem(
    @ColumnInfo(name = "time")
    val time: Long,
    @PrimaryKey
    @ColumnInfo(name = "search_text")
    val searchText: String,
    @ColumnInfo(name = "supabase_id", defaultValue = "") val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1") val isDirty: Boolean = true,
)

@Serializable
@Entity(tableName = "RecentlyViewed")
data class RecentModel @OptIn(ExperimentalTime::class) constructor(
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String,
    @PrimaryKey
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "imageUrl")
    val imageUrl: String,
    @ColumnInfo(name = "sources")
    val source: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    @ColumnInfo(name = "supabase_id", defaultValue = "") val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1") val isDirty: Boolean = true,
)