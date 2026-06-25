package com.programmersbox.kmpuiviews.presentation.settings.search

import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen

// TODO: compile after Task 5 — Screen.Settings.* keys (Appearance, Library, etc.) are added there
object SettingsScreenDisplayNames {

    fun displayNameFor(screen: NavKey): String = when (screen) {
        // Existing screens
        Screen.ThemeSettings -> "Theme"
        Screen.DetailsSettings -> "Details"
        Screen.NotificationsSettings -> "Notifications"
        Screen.SecuritySettings -> "Security"
        Screen.MoreSettings -> "Backup & Restore"
        Screen.AccountInfo -> "Account"
        Screen.WorkerInfoScreen -> "Worker Info"
        Screen.ExceptionScreen -> "Exceptions"
        Screen.PrereleaseScreen -> "Pre-release Builds"
        Screen.ColorHelper -> "Color Helper"
        Screen.IncognitoScreen -> "Incognito Sources"
        Screen.DebugScreen -> "Debug Menu"
        Screen.OtherSettings -> "Player"
        // New Screen.Settings.* keys — added in Task 5
        Screen.Settings -> "Settings"
        Screen.Settings.Library -> "Library"
        Screen.Settings.Discover -> "Discover"
        Screen.Settings.Sources -> "Sources & Extensions"
        Screen.Settings.Integrations -> "Integrations"
        Screen.Settings.Appearance -> "Appearance"
        Screen.Settings.Colors -> "Colors"
        Screen.Settings.Behavior -> "Behavior"
        Screen.Settings.Layout -> "Layout"
        Screen.Settings.ContentReading -> "Content & Reading"
        Screen.Settings.PrivacySecurity -> "Privacy & Security"
        Screen.Settings.Data -> "Data Management"
        Screen.Settings.About -> "About"
        Screen.Settings.Diagnostics -> "Diagnostics"
        Screen.Settings.Developer -> "Developer"
        else -> screen::class.simpleName ?: "Unknown"
    }

    fun breadcrumbText(breadcrumb: List<NavKey>): String =
        breadcrumb.joinToString(" > ") { displayNameFor(it) }
}
