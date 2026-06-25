package com.programmersbox.kmpuiviews.settings

import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.settings.search.SettingsSearchItem
import com.programmersbox.kmpuiviews.presentation.settings.search.SettingsSearchRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsSearchRegistryTest {

    private fun makeItem(displayName: String, keywords: List<String> = emptyList()) =
        SettingsSearchItem(
            displayName = displayName,
            keywords = keywords,
            breadcrumb = emptyList(),
            targetScreen = Screen.ThemeSettings,
            highlightKey = displayName.lowercase().replace(" ", "_"),
        )

    @Test
    fun blankQueryReturnsEmpty() {
        val registry = SettingsSearchRegistry(listOf(makeItem("Dark Mode")))
        assertTrue(registry.search("").isEmpty())
        assertTrue(registry.search("   ").isEmpty())
    }

    @Test
    fun matchesDisplayNameCaseInsensitive() {
        val item = makeItem("Dark Mode")
        val registry = SettingsSearchRegistry(listOf(item))
        assertEquals(listOf(item), registry.search("dark"))
        assertEquals(listOf(item), registry.search("DARK"))
        assertEquals(listOf(item), registry.search("Dark Mode"))
    }

    @Test
    fun matchesKeyword() {
        val item = makeItem("Theme", listOf("amoled", "dark", "light"))
        val registry = SettingsSearchRegistry(listOf(item))
        assertEquals(listOf(item), registry.search("amoled"))
        assertEquals(listOf(item), registry.search("AMOLED"))
    }

    @Test
    fun noMatchReturnsEmpty() {
        val registry = SettingsSearchRegistry(listOf(makeItem("Dark Mode")))
        assertTrue(registry.search("xyz123").isEmpty())
    }

    @Test
    fun multipleItemsFilteredCorrectly() {
        val darkMode = makeItem("Dark Mode")
        val gridType = makeItem("Grid Type", listOf("columns", "layout"))
        val registry = SettingsSearchRegistry(listOf(darkMode, gridType))
        assertEquals(listOf(darkMode), registry.search("dark"))
        assertEquals(listOf(gridType), registry.search("columns"))
        assertEquals(2, registry.search("e").size) // matches both
    }
}
