package com.programmersbox.uiviews.utils

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.gsonutils.toJson
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.uiviews.CurrentSourceRepository
import com.programmersbox.uiviews.GenericInfo
import org.koin.compose.koinInject
import java.util.UUID

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
    data object CustomListItemScreen : Screen("custom_list_item") {
        fun navigate(navController: NavController, uuid: UUID) {
            navController.navigate("$route/$uuid") { launchSingleTop = true }
        }
    }

    data object ImportListScreen : Screen("import_list") {
        fun navigate(navController: NavController, uri: Uri) {
            navController.navigate("$route?uri=$uri") { launchSingleTop = true }
        }
    }

    data object TranslationScreen : Screen("translation_models")
    data object GlobalSearchScreen : Screen("global_search") {
        fun navigate(navController: NavController, title: String? = null) {
            navController.navigate("$route?searchFor=$title") { launchSingleTop = true }
        }
    }

    data object SourceChooserScreen : Screen("source_chooser")
    data object ExtensionListScreen : Screen("extension_list")
}

fun NavController.navigateToDetails(model: ItemModel) = navigate(
    Screen.DetailsScreen.route + "/${Uri.encode(model.toJson(ApiService::class.java to ApiServiceSerializer()))}"
) { launchSingleTop = true }

@Composable
fun OtakuMaterialTheme(
    navController: NavHostController,
    genericInfo: GenericInfo,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES ||
            (isSystemInDarkTheme() && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    DisposableEffect(darkTheme) {
        (context as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) { darkTheme },
        )
        onDispose {}
    }

    MaterialTheme(currentColorScheme) {
        androidx.compose.material.MaterialTheme(
            colors = if (darkTheme)
                darkColors(
                    primary = Color(0xff90CAF9),
                    secondary = Color(0xff90CAF9)
                )
            else
                lightColors(
                    primary = Color(0xff2196F3),
                    secondary = Color(0xff90CAF9)
                ),
        ) {
            CompositionLocalProvider(
                LocalActivity provides remember { context.findActivity() },
                LocalNavController provides navController,
                LocalGenericInfo provides genericInfo,
                LocalSettingsHandling provides koinInject(),
                LocalItemDao provides remember { ItemDatabase.getInstance(context).itemDao() },
                LocalHistoryDao provides remember { HistoryDatabase.getInstance(context).historyDao() },
                LocalCustomListDao provides remember { ListDatabase.getInstance(context).listDao() },
                LocalSystemDateTimeFormat provides remember { context.getSystemDateTimeFormat() },
                LocalSourcesRepository provides koinInject(),
                LocalCurrentSource provides koinInject()
            ) { content() }
        }
    }
}

val LocalItemDao = staticCompositionLocalOf<ItemDao> { error("nothing here") }
val LocalHistoryDao = staticCompositionLocalOf<HistoryDao> { error("nothing here") }
val LocalCustomListDao = staticCompositionLocalOf<ListDao> { error("nothing here") }
val LocalSourcesRepository = staticCompositionLocalOf<SourceRepository> { error("nothing here") }
val LocalCurrentSource = staticCompositionLocalOf<CurrentSourceRepository> { CurrentSourceRepository() }
