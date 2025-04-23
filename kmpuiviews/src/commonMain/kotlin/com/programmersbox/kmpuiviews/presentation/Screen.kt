package com.programmersbox.kmpuiviews.presentation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen(val route: String) {

    @Serializable
    data object RecentScreen : Screen("recent")

    @Serializable
    data object AllScreen : Screen("all")

    @Serializable
    data object Settings : Screen("settings")

    @Serializable
    data object SettingsScreen : Screen("settings_screen")

    @Serializable
    data object GeneralSettings : Screen("general_settings")

    @Serializable
    data object OtherSettings : Screen("others_settings")

    @Serializable
    data object MoreInfoSettings : Screen("more_info_settings")

    @Serializable
    data object PrereleaseScreen : Screen("prerelease")

    @Serializable
    data object MoreSettings : Screen("more_settings")

    @Serializable
    data object NotificationsSettings : Screen("notifications_settings")

    @Serializable
    data object DetailsScreen : Screen("details") {
        @Serializable
        data class Details(
            val title: String,
            val description: String,
            val url: String,
            val imageUrl: String,
            val source: String,
        )
    }

    @Serializable
    data object NotificationScreen : Screen("notifications") {
        @Serializable
        data object Home : Screen("home")
    }

    @Serializable
    data object HistoryScreen : Screen("history")

    @Serializable
    data object FavoriteScreen : Screen("favorite") {
        @Serializable
        data object Home : Screen("home")
    }

    @Serializable
    data object AboutScreen : Screen("about")

    @Serializable
    data object DebugScreen : Screen("debug")

    @Serializable
    data object CustomListScreen : Screen("custom_list") {
        @Serializable
        data object Home : Screen("home")

        @Serializable
        data class DeleteFromList(val uuid: String)
    }

    @Serializable
    data class ImportListScreen(val uri: String) : Screen("import_list")

    @Serializable
    data class ImportFullListScreen(val uri: String) : Screen("import_full_list")

    @Serializable
    data class GlobalSearchScreen(
        val title: String? = null,
    ) : Screen("global_search") {
        @Serializable
        data class Home(val title: String? = null) : Screen("home")
    }

    @Serializable
    data object ExtensionListScreen : Screen("extension_list") {
        @Serializable
        data object Home : Screen("home")
    }

    @Serializable
    data object GeminiScreen : Screen("gemini")

    @Serializable
    data object OrderScreen : Screen("order")

    @Serializable
    data object DownloadInstallScreen : Screen("download_install")

    @Serializable
    data object OnboardingScreen : Screen("onboarding")

    @Serializable
    data object IncognitoScreen : Screen("incognito")

    @Serializable
    data class WebViewScreen(val url: String) : Screen("webview")
}