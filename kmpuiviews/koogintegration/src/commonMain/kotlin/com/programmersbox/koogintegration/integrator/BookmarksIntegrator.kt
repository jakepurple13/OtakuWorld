package com.programmersbox.koogintegration.integrator

import com.programmersbox.favoritesdatabase.BookmarkDao

class BookmarksIntegrator(
    private val bookmarksDao: BookmarkDao,
) : KoogIntegrator<String>() {
    override suspend fun map(input: String): String {
        return buildString {
            appendLine("Bookmarks")
            bookmarksDao
                .getAllBookmarksSync()
                .groupBy { it.parentTitle }
                .forEach { (title, value) ->
                    appendLine(title)
                    appendLine("Chapters:")
                    value.forEach {
                        appendLine(it.chapterName)
                        appendLine("for source: ${it.source}")
                    }
                }
        }
    }
}