package com.programmersbox.kmpuiviews.tabs

import com.programmersbox.kmpuiviews.presentation.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TabHistoryTest {
    private val r = Screen.RecentScreen
    private val a = Screen.Settings
    private val b = Screen.FavoriteScreen
    private val c = Screen.HistoryScreen

    @Test
    fun pushAppendsAndMovesIndex() {
        val h = TabHistory(r)
        h.push(a)
        h.push(b)
        assertEquals(listOf(r, a, b), h.entries.toList())
        assertEquals(2, h.index)
    }

    @Test
    fun pushSameAsCurrentIsNoOp() {
        val h = TabHistory(r)
        h.push(r)
        assertEquals(listOf(r), h.entries.toList())
    }

    @Test
    fun pushFromEarlierEntryDropsForward() {
        val h = TabHistory(r)
        h.push(a)
        h.push(b)
        h.back()
        h.push(c)
        assertEquals(listOf(r, a, c), h.entries.toList())
        assertEquals(2, h.index)
        assertFalse(h.canGoForward)
    }

    @Test
    fun backAndForwardStepOnce() {
        val h = TabHistory(r)
        h.push(a)
        h.push(b)
        assertTrue(h.back())
        assertEquals(1, h.index)
        assertTrue(h.back())
        assertFalse(h.back())
        assertEquals(0, h.index)
        h.forward()
        h.forward()
        assertFalse(h.forward())
        assertEquals(2, h.index)
    }

    @Test
    fun jumpToKeepsBothSides() {
        val h = TabHistory(r)
        h.push(a)
        h.push(b)
        h.push(c)
        assertTrue(h.jumpTo(1))
        assertEquals(1, h.index)
        assertEquals(listOf(r, a, b, c), h.entries.toList())
        assertFalse(h.jumpTo(1))
        assertFalse(h.jumpTo(9))
    }

    @Test
    fun popToMovesToLastOccurrenceAtOrBeforeIndex() {
        val h = TabHistory(r)
        h.restore(listOf(r, a, r, b), 3)
        assertTrue(h.popTo(r, inclusive = false))
        assertEquals(2, h.index)
        assertTrue(h.popTo(a, inclusive = true))
        assertEquals(0, h.index)
    }

    @Test
    fun popToMissingKeyReturnsFalse() {
        val h = TabHistory(r)
        h.restore(listOf(r, a, r, b), 3)
        assertFalse(h.popTo(c, false))
        assertEquals(3, h.index)
    }

    @Test
    fun removeDropsAllOccurrencesAndKeepsPosition() {
        val h = TabHistory(r)
        h.restore(listOf(r, a, b, a), 2)
        h.remove(a)
        assertEquals(listOf(r, b), h.entries.toList())
        assertEquals(1, h.index)
        h.remove(r)
        h.remove(b)
        assertEquals(listOf(b), h.entries.toList())
    }

    @Test
    fun capDropsOldest() {
        val h = TabHistory(r)
        repeat(60) { h.push(Screen.SourceSearchScreen("s", "$it")) }
        assertEquals(MAX_TAB_HISTORY, h.entries.size)
        assertEquals(Screen.SourceSearchScreen("s", "59"), h.current)
        assertEquals(Screen.SourceSearchScreen("s", "10"), h.entries.first())
        assertEquals(MAX_TAB_HISTORY - 1, h.index)
    }

    @Test
    fun backStackKeepsLastOccurrence() {
        val h = TabHistory(r)
        h.restore(listOf(r, a, r, b), 3)
        assertEquals(listOf(a, r, b), h.backStack())
        h.jumpTo(1)
        assertEquals(listOf(r, a), h.backStack())
    }

    @Test
    fun restoreCoercesIndex() {
        val h = TabHistory(r)
        h.restore(listOf(r, a), 7)
        assertEquals(1, h.index)
        h.restore(listOf(r, a), -1)
        assertEquals(1, h.index)
    }
}
