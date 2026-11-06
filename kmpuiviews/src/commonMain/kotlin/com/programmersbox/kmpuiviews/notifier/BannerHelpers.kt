package com.programmersbox.kmpuiviews.notifier

import com.programmersbox.sharedcomponents.notifier.BannerId
import com.programmersbox.sharedcomponents.notifier.InAppNotifier
import kotlinx.coroutines.flow.first

/**
 * "Source not installed" error banner with a More options action.
 * The labels are resolved by the caller because the notifier is not suspending.
 */
internal fun InAppNotifier.sourceMissing(
    title: String,
    sourceName: String,
    missingLabel: String,
    moreOptionsLabel: String,
    onMoreOptions: suspend () -> Unit,
): BannerId = error(
    title = missingLabel,
    subtitle = "$title · $sourceName",
    action = moreOptionsLabel,
    onAction = onMoreOptions,
)

/**
 * Shows an info banner with an Undo action and suspends until the banner has left the stack
 * (timeout, swipe, close or Undo). Returns whether Undo ran. If the caller is cancelled
 * the banner is dismissed, so its Undo can no longer fire.
 */
internal suspend fun InAppNotifier.showUndoable(
    title: String,
    undoLabel: String,
    onUndo: suspend () -> Unit,
): Boolean {
    var undone = false
    val id = info(title = title, action = undoLabel) {
        undone = true
        onUndo()
    }
    try {
        banners.first { list -> list.none { it.id == id } }
    } finally {
        if (!undone) dismiss(id)
    }
    return undone
}
