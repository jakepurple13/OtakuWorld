package com.programmersbox.manga.shared.reader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChapterWindowTest {

    private fun chapter(index: Int, pages: Int = 2): List<PageItem> =
        List(pages) { PageItem.Page("c$index-p$it", index, it) }

    /** Chapters in reading order (oldest first), each with its top boundary, plus the bottom boundary. */
    private fun window(vararg chapters: Int): MutableList<PageItem> = buildList {
        chapters.forEach { c ->
            if (none { it == PageItem.ChapterTransition(c + 1, c) }) add(PageItem.ChapterTransition(c + 1, c))
            addAll(chapter(c))
            add(PageItem.ChapterTransition(c, c - 1))
        }
    }.toMutableList()

    @Test
    fun `top boundary of the opened chapter does not prepend until scrolling back`() {
        val top = PageItem.ChapterTransition(6, 5)
        assertNull(top.loadFor(listOf(5), scrolledBackward = false))
        assertEquals(TransitionLoad.Prepend(6), top.loadFor(listOf(5), scrolledBackward = true))
    }

    @Test
    fun `bottom boundary appends the newer chapter`() {
        val bottom = PageItem.ChapterTransition(5, 4)
        assertEquals(TransitionLoad.Append(4), bottom.loadFor(listOf(5), scrolledBackward = false))
        assertEquals(TransitionLoad.Append(4), bottom.loadFor(listOf(5), scrolledBackward = true))
    }

    @Test
    fun `boundary between loaded chapters loads nothing`() {
        assertNull(PageItem.ChapterTransition(5, 4).loadFor(listOf(5, 4), scrolledBackward = true))
    }

    @Test
    fun `evicting the top chapter keeps the boundary that leads back to it`() {
        val items = window(6, 5, 4)
        items.evictChapter(6, fromTop = true)
        assertEquals(window(5, 4), items)
        // The new top boundary loads chapter 6 again when scrolling back
        val top = items.first() as PageItem.ChapterTransition
        assertEquals(TransitionLoad.Prepend(6), top.loadFor(listOf(5, 4), scrolledBackward = true))
    }

    @Test
    fun `evicting the bottom chapter keeps the boundary that leads back to it`() {
        val items = window(6, 5, 4)
        items.evictChapter(4, fromTop = false)
        assertEquals(window(6, 5), items)
        val bottom = items.last() as PageItem.ChapterTransition
        assertEquals(PageItem.ChapterTransition(5, 4), bottom)
        assertEquals(TransitionLoad.Append(4), bottom.loadFor(listOf(6, 5), scrolledBackward = false))
    }

    @Test
    fun `evicting actually removes the chapter's pages`() {
        val items = window(6, 5, 4)
        items.evictChapter(6, fromTop = true)
        assertEquals(0, items.count { it is PageItem.Page && it.chapterListIndex == 6 })
        assertEquals(4, items.count { it is PageItem.Page })
    }
}
