package com.programmersbox.uiviews.utils

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
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
import com.programmersbox.uiviews.SystemThemeMode
import io.kamel.core.ExperimentalKamelApi
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.takeFrom
import io.kamel.image.config.Default
import io.kamel.image.config.LocalKamelConfig
import io.kamel.image.config.animatedImageDecoder
import io.kamel.image.config.imageBitmapResizingDecoder
import io.kamel.image.config.resourcesFetcher
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class, ExperimentalKamelApi::class)
@Composable
fun OtakuMaterialTheme(
    navController: NavHostController,
    genericInfo: GenericInfo,
    isAmoledMode: Boolean = false,
    themeSetting: SystemThemeMode,
    content: @Composable () -> Unit,
) {
    val defaultUriHandler = LocalUriHandler.current

    KoinAndroidContext {
        val context = LocalContext.current
        val darkTheme = when (themeSetting) {
            SystemThemeMode.FollowSystem -> isSystemInDarkTheme()
            SystemThemeMode.Day -> false
            SystemThemeMode.Night -> true
            SystemThemeMode.UNRECOGNIZED -> isSystemInDarkTheme()
        }

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

        var colorScheme = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
            darkTheme -> darkColorScheme(
                primary = Color(0xff90CAF9),
                secondary = Color(0xff90CAF9)
            )

            else -> lightColorScheme(
                primary = Color(0xff2196F3),
                secondary = Color(0xff90CAF9)
            )
        }

        if (isAmoledMode && darkTheme) {
            colorScheme = colorScheme.copy(
                surface = Color.Black,
                inverseSurface = Color.White,
                background = Color.Black
            )
        }

        MaterialTheme(colorScheme.animate()) {
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
                LocalKamelConfig provides KamelConfig {
                    takeFrom(KamelConfig.Default)
                    imageBitmapResizingDecoder()
                    animatedImageDecoder()
                    resourcesFetcher(context)
                },
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

val LocalItemDao = staticCompositionLocalOf<ItemDao> { error("nothing here") }
val LocalHistoryDao = staticCompositionLocalOf<HistoryDao> { error("nothing here") }
val LocalCustomListDao = staticCompositionLocalOf<ListDao> { error("nothing here") }
val LocalSourcesRepository = staticCompositionLocalOf<SourceRepository> { error("nothing here") }
val LocalCurrentSource = staticCompositionLocalOf<CurrentSourceRepository> { CurrentSourceRepository() }

@Composable
private fun ColorScheme.animate() = copy(
    primary.animate().value,
    onPrimary.animate().value,
    primaryContainer.animate().value,
    onPrimaryContainer.animate().value,
    inversePrimary.animate().value,
    secondary.animate().value,
    onSecondary.animate().value,
    secondaryContainer.animate().value,
    onSecondaryContainer.animate().value,
    tertiary.animate().value,
    onTertiary.animate().value,
    tertiaryContainer.animate().value,
    onTertiaryContainer.animate().value,
    background.animate().value,
    onBackground.animate().value,
    surface.animate().value,
    onSurface.animate().value,
    surfaceVariant.animate().value,
    onSurfaceVariant.animate().value,
    surfaceTint.animate().value,
    inverseSurface.animate().value,
    inverseOnSurface.animate().value,
    error.animate().value,
    onError.animate().value,
    errorContainer.animate().value,
    onErrorContainer.animate().value,
    outline.animate().value,
)