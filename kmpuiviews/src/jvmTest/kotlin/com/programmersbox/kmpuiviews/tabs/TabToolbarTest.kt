package com.programmersbox.kmpuiviews.tabs

import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlin.test.Test
import kotlin.test.assertEquals

class TabToolbarTest {
    private val keys = TabKeys(emptyList())
    private fun label(key: NavKey) = keys.labelOf(key).text

    @Test
    fun shortPathHasNoEllipsis() {
        val stack = listOf(Screen.RecentScreen, Screen.HistoryScreen, Screen.Settings)
        assertEquals(
            (listOf("MangaWorld") + stack.map(::label)).joinToString(" / "),
            tabPath("MangaWorld", stack, keys),
        )
    }

    @Test
    fun longPathKeepsTheLastThreeAfterAnEllipsis() {
        val stack = listOf(Screen.RecentScreen, Screen.HistoryScreen, Screen.Settings, Screen.CustomListScreen, Screen.FavoriteScreen)
        assertEquals(
            (listOf("MangaWorld", "…") + stack.takeLast(3).map(::label)).joinToString(" / "),
            tabPath("MangaWorld", stack, keys),
        )
    }
}
