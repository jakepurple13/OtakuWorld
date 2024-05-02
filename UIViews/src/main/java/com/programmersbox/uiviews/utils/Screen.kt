package com.programmersbox.uiviews.utils

import androidx.navigation.NavController
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.models.ItemModel
import com.programmersbox.uiviews.GenericInfo
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
            ): ItemModel? = (sourceRepository.toSourceByApiServiceName(source)?.apiService ?: genericInfo.toSource(source))
                ?.let {
                    ItemModel(
                        title = title,
                        description = description,
                        url = url,
                        imageUrl = imageUrl,
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
}

fun NavController.navigateToDetails(model: ItemModel) = navigate(
    Screen.DetailsScreen.Details(
        title = model.title,
        description = model.description,
        url = model.url,
        imageUrl = model.imageUrl,
        source = model.source.serviceName
    )
) { launchSingleTop = true }
