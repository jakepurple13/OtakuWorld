package com.programmersbox.kmpuiviews.presentation.quickactions

/** True on desktop, where the card gets a filter field and keyboard selection. */
internal expect val quickActionsDesktopExtras: Boolean

/** [texts] holds each action's "title supporting" text. Blank query returns every action. */
fun filterQuickActions(query: String, texts: Map<QuickAction, String>): List<QuickAction> {
    val q = query.trim()
    if (q.isEmpty()) return QuickAction.entries
    return QuickAction.entries.filter { texts[it].orEmpty().contains(q, ignoreCase = true) }
}

fun moveSelection(current: Int, delta: Int, size: Int): Int =
    if (size <= 0) 0 else (current + delta).coerceIn(0, size - 1)
