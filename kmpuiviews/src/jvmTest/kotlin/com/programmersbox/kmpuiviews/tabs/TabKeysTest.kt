package com.programmersbox.kmpuiviews.tabs

import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TabKeysTest {
    private val keys = TabKeys(listOf(CoreTabKeyProvider))

    @Test
    fun detailsMapsToTitleByUrlAndSource() {
        fun details(title: String, source: String) = Screen.DetailsScreen.Details(
            title = title,
            description = "d",
            url = "https%3A%2F%2Fa.com%2F1",
            imageUrl = "https%3A%2F%2Fa.com%2Fi.png",
            source = source,
        )

        val a = details("One", "src")
        assertEquals(TabKey.Title("https%3A%2F%2Fa.com%2F1", "src"), keys.keyOf(a))
        assertEquals(keys.keyOf(a), keys.keyOf(details("Other title", "src")))
        assertTrue(keys.keyOf(a) != keys.keyOf(details("One", "other")))

        val label = keys.labelOf(a)
        assertEquals("One", label.text)
        assertEquals("https://a.com/i.png", label.imageUrl)
    }

    @Test
    fun allSettingsScreensMapToSettings() {
        listOf<NavKey>(
            Screen.Settings,
            Screen.Settings.AppInfo,
            Screen.Settings.Blur,
            Screen.Settings.Library,
            Screen.Settings.Discover,
            Screen.Settings.Sources,
            Screen.Settings.Integrations,
            Screen.Settings.Ai,
            Screen.Settings.Translation,
            Screen.Settings.Appearance,
            Screen.Settings.Colors,
            Screen.Settings.Behavior,
            Screen.Settings.Layout,
            Screen.Settings.ContentReading,
            Screen.Settings.PrivacySecurity,
            Screen.Settings.Data,
            Screen.Settings.About,
            Screen.Settings.Diagnostics,
            Screen.Settings.Developer,
            Screen.SettingsScreen,
            Screen.GeneralSettings,
            Screen.OtherSettings,
            Screen.MoreInfoSettings,
            Screen.MoreSettings,
            Screen.NotificationsSettings,
            Screen.UpdateSourcesSettings,
        ).forEach { assertEquals(TabKey.Settings, keys.keyOf(it), "$it") }
    }

    @Test
    fun searchWithDifferentQueriesSameKey() {
        assertEquals(TabKey.Search, keys.keyOf(Screen.GlobalSearchScreen("a")))
        assertEquals(TabKey.Search, keys.keyOf(Screen.GlobalSearchScreen("b")))
        assertEquals(TabKey.Search, keys.keyOf(Screen.GlobalSearchScreen.Home("c")))
    }

    @Test
    fun listItemsHaveOwnKey() {
        assertEquals(TabKey.Lists, keys.keyOf(Screen.CustomListScreen))
        assertEquals(TabKey.Lists, keys.keyOf(Screen.CustomListScreen.Home))
        assertEquals(TabKey.SingleList("x"), keys.keyOf(Screen.CustomListScreen.CustomListItem("x")))
        assertTrue(
            keys.keyOf(Screen.CustomListScreen.CustomListItem("x")) !=
                    keys.keyOf(Screen.CustomListScreen.CustomListItem("y"))
        )
        assertEquals(TabKey.Notifications, keys.keyOf(Screen.NotificationScreen))
        assertEquals(TabKey.Notifications, keys.keyOf(Screen.NotificationScreen.Home))
        assertEquals(TabKey.Notifications, keys.keyOf(Screen.NotificationScreen.CatchUp))
    }

    @Test
    fun singleInstanceFamily() {
        listOf(TabKey.Settings, TabKey.Search, TabKey.Lists, TabKey.Notifications, TabKey.Downloads)
            .forEach { assertTrue(it.isSingleInstanceFamily) }
        assertTrue(!TabKey.Title("u", "s").isSingleInstanceFamily)
        assertTrue(!TabKey.Chapter("u").isSingleInstanceFamily)
        assertTrue(!TabKey.SingleList("u").isSingleInstanceFamily)
    }

    @Test
    fun homeSectionsHaveNoKey() {
        listOf<NavKey>(
            Screen.RecentScreen,
            Screen.AllScreen,
            Screen.FavoriteScreen,
            Screen.FavoriteScreen.Home,
            Screen.HistoryScreen,
            Screen.ExtensionListScreen,
            Screen.ExtensionListScreen.Home,
            Screen.ExtensionListScreen.Catalog("c"),
        ).forEach { assertNull(keys.keyOf(it), "$it") }
    }

    @Test
    fun firstProviderWins() {
        fun provider(text: String) = object : TabKeyProvider {
            override fun keyOf(key: NavKey): TabKey = TabKey.Chapter(text)
            override fun labelOf(key: NavKey): TabLabel = TabLabel(text, null, null)
        }

        val tabKeys = TabKeys(listOf(provider("first"), provider("second"), CoreTabKeyProvider))
        assertEquals(TabKey.Chapter("first"), tabKeys.keyOf(Screen.Settings))
        assertEquals("first", tabKeys.labelOf(Screen.Settings).text)
    }

    @Test
    fun unknownKeyFallsBackToClassNameLabel() {
        val label = keys.labelOf(Screen.RecentScreen)
        assertEquals("RecentScreen", label.text)
        assertNull(label.imageUrl)
    }
}
