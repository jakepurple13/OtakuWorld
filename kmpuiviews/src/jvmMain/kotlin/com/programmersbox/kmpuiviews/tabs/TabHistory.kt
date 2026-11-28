package com.programmersbox.kmpuiviews.tabs

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey

const val MAX_TAB_HISTORY = 50

/**
 * Browser-style navigation history for one desktop tab.
 * [entries] holds every visited key; [index] points at the one being shown.
 */
class TabHistory(start: NavKey) {
    private val _entries = mutableStateListOf(start)
    private val _index = mutableIntStateOf(0)

    val entries: List<NavKey> get() = _entries
    val index: Int get() = _index.intValue
    val current: NavKey get() = _entries[index]
    val canGoBack: Boolean get() = index > 0
    val canGoForward: Boolean get() = index < _entries.lastIndex

    fun push(key: NavKey) {
        if (key == current) return
        while (_entries.lastIndex > index) _entries.removeAt(_entries.lastIndex)
        _entries.add(key)
        while (_entries.size > MAX_TAB_HISTORY) _entries.removeAt(0)
        _index.intValue = _entries.lastIndex
    }

    fun back(): Boolean = jumpTo(index - 1)

    fun forward(): Boolean = jumpTo(index + 1)

    fun jumpTo(target: Int): Boolean {
        if (target !in _entries.indices || target == index) return false
        _index.intValue = target
        return true
    }

    /** Moves to the last [key] at or before [index]; with [inclusive] moves to the entry before it. */
    fun popTo(key: NavKey, inclusive: Boolean): Boolean {
        val found = (index downTo 0).firstOrNull { _entries[it] == key } ?: return false
        val target = if (inclusive) found - 1 else found
        if (target < 0) return false
        _index.intValue = target
        return true
    }

    fun remove(key: NavKey) {
        val kept = _entries.filter { it != key }
        if (kept.isEmpty()) return
        val keptUpToCurrent = _entries.take(index + 1).count { it != key }
        val newIndex = (keptUpToCurrent - 1).coerceAtLeast(0)
        _entries.clear()
        _entries.addAll(kept)
        _index.intValue = newIndex
    }

    fun reset(key: NavKey) {
        _entries.clear()
        _entries.add(key)
        _index.intValue = 0
    }

    /** [index] below zero means the last entry. */
    fun restore(entries: List<NavKey>, index: Int) {
        require(entries.isNotEmpty()) { "History cannot be empty" }
        _entries.clear()
        _entries.addAll(entries)
        _index.intValue = if (index < 0) entries.lastIndex else index.coerceAtMost(entries.lastIndex)
    }

    fun backStack(): List<NavKey> = _entries
        .take(index + 1)
        .asReversed()
        .distinct()
        .asReversed()
}
