package com.programmersbox.uiviews.utils

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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.uiviews.CurrentSourceRepository
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun OtakuMaterialTheme(
    navController: NavHostController,
    genericInfo: GenericInfo,
    content: @Composable () -> Unit,
) {
    val defaultUriHandler = LocalUriHandler.current

    KoinAndroidContext {
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
                    LocalItemDao provides koinInject<ItemDatabase>().itemDao(),
                    LocalHistoryDao provides koinInject<HistoryDatabase>().historyDao(),
                    LocalCustomListDao provides koinInject<ListDatabase>().listDao(),
                    LocalSystemDateTimeFormat provides remember { context.getSystemDateTimeFormat() },
                    LocalSourcesRepository provides koinInject(),
                    LocalCurrentSource provides koinInject(),
                    LocalUriHandler provides remember {
                        object : UriHandler {
                            override fun openUri(uri: String) {
                                runCatching {
                                    navController.navigateChromeCustomTabs(
                                        uri,
                                        {
                                            anim {
                                                enter = R.anim.slide_in_right
                                                popEnter = R.anim.slide_in_right
                                                exit = R.anim.slide_out_left
                                                popExit = R.anim.slide_out_left
                                            }
                                        }
                                    )
                                }.onFailure { defaultUriHandler.openUri(uri) }
                            }
                        }
                    }
                ) { content() }
            }
        }
    }
}

val LocalItemDao = staticCompositionLocalOf<ItemDao> { error("nothing here") }
val LocalHistoryDao = staticCompositionLocalOf<HistoryDao> { error("nothing here") }
val LocalCustomListDao = staticCompositionLocalOf<ListDao> { error("nothing here") }
val LocalSourcesRepository = staticCompositionLocalOf<SourceRepository> { error("nothing here") }
val LocalCurrentSource = staticCompositionLocalOf<CurrentSourceRepository> { CurrentSourceRepository() }
