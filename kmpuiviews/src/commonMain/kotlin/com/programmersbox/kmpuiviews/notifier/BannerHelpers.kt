package com.programmersbox.kmpuiviews.notifier

import com.programmersbox.sharedcomponents.notifier.BannerId
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.programmersbox.sharedcomponents.notifier.InAppNotifier
import com.programmersbox.sharedcomponents.notifier.ProgressHandle
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject

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

/**
 * A progress banner owned by a screen. [start] shows it, [success]/[error] morph it into the result.
 * If the screen leaves composition while it is still running, it is dismissed
 * (progress banners never time out, so it would otherwise spin forever).
 */
internal class ScreenProgressBanner(private val notifier: InAppNotifier) {
    private var handle: ProgressHandle? = null

    fun start(title: String) {
        handle?.dismiss()
        handle = notifier.progress(title)
    }

    fun success(title: String) {
        val current = handle
        handle = null
        if (current != null) current.success(title) else notifier.success(title)
    }

    fun error(title: String, subtitle: String?) {
        val current = handle
        handle = null
        if (current != null) current.error(title, subtitle) else notifier.error(title, subtitle)
    }

    fun dismissIfRunning() {
        handle?.dismiss()
        handle = null
    }
}

@Composable
internal fun rememberScreenProgressBanner(): ScreenProgressBanner {
    val notifier = koinInject<InAppNotifier>()
    val banner = remember(notifier) { ScreenProgressBanner(notifier) }
    DisposableEffect(banner) { onDispose { banner.dismissIfRunning() } }
    return banner
}
