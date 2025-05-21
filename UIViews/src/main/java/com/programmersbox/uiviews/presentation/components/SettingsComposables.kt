package com.programmersbox.uiviews.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.programmersbox.kmpuiviews.presentation.components.settings.CategorySetting
import com.programmersbox.kmpuiviews.presentation.components.settings.CheckBoxSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SliderSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.StandalonePreviewTheme

@LightAndDarkPreviews
@Composable
private fun SwitchSettingPreview() {
    StandalonePreviewTheme {
        Column {
            SwitchSetting(
                value = true,
                updateValue = {},
                settingTitle = { Text("Title") },
                settingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                summaryValue = { Text("Value") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@LightAndDarkPreviews
@Composable
private fun CheckboxSettingPreview() {
    StandalonePreviewTheme {
        Column {
            CheckBoxSetting(
                value = true,
                updateValue = {},
                settingTitle = { Text("Title") },
                settingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                summaryValue = { Text("Value") }
            )
        }
    }
}

@LightAndDarkPreviews
@Composable
private fun SliderSettingPreview() {
    StandalonePreviewTheme {
        Column {
            SliderSetting(
                sliderValue = 5f,
                updateValue = {},
                range = 0f..10f,
                settingTitle = { Text("Slider") },
                settingSummary = { Text("Summary") },
                settingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
            )
        }
    }
}

@LightAndDarkPreviews
@Composable
private fun CategoryPreview() {
    StandalonePreviewTheme {
        Column {
            CategorySetting(
                settingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                settingTitle = { Text("Title") }
            )
            CategorySetting(
                settingTitle = { Text("Title") }
            )
        }
    }
}