package com.programmersbox.uiviews.presentation.navigation

import android.app.Activity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.programmersbox.kmpuiviews.presentation.HomeNav
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import com.programmersbox.uiviews.GenericInfo
import org.koin.compose.koinInject

@OptIn(
    ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class,
)
@Composable
fun HomeNav(
    activity: Activity,
    customPreferences: ComposeSettingsDsl,
    bottomBarAdditions: @Composable () -> Unit,
    genericInfo: GenericInfo = koinInject(),
) {
    val windowSize = calculateWindowSizeClass(activity = activity)

    CompositionLocalProvider(
        LocalWindowSizeClass provides windowSize,
    ) {
        HomeNav(
            bottomBarAdditions = bottomBarAdditions,
            windowSize = windowSize,
            customPreferences = customPreferences,
            genericInfo = genericInfo
        )
    }
}