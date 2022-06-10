package com.programmersbox.uiviews.utils

import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.programmersbox.gsonutils.toJson
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel

sealed class Screen(val route: String) {

    object RecentScreen : Screen("recent")
    object AllScreen : Screen("all")
    object SettingsScreen : Screen("settings")
    object DetailsScreen : Screen("details")
    object NotificationScreen : Screen("notifications")
    object HistoryScreen : Screen("history")
    object GlobalSearchScreen : Screen("global_search")
    object FavoriteScreen : Screen("favorite")
    object DebugScreen : Screen("debug")

    companion object {
        val bottomItems = listOf(RecentScreen, AllScreen, SettingsScreen)
    }

}

fun NavController.navigateToDetails(model: ItemModel) = navigate(
    Screen.DetailsScreen.route + "/${Uri.encode(model.toJson(ApiService::class.java to ApiServiceSerializer()))}"
)

@Composable
fun OtakuMaterialTheme(navController: NavHostController, content: @Composable () -> Unit) {
    val context = LocalContext.current
    MaterialTheme(currentColorScheme) {
        CompositionLocalProvider(
            LocalActivity provides remember { context.findActivity() },
            LocalNavController provides navController,
        ) { content() }
    }
}