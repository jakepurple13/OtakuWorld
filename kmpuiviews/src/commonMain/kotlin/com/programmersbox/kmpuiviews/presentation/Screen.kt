package com.programmersbox.kmpuiviews.presentation

import androidx.navigation.NavController
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.serialization.Serializable
import net.thauvin.erik.urlencoder.UrlEncoderUtil

@Serializable
sealed class Screen(val route: String) : NavKey {

    @Serializable
    data object RecentScreen : Screen("recent")

    @Serializable
    data object AllScreen : Screen("all")

    @Serializable
    data object Settings : Screen("settings") {

        @Serializable
        data object AppInfo : Screen("app_info")

        @Serializable
        data object Blur : Screen("blur")

        // Added as stubs to unblock SettingsScreenDisplayNames.kt compilation — Task 5 will formalize
        @Serializable
        data object Library : Screen("library")

        @Serializable
        data object Discover : Screen("discover")

        @Serializable
        data object Sources : Screen("sources")

        @Serializable
        data object Integrations : Screen("integrations")

        @Serializable
        data object Appearance : Screen("appearance")

        @Serializable
        data object Colors : Screen("colors")

        @Serializable
        data object Behavior : Screen("behavior")

        @Serializable
        data object Layout : Screen("layout")

        @Serializable
        data object ContentReading : Screen("content_reading")

        @Serializable
        data object PrivacySecurity : Screen("privacy_security")

        @Serializable
        data object Data : Screen("data")

        @Serializable
        data object About : Screen("about")

        @Serializable
        data object Diagnostics : Screen("diagnostics")

        @Serializable
        data object Developer : Screen("developer")
    }

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
    data object BackupWizard : Screen("backup_wizard")

    @Serializable
    data object RestoreWizard : Screen("restore_wizard")

    @Serializable
    data object NotificationsSettings : Screen("notifications_settings")

    @Serializable
    data object AccountInfo : Screen("account_info")

    @Serializable
    data object DetailsScreen : Screen("details") {
        @Serializable
        data class Details(
            val title: String,
            val description: String,
            val url: String,
            val imageUrl: String,
            val source: String,
        ) : NavKey
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
        data class DeleteFromList(val uuid: String) : NavKey

        @Serializable
        data class CustomListItem(val uuid: String) : Screen("import_list")
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
    data object OrderScreen : Screen("order")

    @Serializable
    data object DownloadInstallScreen : Screen("download_install")

    @Serializable
    data object OnboardingScreen : Screen("onboarding")

    @Serializable
    data object IncognitoScreen : Screen("incognito")

    @Serializable
    data class WebViewScreen(val url: String) : Screen("webview")

    @Serializable
    data object ScanQrCodeScreen : Screen("scan_qr_code")

    @Serializable
    data object WorkerInfoScreen : Screen("worker_info")

    @Serializable
    data object UrlOpener : Screen("url_opener")

    @Serializable
    data object ColorHelper : Screen("color_helper")

    @Serializable
    data object ThemeSettings : Screen("theme_settings")

    @Serializable
    data object DetailsSettings : Screen("details_settings")

    @Serializable
    data object SecuritySettings : Screen("security_settings")

    @Serializable
    data object ExceptionScreen : Screen("exception")

    @Serializable
    data object BookmarkScreen : Screen("bookmarks")

    @Serializable
    data object NotesScreen : Screen("notes")

    @Serializable
    data object DictionaryScreen : Screen("dictionary") {
        @Serializable
        data class Detail(val id: Long) : Screen("dictionary_detail")

        @Serializable
        data class Form(val id: Long? = null) : Screen("dictionary_form")
    }
}

fun NavController.navigateToDetails(model: KmpItemModel) = navigate(
    Screen.DetailsScreen.Details(
        title = model.title.ifEmpty { "NA" },
        description = model.description.ifEmpty { "NA" },
        url = model.url.let { UrlEncoderUtil.encode(it) },
        imageUrl = model.imageUrl.let { UrlEncoderUtil.encode(it) },
        source = model.source.serviceName
    )
) { launchSingleTop = true }

fun Screen.DetailsScreen.Details.toItemModel(
    sourceRepository: SourceRepository,
): KmpItemModel? = sourceRepository.toSourceByApiServiceName(source)?.apiService
    ?.let {
        KmpItemModel(
            title = runCatching { UrlEncoderUtil.decode(title) }.getOrDefault(title),
            description = runCatching { UrlEncoderUtil.decode(description) }.getOrDefault(description),
            url = UrlEncoderUtil.decode(url),
            imageUrl = UrlEncoderUtil.decode(imageUrl),
            source = it
        )
    }
