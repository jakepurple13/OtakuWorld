package com.programmersbox.uiviews.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.theme.generateColorScheme
import com.programmersbox.kmpuiviews.utils.KmpLocalCompositionSetup
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.LocalGenericInfo
import io.kamel.core.ExperimentalKamelApi
import org.koin.androidx.compose.KoinAndroidContext
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
        KmpLocalCompositionSetup(navController) {
            CompositionLocalProvider(
                LocalGenericInfo provides genericInfo,
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
