package com.programmersbox.kmpuiviews.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation3.runtime.NavKey
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.navactions.TopLevelBackStack
import com.programmersbox.kmpuiviews.utils.KmpLocalCompositionSetup
import io.kamel.core.ExperimentalKamelApi
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class, ExperimentalKamelApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OtakuMaterialTheme(
    navController: NavHostController,
    navBackStack: TopLevelBackStack<NavKey>,
    settingsHandling: NewSettingsHandling,
    content: @Composable () -> Unit,
) {
    KmpLocalCompositionSetup(navController, navBackStack) {
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
