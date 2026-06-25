package com.programmersbox.uiviews.presentation.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import com.programmersbox.kmpuiviews.presentation.settings.SettingScreen
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.PreviewThemeColorsSizes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@PreviewThemeColorsSizes
@Composable
private fun SettingsPreview() {
    PreviewTheme {
        SettingScreen(
            composeSettingsDsl = ComposeSettingsDsl(),
            accountSettings = {},
        )
    }
}
