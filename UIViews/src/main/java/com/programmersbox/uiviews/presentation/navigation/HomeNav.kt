package com.programmersbox.uiviews.presentation.navigation

import android.app.Activity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.programmersbox.kmpuiviews.presentation.HomeNav
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.utils.ChromeCustomTabsNavigator
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import org.koin.compose.koinInject

@OptIn(
    ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class,
    ExperimentalHazeMaterialsApi::class
)
@Composable
fun HomeNav(
    activity: Activity,
    startDestination: Screen,
    customPreferences: ComposeSettingsDsl,
    bottomBarAdditions: @Composable () -> Unit,
    navController: NavHostController = rememberNavController(
        remember { ChromeCustomTabsNavigator(activity) }
    ),
    genericInfo: GenericInfo = koinInject(),
    notificationLogo: NotificationLogo = koinInject(),
) {
    val windowSize = calculateWindowSizeClass(activity = activity)

    CompositionLocalProvider(
        LocalWindowSizeClass provides windowSize,
    ) {
        HomeNav(
            startDestination = startDestination,
            customPreferences = customPreferences,
            navController = navController,
            bottomBarAdditions = bottomBarAdditions,
            windowSize = windowSize,
        ) {
            NavigationGraph(
                navigationActions = LocalNavActions.current,
                genericInfo = genericInfo,
                windowSize = windowSize,
                customPreferences = customPreferences,
                notificationLogo = notificationLogo,
                startDestination = startDestination,
                navController = navController
            )
        }
    }
}