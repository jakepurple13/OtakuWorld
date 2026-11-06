package com.programmersbox.kmpuiviews.notifier

import com.programmersbox.sharedcomponents.notifier.BannerId
import com.programmersbox.sharedcomponents.notifier.InAppNotifier

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
