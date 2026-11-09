package com.programmersbox.anime.shared.episodeload

/**
 * Whether the Android player, having drawn a frame, may end the loading overlay's hand-off.
 *
 * The player requests landscape. On a phone that recreates the activity, so the old portrait activity must not end
 * the hand-off (its window would show rotated until the new one draws): wait for landscape. Where the request is
 * ignored the app stays portrait, so finish anyway: in multi-window, and on large screens (smallest width >= 600dp,
 * e.g. tablets, unfolded foldables, desktop windowing; Android 16+ ignores orientation requests there).
 */
internal fun shouldFinishHandoff(isLandscape: Boolean, inMultiWindow: Boolean, smallestWidthDp: Int): Boolean =
    isLandscape || inMultiWindow || smallestWidthDp >= 600
