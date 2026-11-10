package com.programmersbox.kmpuiviews.presentation.quickactions

import com.programmersbox.kmpuiviews.presentation.shortcuts.AppShortcut
import com.programmersbox.kmpuiviews.presentation.shortcuts.label

internal actual val quickActionsDesktopExtras: Boolean = true

internal actual fun quickActionShortcutLabel(action: QuickAction): String? =
    if (action == QuickAction.GlobalSearch) AppShortcut.GlobalSearch.label(isMacOs) else null

internal actual val runKeyLabel: String = if (isMacOs) "↵" else "Enter"

internal actual val shortcutsListKeyLabel: String = AppShortcut.ShortcutsList.label(isMacOs)
