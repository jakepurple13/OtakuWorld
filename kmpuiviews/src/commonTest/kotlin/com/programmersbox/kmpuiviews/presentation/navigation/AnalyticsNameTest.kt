package com.programmersbox.kmpuiviews.presentation.navigation

import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsNameTest {
    @Test
    fun screenUsesRoute() = assertEquals("recent", Screen.RecentScreen.analyticsName())

    @Test
    fun dataScreenDropsArguments() = assertEquals("notes_detail", Screen.NotesScreen.Detail("https://x/y").analyticsName())

    @Test
    fun detailsNeverLeaksTitleOrUrl() {
        val name = Screen.DetailsScreen.Details("Secret Title", "d", "https://site/item", "i", "Src").analyticsName()
        assertEquals("Details", name)
    }

    @Test
    fun nullIsNone() = assertEquals("none", (null as NavKey?).analyticsName())
}
