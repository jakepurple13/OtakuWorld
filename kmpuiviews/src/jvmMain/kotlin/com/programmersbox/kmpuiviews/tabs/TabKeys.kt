package com.programmersbox.kmpuiviews.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tab
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen
import net.thauvin.erik.urlencoder.UrlEncoderUtil

/** Identity of a tab's content. Navigating to a key that is already open focuses that tab instead of opening another. */
sealed interface TabKey {
    data class Title(val url: String, val source: String) : TabKey
    data class Chapter(val url: String) : TabKey
    data object Settings : TabKey
    data object Search : TabKey
    data object Lists : TabKey
    data object Notifications : TabKey
    data class SingleList(val uuid: String) : TabKey
    data object Downloads : TabKey
}

val TabKey.isSingleInstanceFamily: Boolean
    get() = when (this) {
        TabKey.Settings,
        TabKey.Search,
        TabKey.Lists,
        TabKey.Notifications,
        TabKey.Downloads -> true

        is TabKey.Title,
        is TabKey.Chapter,
        is TabKey.SingleList -> false
    }

data class TabLabel(val text: String, val imageUrl: String?, val icon: ImageVector?)

interface TabKeyProvider {
    fun keyOf(key: NavKey): TabKey?
    fun labelOf(key: NavKey): TabLabel?
}

/** Where the Downloads destination lives in this app, or null when the app has none. */
data class DownloadsDestination(val key: NavKey?)

class TabKeys(private val providers: List<TabKeyProvider>) {
    fun keyOf(key: NavKey): TabKey? = providers.firstNotNullOfOrNull { it.keyOf(key) }

    fun labelOf(key: NavKey): TabLabel = providers.firstNotNullOfOrNull { it.labelOf(key) }
        ?: TabLabel(key::class.simpleName ?: "Tab", null, Icons.Default.Tab)
}

object CoreTabKeyProvider : TabKeyProvider {
    override fun keyOf(key: NavKey): TabKey? = when (key) {
        is Screen.DetailsScreen.Details -> TabKey.Title(key.url, key.source)

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
        Screen.ThemeSettings,
        Screen.DetailsSettings,
        Screen.SecuritySettings,
        Screen.DailyOverview.Settings -> TabKey.Settings

        is Screen.GlobalSearchScreen,
        is Screen.GlobalSearchScreen.Home -> TabKey.Search

        Screen.CustomListScreen,
        Screen.CustomListScreen.Home -> TabKey.Lists

        is Screen.CustomListScreen.CustomListItem -> TabKey.SingleList(key.uuid)

        Screen.NotificationScreen,
        Screen.NotificationScreen.Home,
        Screen.NotificationScreen.CatchUp -> TabKey.Notifications

        // These live inside Home, so they never get their own tab.
        else -> null
    }

    override fun labelOf(key: NavKey): TabLabel? = when (key) {
        is Screen.DetailsScreen.Details -> TabLabel(
            text = key.title,
            imageUrl = runCatching { UrlEncoderUtil.decode(key.imageUrl) }.getOrDefault(key.imageUrl),
            icon = null,
        )

        else -> null
    }
}
