package com.programmersbox.manga.shared

import com.programmersbox.manga.shared.downloads.DownloadedChapters
import com.programmersbox.manga.shared.downloads.sortedChapterPaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SortedChapterPathsTest {

    private fun chapter(name: String, folder: String) = DownloadedChapters(
        name = name,
        id = folder,
        data = "",
        assetFileStringUri = "",
        folder = "",
        folderName = "",
        chapterFolder = folder,
        chapterName = name,
    )

    @Test
    fun `sortedChapterPaths orders paths by numeric digits in name descending`() {
        val map = mapOf(
            "/root/ch1" to listOf(chapter("Chapter 1", "/root/ch1")),
            "/root/ch10" to listOf(chapter("Chapter 10", "/root/ch10")),
            "/root/ch2" to listOf(chapter("Chapter 2", "/root/ch2")),
        )
        val result = sortedChapterPaths(map)
        assertEquals(listOf("/root/ch10", "/root/ch2", "/root/ch1"), result)
    }

    @Test
    fun `sortedChapterPaths puts non-numeric names last`() {
        val map = mapOf(
            "/root/prologue" to listOf(chapter("Prologue", "/root/prologue")),
            "/root/ch1" to listOf(chapter("Chapter 1", "/root/ch1")),
        )
        val result = sortedChapterPaths(map)
        // "Chapter 1" digits=1, "Prologue" digits=0 → ch1 first
        assertEquals(listOf("/root/ch1", "/root/prologue"), result)
    }

    @Test
    fun `sortedChapterPaths handles empty map`() {
        assertEquals(emptyList(), sortedChapterPaths(emptyMap()))
    }
}

class ChapterHolderTest {

    @Test
    fun `downloadedChapterPaths is null by default`() {
        val holder = ChapterHolder()
        assertNull(holder.downloadedChapterPaths)
    }

    @Test
    fun `downloadedChapterPaths stores and clears list`() {
        val holder = ChapterHolder()
        holder.downloadedChapterPaths = listOf("/manga/ch10", "/manga/ch2", "/manga/ch1")
        assertEquals(3, holder.downloadedChapterPaths?.size)
        assertEquals("/manga/ch10", holder.downloadedChapterPaths?.first())
        holder.downloadedChapterPaths = null
        assertNull(holder.downloadedChapterPaths)
    }
}
