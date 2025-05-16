package com.programmersbox.kmpuiviews.presentation.onboarding.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.presentation.components.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.SliderSetting
import com.programmersbox.kmpuiviews.presentation.components.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.settings.general.BlurSetting
import com.programmersbox.kmpuiviews.presentation.settings.general.ShareChapterSettings
import com.programmersbox.kmpuiviews.presentation.settings.general.ShowDownloadSettings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.are_you_sure_stop_checking
import otakuworld.kmpuiviews.generated.resources.check_for_periodic_updates
import otakuworld.kmpuiviews.generated.resources.no
import otakuworld.kmpuiviews.generated.resources.yes

@Composable
internal fun GeneralContent(
    navigationBarSettings: @Composable () -> Unit,
) {
    val handling = koinInject<NewSettingsHandling>()
    val dataStoreHandling = koinInject<DataStoreHandling>()
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("General Settings") },
        )

        HorizontalDivider()

        BlurSetting(handling = handling)

        HorizontalDivider()

        ShowDownloadSettings(handling = handling)

        ShareChapterSettings(handling = handling)

        HorizontalDivider()

        //NavigationBarSettings(handling = handling)
        navigationBarSettings()

        HorizontalDivider()

        var notifyOnBoot by handling.notifyOnReboot.rememberPreference()

        SwitchSetting(
            settingTitle = { Text("Notify on Boot") },
            value = notifyOnBoot,
            updateValue = { notifyOnBoot = it },
            settingIcon = { Icon(Icons.Default.Notifications, null, modifier = Modifier.fillMaxSize()) }
        )

        var canCheck by dataStoreHandling
            .shouldCheck
            .asState()

        var showDialog by remember { mutableStateOf(false) }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(stringResource(Res.string.are_you_sure_stop_checking)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            canCheck = false
                            showDialog = false
                        }
                    ) { Text(stringResource(Res.string.yes)) }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(Res.string.no)) } }
            )
        }

        SwitchSetting(
            settingTitle = { Text(stringResource(Res.string.check_for_periodic_updates)) },
            value = canCheck,
            settingIcon = { Icon(Icons.Default.Timelapse, null, modifier = Modifier.fillMaxSize()) },
            updateValue = {
                if (!it) {
                    showDialog = true
                } else {
                    canCheck = it
                }
            }
        )

        ShowWhen(canCheck) {
            var updateHourCheck by dataStoreHandling
                .updateHourCheck
                .asState()

            var sliderValue by remember(updateHourCheck) {
                mutableFloatStateOf(updateHourCheck.toFloat())
            }

            SliderSetting(
                settingTitle = { Text("Check Every $updateHourCheck hours") },
                settingSummary = { Text("How often do you want to check for updates? Default is 1 hour.") },
                sliderValue = sliderValue,
                updateValue = { sliderValue = it },
                range = 1f..24f,
                steps = 23,
                onValueChangedFinished = { updateHourCheck = sliderValue.toLong() },
                settingIcon = {
                    Icon(
                        Icons.Default.HourglassTop,
                        null,
                    )
                }
            )
        }
    }
}