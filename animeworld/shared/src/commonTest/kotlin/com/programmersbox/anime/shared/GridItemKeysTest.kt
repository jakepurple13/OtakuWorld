package com.programmersbox.anime.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class GridItemKeysTest {
    @Test
    fun keysAreUrlsAndDoNotShiftOnInsert() {
        val before = gridItemKeys(listOf("a", "b", "c"))
        val after = gridItemKeys(listOf("new", "a", "b", "c"))
        assertEquals(listOf("a", "b", "c"), before)
        assertEquals(before, after.drop(1))
    }

    @Test
    fun repeatedUrlsStayUnique() {
        val keys = gridItemKeys(listOf("a", "b", "a", "a"))
        assertEquals(keys.size, keys.toSet().size)
        assertEquals("a", keys[0])
    }
}
