package com.programmersbox.uiviews.theme

import android.os.Build
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicColorScheme
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SystemThemeMode
import com.programmersbox.datastore.ThemeColor
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.BlurHashDao
import com.programmersbox.favoritesdatabase.BlurHashDatabase
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.datastore.rememberSwatchStyle
import com.programmersbox.uiviews.presentation.components.seedColor
import com.programmersbox.uiviews.repository.CurrentSourceRepository
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.LocalSystemDateTimeFormat
import com.programmersbox.uiviews.utils.animate
import com.programmersbox.uiviews.utils.getSystemDateTimeFormat
import com.programmersbox.uiviews.utils.navigateChromeCustomTabs
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

@OptIn(KoinExperimentalAPI::class, ExperimentalKamelApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OtakuMaterialTheme(
    navController: NavHostController,
    genericInfo: GenericInfo,
    isAmoledMode: Boolean = false,
    settingsHandling: NewSettingsHandling,
    content: @Composable () -> Unit,
) {
    val defaultUriHandler = LocalUriHandler.current

    val themeSetting by settingsHandling.rememberSystemThemeMode()
    val themeColor by settingsHandling.rememberThemeColor()

    KoinAndroidContext {
        val context = LocalContext.current
        val darkTheme = when (themeSetting) {
            SystemThemeMode.FollowSystem -> isSystemInDarkTheme()
            SystemThemeMode.Day -> false
            SystemThemeMode.Night -> true
            else -> isSystemInDarkTheme()
        }

        val colorScheme = if (themeColor == ThemeColor.Dynamic) {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
                darkTheme -> darkColorScheme(
                    primary = Color(0xff90CAF9),
                    secondary = Color(0xff90CAF9)
                )

                else -> expressiveLightColorScheme(
                    //primary = Color(0xff2196F3),
                    //secondary = Color(0xff90CAF9)
                )
            }.let {
                if (isAmoledMode && darkTheme) {
                    it.copy(
                        surface = Color.Black,
                        onSurface = Color.White,
                        background = Color.Black,
                        onBackground = Color.White,
                    )
                } else {
                    it
                }
            }
        } else {
            val swatchStyle by rememberSwatchStyle()

            rememberDynamicColorScheme(
                seedColor = themeColor.seedColor,
                isAmoled = isAmoledMode,
                isDark = darkTheme,
                style = swatchStyle,
            )
        }

        MaterialExpressiveTheme(
            colorScheme = animateColorScheme(colorScheme, tween()),
            motionScheme = if (settingsHandling.rememberShowExpressiveness().value) MotionScheme.expressive()
            else MotionScheme.standard(),
        ) {
            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalGenericInfo provides genericInfo,
                LocalSettingsHandling provides koinInject(),
                LocalItemDao provides koinInject<ItemDatabase>().itemDao(),
                LocalBlurDao provides koinInject<BlurHashDatabase>().blurDao(),
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
val LocalBlurDao = staticCompositionLocalOf<BlurHashDao> { error("nothing here") }
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