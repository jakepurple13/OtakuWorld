package com.programmersbox.favoritesdatabase

import androidx.paging.PagingSource
import androidx.room.AutoMigration
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
    entities = [HistoryItem::class, RecentModel::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
abstract class HistoryDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao

    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): HistoryDatabase = databaseBuilder
            .build<HistoryDatabase>("history.db")
            .build()
    }

}

@Dao
interface HistoryDao {

    @Query("SELECT * FROM History ORDER BY time DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Query("SELECT COUNT(search_text) FROM History")
    fun getAllHistoryCount(): Flow<Int>

    @Query("SELECT * FROM History WHERE search_text LIKE :searchText ORDER BY time DESC")
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

}

@Entity(tableName = "History")
data class HistoryItem(
    @ColumnInfo(name = "time")
    val time: Long,
    @PrimaryKey
    @ColumnInfo(name = "search_text")
    val searchText: String
)

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
)