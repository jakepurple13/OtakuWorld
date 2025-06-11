package com.programmersbox.kmpuiviews.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.navigation.NavController.OnDestinationChangedListener
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.utils.AppConfig
import org.koin.compose.koinInject

@Composable
fun AddBreadcrumbLogging(navController: NavController) {
    val appConfig = koinInject<AppConfig>()
    if (appConfig.buildType != BuildType.NoFirebase) {
        DisposableEffect(Unit) {
            val destinationListener = OnDestinationChangedListener { _, destination, _ ->
                logFirebaseMessage("Navigated to: ${destination.route}")
            }
            navController.addOnDestinationChangedListener(destinationListener)
            onDispose { navController.removeOnDestinationChangedListener(destinationListener) }
        }
    }
}