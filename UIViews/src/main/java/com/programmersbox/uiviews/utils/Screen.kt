package com.programmersbox.uiviews.utils

import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.programmersbox.gsonutils.toJson
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.uiviews.GenericInfo

sealed class Screen(val route: String) {

    object RecentScreen : Screen("recent")
    object AllScreen : Screen("all")
    object SettingsScreen : Screen("settings")
    object DetailsScreen : Screen("details")
    object NotificationScreen : Screen("notifications")
    object HistoryScreen : Screen("history")
    object FavoriteScreen : Screen("favorite")
    object AboutScreen : Screen("about")
    object DebugScreen : Screen("debug")
    object GlobalSearchScreen : Screen("global_search") {
        fun navigate(navController: NavController, title: String? = null) {
            navController.navigate("$route?searchFor=$title") { launchSingleTop = true }
        }
    }

    companion object {
        val bottomItems = listOf(RecentScreen, AllScreen, SettingsScreen)
    }

}

fun NavController.navigateToDetails(model: ItemModel) = navigate(
    Screen.DetailsScreen.route + "/${Uri.encode(model.toJson(ApiService::class.java to ApiServiceSerializer()))}"
) { launchSingleTop = true }

@Composable
fun OtakuMaterialTheme(
    navController: NavHostController,
    genericInfo: GenericInfo,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES ||
            (isSystemInDarkTheme() && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
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
                LocalGenericInfo provides genericInfo
            ) { content() }
        }
    }
}

@Composable
fun LifecycleHandle(
    onCreate: () -> Unit = {},
    onStart: () -> Unit = {},
    onResume: () -> Unit = {},
    onPause: () -> Unit = {},
    onStop: () -> Unit = {},
    onDestroy: () -> Unit = {},
    onAny: () -> Unit = {},
    vararg keys: Any
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // If `lifecycleOwner` changes, dispose and reset the effect
    DisposableEffect(lifecycleOwner, *keys) {
        // Create an observer that triggers our remembered callbacks
        // for sending analytics events
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> onCreate
                Lifecycle.Event.ON_START -> onStart
                Lifecycle.Event.ON_RESUME -> onResume
                Lifecycle.Event.ON_PAUSE -> onPause
                Lifecycle.Event.ON_STOP -> onStop
                Lifecycle.Event.ON_DESTROY -> onDestroy
                Lifecycle.Event.ON_ANY -> onAny
            }()
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}