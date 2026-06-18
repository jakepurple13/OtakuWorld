package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.BookmarkDao
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import com.programmersbox.supabaseintegration.auth.AuthManager
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

class BookmarkRepository(
    private val dao: BookmarkDao,
    private val authManager: AuthManager,
) {

    fun getAllBookmarks(): Flow<List<BookmarkedChapter>> = dao.getAllBookmarks()

    fun getBookmarksForDetail(parentUrl: String): Flow<List<BookmarkedChapter>> =
        dao.getBookmarksForDetail(parentUrl)

    fun getBookmark(chapterUrl: String): Flow<BookmarkedChapter?> =
        dao.getBookmark(chapterUrl)

    fun searchBookmarks(query: String): Flow<List<BookmarkedChapter>> =
        dao.searchBookmarks(query)

    suspend fun insertBookmark(bookmark: BookmarkedChapter) =
        dao.insertBookmark(bookmark)

    suspend fun deleteBookmark(chapterUrl: String) {
        if (authManager.isLoggedIn())
            dao.softDeleteBookmark(chapterUrl, Clock.System.now().toEpochMilliseconds())
        else
            dao.deleteBookmarkByUrl(chapterUrl)
    }

    suspend fun getAllBookmarksSync(): List<BookmarkedChapter> =
        dao.getAllBookmarksSync()
}
