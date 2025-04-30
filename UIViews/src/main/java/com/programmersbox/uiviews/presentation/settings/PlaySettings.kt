package com.programmersbox.uiviews.presentation.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.components.SliderSetting
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.PreviewTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaySettings(
    customSettings: @Composable () -> Unit = {},
) {
    SettingsScaffold(stringResource(R.string.playSettings)) {
        val scope = rememberCoroutineScope()
        val settingsHandling: NewSettingsHandling = koinInject()
        val batteryPercent = settingsHandling.batteryPercent
        val slider by batteryPercent.rememberPreference()
        var sliderValue by remember(slider) { mutableFloatStateOf(slider.toFloat()) }

        SliderSetting(
            sliderValue = sliderValue,
            settingTitle = { Text(stringResource(R.string.battery_alert_percentage)) },
            settingSummary = { Text(stringResource(R.string.battery_default)) },
            settingIcon = { Icon(Icons.Default.BatteryAlert, null) },
            range = 1f..100f,
            updateValue = { sliderValue = it },
            onValueChangedFinished = { scope.launch { batteryPercent.set(sliderValue.toInt()) } }
        )

        customSettings()
    }
}

@LightAndDarkPreviews
@Composable
private fun PlaySettingsPreview() {
    PreviewTheme {
        PlaySettings()
    }
}