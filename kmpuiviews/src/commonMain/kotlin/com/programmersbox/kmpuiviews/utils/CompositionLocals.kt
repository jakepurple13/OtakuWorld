package com.programmersbox.kmpuiviews.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import androidx.navigation3.runtime.NavKey
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.BlurHashDao
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.DateTimeFormatHandler
import com.programmersbox.kmpuiviews.customKamelConfig
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation2Actions
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.navactions.TopLevelBackStack
import com.programmersbox.kmpuiviews.rememberCustomUriHandler
import com.programmersbox.kmpuiviews.repository.CurrentSourceRepository
import io.kamel.image.config.LocalKamelConfig
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.DateTimeFormat
import org.koin.compose.koinInject

val LocalNavHostPadding = staticCompositionLocalOf<PaddingValues> { error("") }
val LocalNavActions = staticCompositionLocalOf<NavigationActions> { error("No NavController Found!") }
val LocalItemDao = staticCompositionLocalOf<ItemDao> { error("nothing here") }
val LocalBlurDao = staticCompositionLocalOf<BlurHashDao> { error("nothing here") }
val LocalHistoryDao = staticCompositionLocalOf<HistoryDao> { error("nothing here") }
val LocalCustomListDao = staticCompositionLocalOf<ListDao> { error("nothing here") }
val LocalSettingsHandling = staticCompositionLocalOf<NewSettingsHandling> { error("Not Set") }
val LocalCurrentSource = staticCompositionLocalOf<CurrentSourceRepository> { CurrentSourceRepository() }
val LocalSourcesRepository = staticCompositionLocalOf<SourceRepository> { error("nothing here") }
val LocalSystemDateTimeFormat = staticCompositionLocalOf<DateTimeFormat<LocalDateTime>> { error("Nothing here!") }

@Composable
fun KmpLocalCompositionSetup(
    navController: NavHostController,
    navBackStack: TopLevelBackStack<NavKey>,
    actions: NavigationActions = if (USE_NAV3)
        Navigation3Actions(navBackStack)
    else
        Navigation2Actions(navController),
    content: @Composable () -> Unit,
) {
    val defaultUriHandler = LocalUriHandler.current
    val customUriHandler = rememberCustomUriHandler()
    val dateTimeFormatHandler: DateTimeFormatHandler = koinInject()

    CompositionLocalProvider(
        LocalNavActions provides actions,
        LocalItemDao provides koinInject(),
        LocalBlurDao provides koinInject(),
        LocalHistoryDao provides koinInject(),
        LocalCustomListDao provides koinInject(),
        LocalSettingsHandling provides koinInject(),
        LocalCurrentSource provides koinInject(),
        LocalSourcesRepository provides koinInject(),
        LocalKamelConfig provides customKamelConfig(),
        LocalSystemDateTimeFormat provides DateTimeFormatItem(isUsing24HourTime = dateTimeFormatHandler.is24Time()),
        LocalUriHandler provides remember {
            object : UriHandler {
                override fun openUri(uri: String) {
                    runCatching { customUriHandler.openUri(uri) }
                        .onFailure { it.printStackTrace() }
                        .recoverCatching { defaultUriHandler.openUri(uri) }
                        .onFailure { it.printStackTrace() }
                        .onFailure { actions.webView(uri) }
                }
            }
        },
        content = content
    )
}