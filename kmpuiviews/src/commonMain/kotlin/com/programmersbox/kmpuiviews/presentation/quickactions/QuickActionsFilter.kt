package com.programmersbox.kmpuiviews.presentation.quickactions

/** True on desktop, where the card gets a filter field and keyboard selection. */
internal expect val quickActionsDesktopExtras: Boolean

/** Shortcut label shown on [action]'s row (desktop only), or null. */
internal expect fun quickActionShortcutLabel(action: QuickAction): String?

/** Label of the key that runs the selected row: Enter glyph/name on desktop, empty elsewhere. */
internal expect val runKeyLabel: String

/** Label of the keyboard-shortcuts-list shortcut (desktop only), empty elsewhere. */
internal expect val shortcutsListKeyLabel: String

/**
 * [texts] holds each action's "title supporting" text. Only [actions] are searched, in order.
 * Blank query returns every one of them.
 */
fun filterQuickActions(
    query: String,
    texts: Map<QuickAction, String>,
    actions: List<QuickAction> = QuickAction.entries,
): List<QuickAction> {
    val q = query.trim()
    if (q.isEmpty()) return actions
    return actions.filter { texts[it].orEmpty().contains(q, ignoreCase = true) }
}

fun moveSelection(current: Int, delta: Int, size: Int): Int =
    if (size <= 0) 0 else (current + delta).coerceIn(0, size - 1)
