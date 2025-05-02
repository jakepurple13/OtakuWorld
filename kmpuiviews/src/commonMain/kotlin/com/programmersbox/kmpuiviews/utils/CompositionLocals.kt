package com.programmersbox.kmpuiviews.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.BlurHashDao
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.customKamelConfig
import com.programmersbox.kmpuiviews.customUriHandler
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.repository.CurrentSourceRepository
import io.kamel.image.config.LocalKamelConfig
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

val LocalNavHostPadding = staticCompositionLocalOf<PaddingValues> { error("") }
val LocalNavController = staticCompositionLocalOf<NavHostController> { error("No NavController Found!") }
val LocalItemDao = staticCompositionLocalOf<ItemDao> { error("nothing here") }
val LocalBlurDao = staticCompositionLocalOf<BlurHashDao> { error("nothing here") }
val LocalHistoryDao = staticCompositionLocalOf<HistoryDao> { error("nothing here") }
val LocalCustomListDao = staticCompositionLocalOf<ListDao> { error("nothing here") }
val LocalSettingsHandling = staticCompositionLocalOf<NewSettingsHandling> { error("Not Set") }
val LocalCurrentSource = staticCompositionLocalOf<CurrentSourceRepository> { CurrentSourceRepository() }
val LocalSourcesRepository = staticCompositionLocalOf<SourceRepository> { error("nothing here") }

@Composable
fun KmpLocalCompositionSetup(
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    val defaultUriHandler = LocalUriHandler.current
    KoinContext {
        CompositionLocalProvider(
            LocalNavController provides navController,
            LocalItemDao provides koinInject(),
            LocalBlurDao provides koinInject(),
            LocalHistoryDao provides koinInject(),
            LocalCustomListDao provides koinInject(),
            LocalSettingsHandling provides koinInject(),
            LocalCurrentSource provides koinInject(),
            LocalSourcesRepository provides koinInject(),
            LocalKamelConfig provides customKamelConfig(),
            LocalUriHandler provides remember {
                object : UriHandler {
                    private val customHandler = customUriHandler(navController)

                    override fun openUri(uri: String) {
                        runCatching { customHandler.openUri(uri) }
                            .onFailure { it.printStackTrace() }
                            .recoverCatching { defaultUriHandler.openUri(uri) }
                            .onFailure { it.printStackTrace() }
                            .onFailure { navController.navigate(Screen.WebViewScreen(uri)) }
                    }
                }
            },
            content = content
        )
    }
}