package com.programmersbox.manga.shared.downloads

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompleteChapterTest {

    @Test
    fun `chapter still downloading is not complete`() {
        assertFalse(isCompleteChapter(arrayOf("000.png", "001.png", INCOMPLETE_MARKER)))
    }

    @Test
    fun `finished chapter is complete`() {
        assertTrue(isCompleteChapter(arrayOf("000.png", "001.png")))
    }

    @Test
    fun `chapter saved before the marker existed still counts`() {
        assertTrue(isCompleteChapter(arrayOf("000.png")))
    }

    @Test
    fun `missing or empty folder is not complete`() {
        assertFalse(isCompleteChapter(null))
        assertFalse(isCompleteChapter(emptyArray()))
        assertFalse(isCompleteChapter(arrayOf(INCOMPLETE_MARKER)))
    }
}
