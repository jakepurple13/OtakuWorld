package com.programmersbox.kmpuiviews.presentation.settings.search

import com.programmersbox.kmpuiviews.presentation.Screen

class SettingsSearchRegistry(val items: List<SettingsSearchItem>) {

    fun search(query: String): List<SettingsSearchItem> {
        if (query.isBlank()) return emptyList()
        return items.filter { item ->
            item.displayName.contains(query, ignoreCase = true)
                || item.keywords.any { it.contains(query, ignoreCase = true) }
        }
    }
}

fun builtInSettingsItems(): List<SettingsSearchItem> = listOf(
    // ── Appearance > Theme ──────────────────────────────────
    SettingsSearchItem(
        displayName = "Light / Dark / System Mode",
        keywords = listOf("theme", "dark", "light", "night", "system"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.ThemeSettings),
        targetScreen = Screen.ThemeSettings,
        highlightKey = "theme_mode",
    ),
    SettingsSearchItem(
        displayName = "Theme Color",
        keywords = listOf("color", "seed", "palette", "material you"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.ThemeSettings),
        targetScreen = Screen.ThemeSettings,
        highlightKey = "theme_color",
    ),
    SettingsSearchItem(
        displayName = "AMOLED Mode",
        keywords = listOf("amoled", "black", "oled", "dark"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.ThemeSettings),
        targetScreen = Screen.ThemeSettings,
        highlightKey = "amoled_mode",
    ),
    SettingsSearchItem(
        displayName = "Expressiveness",
        keywords = listOf("animation", "expressive", "motion"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.ThemeSettings),
        targetScreen = Screen.ThemeSettings,
        highlightKey = "expressiveness",
    ),
    // ── Appearance > Colors ──────────────────────────────────
    SettingsSearchItem(
        displayName = "Color Blindness",
        keywords = listOf("protanopia", "deuteranopia", "tritanopia", "accessibility"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Colors),
        targetScreen = Screen.Settings.Colors,
        highlightKey = "color_blindness",
    ),
    SettingsSearchItem(
        displayName = "Use Palette",
        keywords = listOf("palette", "image color", "dynamic color"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Colors),
        targetScreen = Screen.Settings.Colors,
        highlightKey = "use_palette",
    ),
    SettingsSearchItem(
        displayName = "Swatch Type",
        keywords = listOf("swatch", "vibrant", "muted"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Colors),
        targetScreen = Screen.Settings.Colors,
        highlightKey = "swatch_type",
    ),
    SettingsSearchItem(
        displayName = "Swatch Style",
        keywords = listOf("swatch style", "palette style"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Colors),
        targetScreen = Screen.Settings.Colors,
        highlightKey = "swatch_style",
    ),
    // ── Appearance > Blur ────────────────────────────────────
    SettingsSearchItem(
        displayName = "Show Blur",
        keywords = listOf("blur", "glassmorphism", "glass", "haze"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Blur),
        targetScreen = Screen.Settings.Blur,
        highlightKey = "show_blur",
    ),
    SettingsSearchItem(
        displayName = "Blur Kind",
        keywords = listOf("blur kind", "haze", "liquid glass"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Blur),
        targetScreen = Screen.Settings.Blur,
        highlightKey = "blur_kind",
    ),
    // ── Behavior > Layout ────────────────────────────────────
    SettingsSearchItem(
        displayName = "Grid Type",
        keywords = listOf("grid", "columns", "adaptive", "fixed"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.Layout),
        targetScreen = Screen.Settings.Layout,
        highlightKey = "grid_type",
    ),
    SettingsSearchItem(
        displayName = "Detail Pane",
        keywords = listOf("detail pane", "split", "two pane"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.Layout),
        targetScreen = Screen.Settings.Layout,
        highlightKey = "detail_pane",
    ),
    SettingsSearchItem(
        displayName = "Floating Navigation",
        keywords = listOf("floating", "nav bar", "navigation"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.Layout),
        targetScreen = Screen.Settings.Layout,
        highlightKey = "floating_navigation",
    ),
    SettingsSearchItem(
        displayName = "Middle Navigation Action",
        keywords = listOf("middle button", "navigation action", "center"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.Layout),
        targetScreen = Screen.Settings.Layout,
        highlightKey = "middle_nav_action",
    ),
    // ── Behavior > Content & Reading ─────────────────────────
    SettingsSearchItem(
        displayName = "History Save Count",
        keywords = listOf("history", "save", "limit", "count"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.ContentReading),
        targetScreen = Screen.Settings.ContentReading,
        highlightKey = "history_save",
    ),
    // ── Behavior > Content & Reading > Details ───────────────
    SettingsSearchItem(
        displayName = "Chapter Swipe: Start to End",
        keywords = listOf("swipe", "chapter", "gesture"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.ContentReading, Screen.DetailsSettings),
        targetScreen = Screen.DetailsSettings,
        highlightKey = "swipe_start_end",
    ),
    SettingsSearchItem(
        displayName = "Chapter Swipe: End to Start",
        keywords = listOf("swipe", "chapter", "gesture"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.ContentReading, Screen.DetailsSettings),
        targetScreen = Screen.DetailsSettings,
        highlightKey = "swipe_end_start",
    ),
    SettingsSearchItem(
        displayName = "Share Chapters",
        keywords = listOf("share", "chapter"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.ContentReading, Screen.DetailsSettings),
        targetScreen = Screen.DetailsSettings,
        highlightKey = "share_chapters",
    ),
    SettingsSearchItem(
        displayName = "Show Download Button",
        keywords = listOf("download", "button"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.ContentReading, Screen.DetailsSettings),
        targetScreen = Screen.DetailsSettings,
        highlightKey = "show_download",
    ),
    // ── Behavior > Content & Reading > Player ─────────────────
    SettingsSearchItem(
        displayName = "Battery Alert Percentage",
        keywords = listOf("battery", "alert", "low battery"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.ContentReading, Screen.OtherSettings),
        targetScreen = Screen.OtherSettings,
        highlightKey = "battery_percent",
    ),
    // ── Behavior > Notifications ─────────────────────────────
    SettingsSearchItem(
        displayName = "Enable Periodic Updates",
        keywords = listOf("notifications", "update check", "background"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
        targetScreen = Screen.NotificationsSettings,
        highlightKey = "periodic_updates",
    ),
    SettingsSearchItem(
        displayName = "Check Interval",
        keywords = listOf("check interval", "hours", "frequency"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
        targetScreen = Screen.NotificationsSettings,
        highlightKey = "check_interval",
    ),
    SettingsSearchItem(
        displayName = "Network Type",
        keywords = listOf("wifi", "metered", "unmetered", "network"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
        targetScreen = Screen.NotificationsSettings,
        highlightKey = "network_type",
    ),
    SettingsSearchItem(
        displayName = "Only Run When Charging",
        keywords = listOf("charging", "battery", "power"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
        targetScreen = Screen.NotificationsSettings,
        highlightKey = "requires_charging",
    ),
    SettingsSearchItem(
        displayName = "Don't Run on Low Battery",
        keywords = listOf("low battery", "battery not low"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
        targetScreen = Screen.NotificationsSettings,
        highlightKey = "battery_not_low",
    ),
    SettingsSearchItem(
        displayName = "Notify on Boot",
        keywords = listOf("boot", "startup", "restart"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
        targetScreen = Screen.NotificationsSettings,
        highlightKey = "notify_boot",
    ),
    // ── Behavior > Privacy & Security ────────────────────────
    SettingsSearchItem(
        displayName = "Biometric Lock",
        keywords = listOf("biometric", "fingerprint", "face", "lock", "pin"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.PrivacySecurity, Screen.SecuritySettings),
        targetScreen = Screen.SecuritySettings,
        highlightKey = "biometric",
    ),
    SettingsSearchItem(
        displayName = "Incognito Sources",
        keywords = listOf("incognito", "private", "history", "tracking"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.PrivacySecurity, Screen.IncognitoScreen),
        targetScreen = Screen.IncognitoScreen,
        highlightKey = "incognito",
    ),
    // ── Data Management ──────────────────────────────────────
    SettingsSearchItem(
        displayName = "Create Full Backup",
        keywords = listOf("backup", "export", "save data"),
        breadcrumb = listOf(Screen.Settings.Data, Screen.MoreSettings),
        targetScreen = Screen.MoreSettings,
        highlightKey = "create_backup",
    ),
    SettingsSearchItem(
        displayName = "Restore Full Backup",
        keywords = listOf("restore", "import", "backup"),
        breadcrumb = listOf(Screen.Settings.Data, Screen.MoreSettings),
        targetScreen = Screen.MoreSettings,
        highlightKey = "restore_backup",
    ),
)
