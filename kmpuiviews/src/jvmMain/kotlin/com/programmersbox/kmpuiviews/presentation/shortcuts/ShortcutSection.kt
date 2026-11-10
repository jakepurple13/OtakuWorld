package com.programmersbox.kmpuiviews.presentation.shortcuts

/** One row in the shortcuts list. [listLabel], when set, replaces [label] in the list. */
data class ShortcutEntry(
    val keys: List<String>,
    val label: String,
    val separator: String = " / ",
    val listLabel: String? = null,
) {
    val keysText: String get() = keys.joinToString(separator)
}

data class ShortcutSection(val title: String, val entries: List<ShortcutEntry>)

/** Resolved strings so [builtInShortcutSections] stays pure. */
data class BuiltInShortcutStrings(
    val anywhere: String,
    val inQuickActions: String,
    val quickActions: String,
    val globalSearch: String,
    val shortcutsList: String,
    val move: String,
    val run: String,
    val close: String,
)

fun builtInShortcutSections(isMac: Boolean, strings: BuiltInShortcutStrings): List<ShortcutSection> = listOf(
    ShortcutSection(
        title = strings.anywhere,
        entries = listOf(
            ShortcutEntry(AppShortcut.QuickActions.keyCaps(isMac), strings.quickActions),
            ShortcutEntry(AppShortcut.GlobalSearch.keyCaps(isMac), strings.globalSearch),
            ShortcutEntry(AppShortcut.ShortcutsList.keyCaps(isMac), strings.shortcutsList),
        ),
    ),
    ShortcutSection(
        title = strings.inQuickActions,
        entries = listOf(
            ShortcutEntry(listOf("↑", "↓"), strings.move),
            ShortcutEntry(listOf(if (isMac) "↵" else "Enter"), strings.run),
            ShortcutEntry(listOf("Esc"), strings.close),
        ),
    ),
)
