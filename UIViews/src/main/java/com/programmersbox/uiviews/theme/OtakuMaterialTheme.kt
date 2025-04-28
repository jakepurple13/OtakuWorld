package com.programmersbox.uiviews.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.theme.generateColorScheme
import com.programmersbox.kmpuiviews.utils.KmpLocalCompositionSetup
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.repository.CurrentSourceRepository
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalSystemDateTimeFormat
import com.programmersbox.uiviews.utils.getSystemDateTimeFormat
import io.kamel.core.ExperimentalKamelApi
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
    KoinAndroidContext {
        val context = LocalContext.current
        KmpLocalCompositionSetup(navController) {
            CompositionLocalProvider(
                LocalGenericInfo provides genericInfo,
                LocalSystemDateTimeFormat provides remember { context.getSystemDateTimeFormat() },
                LocalSourcesRepository provides koinInject(),
                LocalCurrentSource provides koinInject(),
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
}

val LocalSourcesRepository = staticCompositionLocalOf<SourceRepository> { error("nothing here") }
val LocalCurrentSource = staticCompositionLocalOf<CurrentSourceRepository> { CurrentSourceRepository() }