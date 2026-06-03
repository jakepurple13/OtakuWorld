package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.BookmarkDao
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(private val dao: BookmarkDao) {

    fun getAllBookmarks(): Flow<List<BookmarkedChapter>> = dao.getAllBookmarks()

    fun getBookmarksForDetail(parentUrl: String): Flow<List<BookmarkedChapter>> =
        dao.getBookmarksForDetail(parentUrl)

    fun getBookmark(chapterUrl: String): Flow<BookmarkedChapter?> =
        dao.getBookmark(chapterUrl)

    fun searchBookmarks(query: String): Flow<List<BookmarkedChapter>> =
        dao.searchBookmarks(query)

    suspend fun insertBookmark(bookmark: BookmarkedChapter) =
        dao.insertBookmark(bookmark)

    suspend fun deleteBookmark(chapterUrl: String) =
        dao.deleteBookmarkByUrl(chapterUrl)

    suspend fun getAllBookmarksSync(): List<BookmarkedChapter> =
        dao.getAllBookmarksSync()
}
