package com.programmersbox.uiviews.utils

import android.net.Uri
import androidx.navigation.NavController
import com.programmersbox.gsonutils.toJson
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel

sealed class Screen(val route: String) {

    data object RecentScreen : Screen("recent")
    data object AllScreen : Screen("all")
    data object Settings : Screen("settings")
    data object SettingsScreen : Screen("settings_screen")
    data object GeneralSettings : Screen("general_settings")
    data object OtherSettings : Screen("others_settings")
    data object MoreInfoSettings : Screen("more_info_settings")
    data object NotificationsSettings : Screen("notifications_settings")
    data object DetailsScreen : Screen("details")
    data object NotificationScreen : Screen("notifications")
    data object HistoryScreen : Screen("history")
    data object FavoriteScreen : Screen("favorite")
    data object AboutScreen : Screen("about")
    data object DebugScreen : Screen("debug")
    data object CustomListScreen : Screen("custom_list")

    data object ImportListScreen : Screen("import_list") {
        fun navigate(navController: NavController, uri: Uri) {
            navController.navigate("$route?uri=$uri") { launchSingleTop = true }
        }
    }

    data object GlobalSearchScreen : Screen("global_search") {
        fun navigate(navController: NavController, title: String? = null) {
            navController.navigate("$route?searchFor=$title") { launchSingleTop = true }
        }
    }

    data object ExtensionListScreen : Screen("extension_list")
}

fun NavController.navigateToDetails(model: ItemModel) = navigate(
    Screen.DetailsScreen.route + "/${Uri.encode(model.toJson(ApiService::class.java to ApiServiceSerializer()))}"
) { launchSingleTop = true }
