package com.programmersbox.favoritesdatabase

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow

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

        @Volatile
        private var INSTANCE: HistoryDatabase? = null

        fun getInstance(context: Context): HistoryDatabase =
            INSTANCE ?: synchronized(this) { INSTANCE ?: buildDatabase(context).also { INSTANCE = it } }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, HistoryDatabase::class.java, "history.db")
                .build()
    }

}

@Dao
interface HistoryDao {

    @Query("SELECT * FROM History ORDER BY time DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

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
data class RecentModel(
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
    var timestamp: Long = System.currentTimeMillis()
)