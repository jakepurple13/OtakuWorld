package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.foundation.lazy.LazyListState

/**
 * Call right after Undo puts rows back into a list that uses item keys.
 *
 * LazyList keeps its first visible item in place by key, so rows restored above it come back
 * scrolled off the top: the list looks unchanged even though the rows are there. If the first
 * restored row lands at or above the current first visible item, this scrolls so it shows,
 * one item down so a pinned sticky header does not cover it.
 *
 * @param keys the list's item keys, in order, after the restore (sticky headers included)
 * @param restored the keys of the restored rows
 */
fun LazyListState.revealRestored(keys: List<Any>, restored: Set<Any>) {
    val target = keys.indexOfFirst { it in restored }
    if (target < 0) return
    val anchorKey = layoutInfo.visibleItemsInfo.firstOrNull { it.index == firstVisibleItemIndex }?.key ?: return
    val anchor = keys.indexOf(anchorKey)
    if (anchor < 0 || target > anchor) return
    requestScrollToItem((target - 1).coerceAtLeast(0))
}
