package com.programmersbox.kmpuiviews.presentation.settings.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.sharedtools.SearchRegistryItem
import com.programmersbox.sharedtools.SettingSearchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach

class SettingsSearchViewModel(
    searchRegistryItems: List<SearchRegistryItem>,
) : ViewModel() {

    private val items = searchRegistryItems.flatMap { it.addSearchItems() }

    val textFieldState = TextFieldState("")

    val searchResults = mutableStateListOf<SettingSearchItem>()

    init {
        snapshotFlow { textFieldState.text }
            .mapLatest { query ->
                if (query.isBlank()) emptyList()
                else items.filter { item ->
                    item.displayName.contains(query, ignoreCase = true)
                            || item.keywords.any { it.contains(query, ignoreCase = true) }
                }
            }
            .flowOn(Dispatchers.IO)
            .onEach {
                searchResults.clear()
                searchResults.addAll(it)
            }
            .launchIn(viewModelScope)
    }
}

class DefaultSettingsItems : SearchRegistryItem {
    override fun addSearchItems(): List<SettingSearchItem> {
        return listOf(
            // -- Library
            SettingSearchItem(
                displayName = "Favorites",
                keywords = listOf("library", "favorite"),
                breadcrumb = listOf(Screen.Settings, Screen.Settings.Library, Screen.FavoriteScreen),
                targetScreen = Screen.FavoriteScreen,
                highlightKey = "favorite",
            ),
            SettingSearchItem(
                displayName = "History",
                keywords = listOf("history", "recent"),
                breadcrumb = listOf(Screen.Settings, Screen.Settings.Library, Screen.HistoryScreen),
                targetScreen = Screen.HistoryScreen,
                highlightKey = "history",
            ),
            SettingSearchItem(
                displayName = "Bookmarks",
                keywords = listOf("bookmark"),
                breadcrumb = listOf(Screen.Settings, Screen.Settings.Library, Screen.BookmarkScreen),
                targetScreen = Screen.BookmarkScreen,
                highlightKey = "bookmark",
            ),
            SettingSearchItem(
                displayName = "Notes",
                keywords = listOf("note"),
                breadcrumb = listOf(Screen.Settings, Screen.Settings.Library, Screen.NotesScreen),
                targetScreen = Screen.NotesScreen,
                highlightKey = "note",
            ),
            SettingSearchItem(
                displayName = "Custom Lists",
                keywords = listOf("custom list", "collection", "list"),
                breadcrumb = listOf(Screen.Settings, Screen.Settings.Library, Screen.CustomListScreen),
                targetScreen = Screen.CustomListScreen,
                highlightKey = "custom_list",
            ),
            SettingSearchItem(
                displayName = "Saved Notifications",
                keywords = listOf("saved", "notification"),
                breadcrumb = listOf(Screen.Settings, Screen.Settings.Library, Screen.NotificationScreen),
                targetScreen = Screen.NotificationScreen,
                highlightKey = "notification",
            ),
            // ── Appearance > Theme ──────────────────────────────────
            SettingSearchItem(
                displayName = "Light / Dark / System Mode",
                keywords = listOf("theme", "dark", "light", "night", "system"),
                breadcrumb = listOf(Screen.Settings.Appearance, Screen.ThemeSettings),
                targetScreen = Screen.ThemeSettings,
                highlightKey = "theme_mode",
            ),
            SettingSearchItem(
                displayName = "Theme Color",
                keywords = listOf("color", "seed", "palette", "material you"),
                breadcrumb = listOf(Screen.Settings.Appearance, Screen.ThemeSettings),
                targetScreen = Screen.ThemeSettings,
                highlightKey = "theme_color",
            ),
            SettingSearchItem(
                displayName = "AMOLED Mode",
                keywords = listOf("amoled", "black", "oled", "dark"),
                breadcrumb = listOf(Screen.Settings.Appearance, Screen.ThemeSettings),
                targetScreen = Screen.ThemeSettings,
                highlightKey = "amoled_mode",
            ),
            SettingSearchItem(
                displayName = "Expressiveness",
                keywords = listOf("animation", "expressive", "motion"),
                breadcrumb = listOf(Screen.Settings.Appearance, Screen.ThemeSettings),
                targetScreen = Screen.ThemeSettings,
                highlightKey = "expressiveness",
            ),
            // ── Appearance > Colors ──────────────────────────────────
            SettingSearchItem(
                displayName = "Color Blindness",
                keywords = listOf("protanopia", "deuteranopia", "tritanopia", "accessibility"),
                breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Colors),
                targetScreen = Screen.Settings.Colors,
                highlightKey = "color_blindness",
            ),
            SettingSearchItem(
                displayName = "Use Palette",
                keywords = listOf("palette", "image color", "dynamic color"),
                breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Colors),
                targetScreen = Screen.Settings.Colors,
                highlightKey = "use_palette",
            ),
            SettingSearchItem(
                displayName = "Swatch Type",
                keywords = listOf("swatch", "vibrant", "muted"),
                breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Colors),
                targetScreen = Screen.Settings.Colors,
                highlightKey = "swatch_type",
            ),
            SettingSearchItem(
                displayName = "Swatch Style",
                keywords = listOf("swatch style", "palette style"),
                breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Colors),
                targetScreen = Screen.Settings.Colors,
                highlightKey = "swatch_style",
            ),
            // ── Appearance > Blur ────────────────────────────────────
            SettingSearchItem(
                displayName = "Show Blur",
                keywords = listOf("blur", "glassmorphism", "glass", "haze"),
                breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Blur),
                targetScreen = Screen.Settings.Blur,
                highlightKey = "show_blur",
            ),
            SettingSearchItem(
                displayName = "Blur Kind",
                keywords = listOf("blur kind", "haze", "liquid glass"),
                breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Blur),
                targetScreen = Screen.Settings.Blur,
                highlightKey = "blur_kind",
            ),
            // ── Behavior > Layout ────────────────────────────────────
            SettingSearchItem(
                displayName = "Grid Type",
                keywords = listOf("grid", "columns", "adaptive", "fixed"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.Layout),
                targetScreen = Screen.Settings.Layout,
                highlightKey = "grid_type",
            ),
            SettingSearchItem(
                displayName = "Detail Pane",
                keywords = listOf("detail pane", "split", "two pane"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.Layout),
                targetScreen = Screen.Settings.Layout,
                highlightKey = "detail_pane",
            ),
            SettingSearchItem(
                displayName = "Floating Navigation",
                keywords = listOf("floating", "nav bar", "navigation"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.Layout),
                targetScreen = Screen.Settings.Layout,
                highlightKey = "floating_navigation",
            ),
            SettingSearchItem(
                displayName = "Middle Navigation Action",
                keywords = listOf("middle button", "navigation action", "center"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.Layout),
                targetScreen = Screen.Settings.Layout,
                highlightKey = "middle_nav_action",
            ),
            // ── Behavior > Content & Reading ─────────────────────────
            SettingSearchItem(
                displayName = "History Save Count",
                keywords = listOf("history", "save", "limit", "count"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.ContentReading),
                targetScreen = Screen.Settings.ContentReading,
                highlightKey = "history_save",
            ),
            // ── Behavior > Content & Reading > Details ───────────────
            SettingSearchItem(
                displayName = "Chapter Swipe: Start to End",
                keywords = listOf("swipe", "chapter", "gesture"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.ContentReading, Screen.DetailsSettings),
                targetScreen = Screen.DetailsSettings,
                highlightKey = "swipe_start_end",
            ),
            SettingSearchItem(
                displayName = "Chapter Swipe: End to Start",
                keywords = listOf("swipe", "chapter", "gesture"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.ContentReading, Screen.DetailsSettings),
                targetScreen = Screen.DetailsSettings,
                highlightKey = "swipe_end_start",
            ),
            SettingSearchItem(
                displayName = "Share Chapters",
                keywords = listOf("share", "chapter"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.ContentReading, Screen.DetailsSettings),
                targetScreen = Screen.DetailsSettings,
                highlightKey = "share_chapters",
            ),
            SettingSearchItem(
                displayName = "Show Download Button",
                keywords = listOf("download", "button"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.ContentReading, Screen.DetailsSettings),
                targetScreen = Screen.DetailsSettings,
                highlightKey = "show_download",
            ),
            // ── Behavior > Content & Reading > Player ─────────────────
            SettingSearchItem(
                displayName = "Battery Alert Percentage",
                keywords = listOf("battery", "alert", "low battery"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.ContentReading, Screen.OtherSettings),
                targetScreen = Screen.OtherSettings,
                highlightKey = "battery_percent",
            ),
            // ── Behavior > Notifications ─────────────────────────────
            SettingSearchItem(
                displayName = "Enable Periodic Updates",
                keywords = listOf("notifications", "update check", "background"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
                targetScreen = Screen.NotificationsSettings,
                highlightKey = "periodic_updates",
            ),
            SettingSearchItem(
                displayName = "Check Interval",
                keywords = listOf("check interval", "hours", "frequency"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
                targetScreen = Screen.NotificationsSettings,
                highlightKey = "check_interval",
            ),
            SettingSearchItem(
                displayName = "Network Type",
                keywords = listOf("wifi", "metered", "unmetered", "network"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
                targetScreen = Screen.NotificationsSettings,
                highlightKey = "network_type",
            ),
            SettingSearchItem(
                displayName = "Only Run When Charging",
                keywords = listOf("charging", "battery", "power"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
                targetScreen = Screen.NotificationsSettings,
                highlightKey = "requires_charging",
            ),
            SettingSearchItem(
                displayName = "Don't Run on Low Battery",
                keywords = listOf("low battery", "battery not low"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
                targetScreen = Screen.NotificationsSettings,
                highlightKey = "battery_not_low",
            ),
            SettingSearchItem(
                displayName = "Notify on Boot",
                keywords = listOf("boot", "startup", "restart"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
                targetScreen = Screen.NotificationsSettings,
                highlightKey = "notify_boot",
            ),
            // ── Behavior > Privacy & Security ────────────────────────
            SettingSearchItem(
                displayName = "Biometric Lock",
                keywords = listOf("biometric", "fingerprint", "face", "lock", "pin"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.PrivacySecurity, Screen.SecuritySettings),
                targetScreen = Screen.SecuritySettings,
                highlightKey = "biometric",
            ),
            SettingSearchItem(
                displayName = "Incognito Sources",
                keywords = listOf("incognito", "private", "history", "tracking"),
                breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.PrivacySecurity, Screen.IncognitoScreen),
                targetScreen = Screen.IncognitoScreen,
                highlightKey = "incognito",
            ),
            // ── Data Management ──────────────────────────────────────
            SettingSearchItem(
                displayName = "Create Full Backup",
                keywords = listOf("backup", "export", "save data"),
                breadcrumb = listOf(Screen.Settings.Data, Screen.MoreSettings),
                targetScreen = Screen.MoreSettings,
                highlightKey = "create_backup",
            ),
            SettingSearchItem(
                displayName = "Restore Full Backup",
                keywords = listOf("restore", "import", "backup"),
                breadcrumb = listOf(Screen.Settings.Data, Screen.MoreSettings),
                targetScreen = Screen.MoreSettings,
                highlightKey = "restore_backup",
            ),
        )
    }
}
