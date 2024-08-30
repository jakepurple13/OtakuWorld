package com.programmersbox.uiviews.utils

import android.net.Uri
import androidx.navigation.NavController
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.gsonutils.toJson
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.uiviews.GenericInfo
import kotlinx.serialization.Serializable
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
        ) {
            fun toItemModel(
                sourceRepository: SourceRepository,
                genericInfo: GenericInfo,
            ): ItemModel? = Uri.decode(source)
                .let { sourceRepository.toSourceByApiServiceName(it)?.apiService ?: genericInfo.toSource(it) }
                ?.let {
                    ItemModel(
                        title = Uri.decode(title),
                        description = Uri.decode(description),
                        url = Uri.decode(url),
                        imageUrl = Uri.decode(imageUrl),
                        source = it
                    )
                }
        }
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
    }

    @Serializable
    data class ImportListScreen(val uri: String) : Screen("import_list")

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
}

fun NavController.navigateToDetails1(model: ItemModel) = navigate(
    Screen.DetailsScreen.route + "/${Uri.encode(model.toJson(ApiService::class.java to ApiServiceSerializer()))}"
) { launchSingleTop = true }

fun NavController.navigateToDetails(model: ItemModel) = navigate(
    Screen.DetailsScreen.Details(
        title = model.title.ifEmpty { "NA" },
        description = model.description.ifEmpty { "NA" },
        url = model.url.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) },
        imageUrl = model.imageUrl.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) },
        source = model.source.serviceName
    )
) { launchSingleTop = true }
