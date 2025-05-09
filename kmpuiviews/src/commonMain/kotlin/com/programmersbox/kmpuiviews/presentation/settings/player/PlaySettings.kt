package com.programmersbox.kmpuiviews.presentation.settings.player

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
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.components.SliderSetting
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.battery_alert_percentage
import otakuworld.kmpuiviews.generated.resources.battery_default
import otakuworld.kmpuiviews.generated.resources.playSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaySettings(
    customSettings: @Composable () -> Unit = {},
) {
    SettingsScaffold(stringResource(Res.string.playSettings)) {
        val scope = rememberCoroutineScope()
        val settingsHandling: NewSettingsHandling = koinInject()
        val batteryPercent = settingsHandling.batteryPercent
        val slider by batteryPercent.rememberPreference()
        var sliderValue by remember(slider) { mutableFloatStateOf(slider.toFloat()) }

        SliderSetting(
            sliderValue = sliderValue,
            settingTitle = { Text(stringResource(Res.string.battery_alert_percentage)) },
            settingSummary = { Text(stringResource(Res.string.battery_default)) },
            settingIcon = { Icon(Icons.Default.BatteryAlert, null) },
            range = 1f..100f,
            updateValue = { sliderValue = it },
            onValueChangedFinished = { scope.launch { batteryPercent.set(sliderValue.toInt()) } }
        )

        customSettings()
    }
}