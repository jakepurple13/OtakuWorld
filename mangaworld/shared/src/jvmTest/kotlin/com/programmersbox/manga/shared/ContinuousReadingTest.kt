package com.programmersbox.manga.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
