package com.programmersbox.uiviews.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.BlurHashDao
import com.programmersbox.favoritesdatabase.BlurHashDatabase
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.theme.generateColorScheme
import com.programmersbox.kmpuiviews.utils.LocalNavController
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.repository.CurrentSourceRepository
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.LocalSystemDateTimeFormat
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
    settingsHandling: NewSettingsHandling,
    content: @Composable () -> Unit,
) {
    val defaultUriHandler = LocalUriHandler.current

    KoinAndroidContext {
        val context = LocalContext.current
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
                        }
                            .recoverCatching { defaultUriHandler.openUri(uri) }
                            .onFailure { navController.navigate(Screen.WebViewScreen(uri)) }
                    }
                }
            }
        ) {
            MaterialExpressiveTheme(
                colorScheme = generateColorScheme(settingsHandling),
                motionScheme = if (settingsHandling.rememberShowExpressiveness().value)
                    MotionScheme.expressive()
                else
                    MotionScheme.standard(),
                content = content
            )
        }
    }
}

val LocalItemDao = staticCompositionLocalOf<ItemDao> { error("nothing here") }
val LocalBlurDao = staticCompositionLocalOf<BlurHashDao> { error("nothing here") }
val LocalHistoryDao = staticCompositionLocalOf<HistoryDao> { error("nothing here") }
val LocalCustomListDao = staticCompositionLocalOf<ListDao> { error("nothing here") }
val LocalSourcesRepository = staticCompositionLocalOf<SourceRepository> { error("nothing here") }
val LocalCurrentSource = staticCompositionLocalOf<CurrentSourceRepository> { CurrentSourceRepository() }