package com.programmersbox.favoritesdatabase

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
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(tableName = "bookmarked_chapters")
@Serializable
data class BookmarkedChapter(
    @PrimaryKey val chapterUrl: String,
    val chapterName: String,
    val parentUrl: String,
    val parentTitle: String,
    val parentImageUrl: String,
    val source: String,
    val timestamp: Long, // epoch millis
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

    @Query("SELECT * FROM bookmarked_chapters ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkedChapter>>

    @Query("SELECT * FROM bookmarked_chapters WHERE parentUrl = :parentUrl")
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
}

@Database(
    entities = [BookmarkedChapter::class, BookmarkedChapterFts::class],
    version = 1,
    exportSchema = true,
)
abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): BookmarkDatabase =
            databaseBuilder
                .build<BookmarkDatabase>("bookmarks.db")
                .build()
    }
}
