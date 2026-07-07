package com.programmersbox.favoritesdatabase

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Fts4
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Update
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Entity(tableName = "bookmarked_chapters")
@Serializable
data class BookmarkedChapter(
    @PrimaryKey
    @ColumnInfo(name = "chapterUrl")
    val chapterUrl: String,
    @ColumnInfo(name = "chapterName")
    val chapterName: String,
    @ColumnInfo(name = "parentUrl")
    val parentUrl: String,
    @ColumnInfo(name = "parentTitle")
    val parentTitle: String,
    @ColumnInfo(name = "parentImageUrl")
    val parentImageUrl: String,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    @ColumnInfo(name = "supabase_id", defaultValue = "") val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1") val isDirty: Boolean = true,
)

@Entity(tableName = "bookmarked_chapters_fts")
@Fts4(contentEntity = BookmarkedChapter::class)
data class BookmarkedChapterFts(
    val chapterName: String,
    val parentTitle: String,
)

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkedChapter)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkedChapter)

    @Query("DELETE FROM bookmarked_chapters WHERE chapterUrl = :chapterUrl")
    suspend fun deleteBookmarkByUrl(chapterUrl: String)

    @Query("SELECT * FROM bookmarked_chapters WHERE is_deleted = 0 ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkedChapter>>

    @Query("SELECT * FROM bookmarked_chapters WHERE is_deleted = 0 AND parentUrl = :parentUrl")
    fun getBookmarksForDetail(parentUrl: String): Flow<List<BookmarkedChapter>>

    @Query("SELECT * FROM bookmarked_chapters WHERE chapterUrl = :chapterUrl")
    fun getBookmark(chapterUrl: String): Flow<BookmarkedChapter?>

    @Query("SELECT * FROM bookmarked_chapters WHERE chapterUrl IN (:urls)")
    fun getBookmarksForChapters(urls: List<String>): Flow<List<BookmarkedChapter>>

    @Query("""
        SELECT * FROM bookmarked_chapters WHERE rowid IN (
            SELECT rowid FROM bookmarked_chapters_fts
            WHERE bookmarked_chapters_fts MATCH :query
        ) ORDER BY timestamp DESC
    """)
    fun searchBookmarks(query: String): Flow<List<BookmarkedChapter>>

    @Query("SELECT * FROM bookmarked_chapters")
    suspend fun getAllBookmarksSync(): List<BookmarkedChapter>

    @Query("SELECT COUNT(chapterUrl) FROM bookmarked_chapters")
    fun getAllBookmarksCount(): Flow<Int>

    @Query("SELECT * FROM bookmarked_chapters WHERE is_dirty = 1")
    suspend fun getDirtyBookmarks(): List<BookmarkedChapter>

    @Query("SELECT COUNT(*) FROM bookmarked_chapters WHERE is_dirty = 1")
    fun observeDirtyBookmarkCount(): Flow<Int>

    @Query("SELECT * FROM bookmarked_chapters WHERE chapterUrl = :chapterUrl")
    suspend fun getBookmarkByChapterUrl(chapterUrl: String): BookmarkedChapter?

    @Query("SELECT EXISTS(SELECT * FROM bookmarked_chapters WHERE chapterUrl = :chapterUrl AND is_deleted = 0)")
    suspend fun hasBookmark(chapterUrl: String): Boolean

    @Query("SELECT EXISTS(SELECT * FROM bookmarked_chapters WHERE chapterUrl = :chapterUrl AND is_deleted = 0)")
    fun hasBookmarkFlow(chapterUrl: String): Flow<Boolean>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateBookmark(bookmark: BookmarkedChapter)

    @Query("UPDATE bookmarked_chapters SET is_deleted = 1, is_dirty = 1, updated_at = :timestamp WHERE chapterUrl = :chapterUrl")
    suspend fun softDeleteBookmark(chapterUrl: String, timestamp: Long)

    @Query("UPDATE bookmarked_chapters SET updated_at = :timestamp, is_dirty = 0 WHERE chapterUrl = :chapterUrl")
    suspend fun markBookmarkSynced(chapterUrl: String, timestamp: Long)

    @Query("UPDATE bookmarked_chapters SET is_deleted = 0")
    suspend fun resetAllBookmarksIsDeleted()

    @Query("DELETE FROM bookmarked_chapters WHERE is_deleted = 1")
    suspend fun deleteAllDeletedBookmarks()
}

@Database(
    entities = [BookmarkedChapter::class, BookmarkedChapterFts::class],
    version = 2,
    exportSchema = true,
)
abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE `bookmarked_chapters` ADD COLUMN `supabase_id` TEXT DEFAULT ''")
                connection.execSQL("ALTER TABLE `bookmarked_chapters` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `bookmarked_chapters` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `bookmarked_chapters` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `bookmarked_chapters` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(databaseBuilder: DatabaseBuilder): BookmarkDatabase =
            databaseBuilder
                .build<BookmarkDatabase>("bookmarks.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
