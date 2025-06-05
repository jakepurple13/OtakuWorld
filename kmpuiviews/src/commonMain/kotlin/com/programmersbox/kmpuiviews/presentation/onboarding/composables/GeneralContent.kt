package com.programmersbox.kmpuiviews.presentation.onboarding.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.MediaCheckerNetworkType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.ListSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.settings.SliderSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.settings.general.BlurSetting
import com.programmersbox.kmpuiviews.presentation.settings.general.ShareChapterSettings
import com.programmersbox.kmpuiviews.presentation.settings.general.ShowDownloadSettings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.are_you_sure_stop_checking
import otakuworld.kmpuiviews.generated.resources.check_for_periodic_updates
import otakuworld.kmpuiviews.generated.resources.no
import otakuworld.kmpuiviews.generated.resources.ok
import otakuworld.kmpuiviews.generated.resources.yes

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun GeneralContent(
    navigationBarSettings: @Composable () -> Unit,
) {
    val handling = koinInject<NewSettingsHandling>()
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("General Settings") },
        )

        CategoryGroup {
            item {
                BlurSetting(handling = handling)
            }
        }

        CategoryGroup {
            item {
                ShowDownloadSettings(handling = handling)
            }
        }

        CategoryGroup {
            item {
                ShareChapterSettings(handling = handling)
            }
        }

        //NavigationBarSettings(handling = handling)
        CategoryGroup {
            item {
                navigationBarSettings()
            }
        }

        var notifyOnBoot by handling.notifyOnReboot.rememberPreference()
        var mediaCheckerSettings by handling
            .mediaCheckerSettings
            .rememberPreference()

        CategoryGroup {
            item {
                SwitchSetting(
                    settingTitle = { Text("Notify on Boot") },
                    value = notifyOnBoot,
                    updateValue = { notifyOnBoot = it },
                    settingIcon = { Icon(Icons.Default.Notifications, null, modifier = Modifier.fillMaxSize()) }
                )
            }

            item {
                var showDialog by remember { mutableStateOf(false) }

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text(stringResource(Res.string.are_you_sure_stop_checking)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    mediaCheckerSettings = mediaCheckerSettings?.copy(shouldRun = false)
                                    showDialog = false
                                }
                            ) { Text(stringResource(Res.string.yes)) }
                        },
                        dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(Res.string.no)) } }
                    )
                }

                SwitchSetting(
                    settingTitle = { Text(stringResource(Res.string.check_for_periodic_updates)) },
                    value = mediaCheckerSettings?.shouldRun == true,
                    settingIcon = { Icon(Icons.Default.Timelapse, null, modifier = Modifier.fillMaxSize()) },
                    updateValue = {
                        if (!it) {
                            showDialog = true
                        } else {
                            mediaCheckerSettings = mediaCheckerSettings?.copy(shouldRun = it)
                        }
                    }
                )
            }

            item {
                ShowWhen(mediaCheckerSettings?.shouldRun == true) {
                    ListSetting(
                        value = mediaCheckerSettings?.networkType ?: MediaCheckerNetworkType.Connected,
                        updateValue = { it, dialog ->
                            mediaCheckerSettings = mediaCheckerSettings?.copy(networkType = it)
                            dialog.value = false
                        },
                        settingTitle = { Text("Network Type") },
                        settingIcon = { Icon(Icons.Default.Wifi, null) },
                        options = MediaCheckerNetworkType.entries.toList(),
                        dialogTitle = { Text("Choose Network Type") },
                        summaryValue = {
                            Column {
                                Text((mediaCheckerSettings?.networkType ?: MediaCheckerNetworkType.Connected).toString())
                                Text("Default is Connected")
                            }
                        },
                        confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.ok)) } },
                        viewText = {
                            when (it) {
                                MediaCheckerNetworkType.Connected -> "Connected - It will run as long as you are connected to the internet"
                                MediaCheckerNetworkType.Metered -> "Metered - It will run only on mobile data"
                                MediaCheckerNetworkType.Unmetered -> "Unmetered - It will run only on wifi"
                            }
                        }
                    )
                }
            }

            item {
                ShowWhen(mediaCheckerSettings?.shouldRun == true) {
                    var sliderValue by remember(mediaCheckerSettings?.interval ?: 1) {
                        mutableFloatStateOf((mediaCheckerSettings?.interval ?: 1).toFloat())
                    }

                    SliderSetting(
                        settingTitle = { Text("Check Every ${mediaCheckerSettings?.interval ?: 1} hours") },
                        settingSummary = { Text("How often do you want to check for updates? Default is 1 hour.") },
                        sliderValue = sliderValue,
                        updateValue = { sliderValue = it },
                        range = 1f..24f,
                        steps = 23,
                        onValueChangedFinished = {
                            mediaCheckerSettings = mediaCheckerSettings?.copy(interval = sliderValue.toLong())
                        },
                        settingIcon = {
                            Icon(
                                Icons.Default.HourglassTop,
                                null,
                            )
                        }
                    )
                }
            }

            item {
                ShowWhen(mediaCheckerSettings?.shouldRun == true) {
                    SwitchSetting(
                        settingTitle = { Text("Only run when charging") },
                        settingIcon = { Icon(Icons.Default.BatteryChargingFull, null) },
                        summaryValue = { Text("Default is false") },
                        value = mediaCheckerSettings?.requiresCharging == true,
                        updateValue = { mediaCheckerSettings = mediaCheckerSettings?.copy(requiresCharging = it) }
                    )
                }
            }

            item {
                ShowWhen(mediaCheckerSettings?.shouldRun == true) {
                    SwitchSetting(
                        settingTitle = { Text("Don't run on low battery") },
                        settingIcon = { Icon(Icons.Default.Battery1Bar, null) },
                        summaryValue = { Text("Default is false") },
                        value = mediaCheckerSettings?.requiresBatteryNotLow == true,
                        updateValue = { mediaCheckerSettings = mediaCheckerSettings?.copy(requiresBatteryNotLow = it) }
                    )
                }
            }

        }
    }
}