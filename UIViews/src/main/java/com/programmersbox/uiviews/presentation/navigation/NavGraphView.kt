package com.programmersbox.uiviews.presentation.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import com.programmersbox.kmpuiviews.presentation.navigation.Nav3
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.uiviews.GenericInfo

@Composable
fun NavigationGraph(
    genericInfo: GenericInfo,
    windowSize: WindowSizeClass,
    customPreferences: ComposeSettingsDsl,
) {
    Nav3(
        genericInfo = genericInfo,
        windowSize = windowSize,
        customPreferences = customPreferences,
    )
}