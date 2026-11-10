package com.programmersbox.anime.shared.videoplayer

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerShortcutsTest {
    @Test
    fun keysTextMatchesTodaysSheet() {
        assertEquals(
            listOf("Space / K", "← / →", "J / L", "S", "↑ / ↓ / wheel", "M", "F / double-click", "C", "Shift + C", "[ / ]", "0–9", "H / click", "?", "Esc"),
            PlayerShortcuts.map { it.keysText },
        )
        assertEquals("This sheet", PlayerShortcuts.single { it.keys == listOf("?") }.label)
        assertEquals("Player shortcuts sheet", PlayerShortcuts.single { it.keys == listOf("?") }.listLabel)
    }

    @Test
    fun sectionTitle() {
        assertEquals("Video player", playerShortcutSection().title)
    }
}
