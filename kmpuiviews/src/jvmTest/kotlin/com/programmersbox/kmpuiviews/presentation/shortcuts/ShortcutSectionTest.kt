package com.programmersbox.kmpuiviews.presentation.shortcuts

import kotlin.test.Test
import kotlin.test.assertEquals

class ShortcutSectionTest {
    private val strings = BuiltInShortcutStrings(
        anywhere = "Anywhere",
        inQuickActions = "In Quick actions",
        quickActions = "Quick actions",
        globalSearch = "Global search",
        shortcutsList = "Keyboard shortcuts",
        move = "Move selection",
        run = "Run selected action",
        close = "Close",
    )

    @Test
    fun builtInSectionsMac() {
        val sections = builtInShortcutSections(isMac = true, strings = strings)
        assertEquals(listOf("Anywhere", "In Quick actions"), sections.map { it.title })
        assertEquals(listOf("⇧", "⌘", "F"), sections[0].entries.first { it.label == "Global search" }.keys)
        assertEquals(listOf("↵"), sections[1].entries.first { it.label == "Run selected action" }.keys)
    }

    @Test
    fun builtInSectionsWindows() {
        val sections = builtInShortcutSections(isMac = false, strings = strings)
        assertEquals(listOf("Ctrl", "Shift", "F"), sections[0].entries.first { it.label == "Global search" }.keys)
        assertEquals(listOf("Enter"), sections[1].entries.first { it.label == "Run selected action" }.keys)
    }

    @Test
    fun keysTextJoins() {
        assertEquals("Space / K", ShortcutEntry(listOf("Space", "K"), "x").keysText)
        assertEquals("Shift + C", ShortcutEntry(listOf("Shift", "C"), "x", separator = " + ").keysText)
    }
}
