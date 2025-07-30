package com.programmersbox.kmpuiviews.presentation.onboarding.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import com.programmersbox.datastore.MiddleNavigationAction
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.rememberFloatingNavigation
import com.programmersbox.kmpuiviews.presentation.components.item
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupDefaults
import com.programmersbox.kmpuiviews.presentation.components.settings.ListSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.settings.SliderSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.components.visibleName
import com.programmersbox.kmpuiviews.presentation.settings.general.BlurSetting
import com.programmersbox.kmpuiviews.presentation.settings.general.ShareChapterSettings
import com.programmersbox.kmpuiviews.presentation.settings.general.ShowDownloadSettings
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.are_you_sure_stop_checking
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.check_for_periodic_updates
import otakuworld.kmpuiviews.generated.resources.no
import otakuworld.kmpuiviews.generated.resources.ok
import otakuworld.kmpuiviews.generated.resources.show_all_screen
import otakuworld.kmpuiviews.generated.resources.yes

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun GeneralContent() {
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

        ListItem(
            headlineContent = { Text("Detail Settings") },
        )

        CategoryGroup {
            item {
                ShowDownloadSettings(handling = handling)
            }

            item {
                ShareChapterSettings(handling = handling)
            }
        }

        ListItem(
            headlineContent = { Text("Navigation Settings") },
        )

        CategoryGroup {
            item {
                NavigationBarSettings(handling = handling)
            }
        }

        ListItem(
            headlineContent = { Text("Notification Settings") },
        )

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
                                    mediaCheckerSettings = mediaCheckerSettings.copy(shouldRun = false)
                                    showDialog = false
                                }
                            ) { Text(stringResource(Res.string.yes)) }
                        },
                        dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(Res.string.no)) } }
                    )
                }

                SwitchSetting(
                    settingTitle = { Text(stringResource(Res.string.check_for_periodic_updates)) },
                    value = mediaCheckerSettings.shouldRun,
                    settingIcon = { Icon(Icons.Default.Timelapse, null, modifier = Modifier.fillMaxSize()) },
                    updateValue = {
                        if (!it) {
                            showDialog = true
                        } else {
                            mediaCheckerSettings = mediaCheckerSettings.copy(shouldRun = it)
                        }
                    }
                )
            }

            item {
                ShowWhen(mediaCheckerSettings.shouldRun) {
                    ListSetting(
                        value = mediaCheckerSettings.networkType,
                        updateValue = { it, dialog ->
                            mediaCheckerSettings = mediaCheckerSettings.copy(networkType = it)
                            dialog.value = false
                        },
                        settingTitle = { Text("Network Type") },
                        settingIcon = { Icon(Icons.Default.Wifi, null) },
                        options = MediaCheckerNetworkType.entries.toList(),
                        dialogTitle = { Text("Choose Network Type") },
                        summaryValue = {
                            Column {
                                Text(mediaCheckerSettings.networkType.toString())
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
                ShowWhen(mediaCheckerSettings.shouldRun) {
                    var sliderValue by remember(mediaCheckerSettings.interval) {
                        mutableFloatStateOf(mediaCheckerSettings.interval.toFloat())
                    }

                    SliderSetting(
                        settingTitle = { Text("Check Every ${mediaCheckerSettings.interval} hours") },
                        settingSummary = { Text("How often do you want to check for updates? Default is 1 hour.") },
                        sliderValue = sliderValue,
                        updateValue = { sliderValue = it },
                        range = 1f..24f,
                        steps = 23,
                        onValueChangedFinished = {
                            mediaCheckerSettings = mediaCheckerSettings.copy(interval = sliderValue.toLong())
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
                ShowWhen(mediaCheckerSettings.shouldRun) {
                    SwitchSetting(
                        settingTitle = { Text("Only run when charging") },
                        settingIcon = { Icon(Icons.Default.BatteryChargingFull, null) },
                        summaryValue = { Text("Default is false") },
                        value = mediaCheckerSettings.requiresCharging,
                        updateValue = { mediaCheckerSettings = mediaCheckerSettings.copy(requiresCharging = it) }
                    )
                }
            }

            item {
                ShowWhen(mediaCheckerSettings.shouldRun) {
                    SwitchSetting(
                        settingTitle = { Text("Don't run on low battery") },
                        settingIcon = { Icon(Icons.Default.Battery1Bar, null) },
                        summaryValue = { Text("Default is false") },
                        value = mediaCheckerSettings.requiresBatteryNotLow,
                        updateValue = { mediaCheckerSettings = mediaCheckerSettings.copy(requiresBatteryNotLow = it) }
                    )
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun NavigationBarSettings(handling: NewSettingsHandling) {
    var floatingNavigation by rememberFloatingNavigation()

    SwitchSetting(
        settingTitle = { Text("Floating Navigation") },
        settingIcon = { Icon(Icons.Default.Navigation, null, modifier = Modifier.fillMaxSize()) },
        value = floatingNavigation,
        updateValue = { floatingNavigation = it }
    )

    CategoryGroupDefaults.Divider()

    if (LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Expanded) {
        var showAllScreen by handling.rememberShowAll()

        SwitchSetting(
            settingTitle = { Text(stringResource(Res.string.show_all_screen)) },
            settingIcon = { Icon(Icons.Default.Menu, null, modifier = Modifier.fillMaxSize()) },
            value = showAllScreen,
            updateValue = { showAllScreen = it }
        )
    } else {

        var middleNavigationAction by handling.rememberMiddleNavigationAction()
        ListSetting(
            settingTitle = { Text("Middle Navigation Destination") },
            dialogIcon = { Icon(Icons.Default.LocationOn, null) },
            settingIcon = { Icon(Icons.Default.LocationOn, null, modifier = Modifier.fillMaxSize()) },
            dialogTitle = { Text("Choose a middle navigation destination") },
            summaryValue = { Text(middleNavigationAction.visibleName) },
            confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(Res.string.cancel)) } },
            value = middleNavigationAction,
            options = MiddleNavigationAction.entries,
            updateValue = { it, d ->
                d.value = false
                middleNavigationAction = it
            }
        )

        MultipleActionsSetting(
            handling = handling,
            middleNavigationAction = middleNavigationAction
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MultipleActionsSetting(
    handling: NewSettingsHandling,
    middleNavigationAction: MiddleNavigationAction,
) {
    var multipleActions by handling.rememberMiddleMultipleActions()

    val multipleActionOptions = MiddleNavigationAction
        .entries
        .filter { it != MiddleNavigationAction.Multiple }

    ShowWhen(middleNavigationAction == MiddleNavigationAction.Multiple) {
        CategoryGroupDefaults.Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                leadingContent = {
                    var showMenu by remember { mutableStateOf(false) }
                    DropdownMenu(
                        showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        multipleActionOptions.forEach {
                            DropdownMenuItem(
                                text = { Text(it.name) },
                                leadingIcon = {
                                    Icon(
                                        it.item?.icon?.invoke(true) ?: Icons.Default.Add,
                                        null,
                                    )
                                },
                                onClick = {
                                    multipleActions = multipleActions?.copy(
                                        startAction = it,
                                    )
                                    showMenu = false
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = { showMenu = true }
                    ) {
                        Icon(
                            multipleActions
                                ?.startAction
                                ?.item
                                ?.icon
                                ?.invoke(true)
                                ?: Icons.Default.Add,
                            null
                        )
                    }
                },
                trailingContent = {
                    var showMenu by remember { mutableStateOf(false) }
                    DropdownMenu(
                        showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        multipleActionOptions.forEach {
                            DropdownMenuItem(
                                text = { Text(it.visibleName) },
                                leadingIcon = {
                                    Icon(
                                        it.item?.icon?.invoke(true) ?: Icons.Default.Add,
                                        null,
                                    )
                                },
                                onClick = {
                                    multipleActions = multipleActions?.copy(endAction = it)
                                    showMenu = false
                                }
                            )
                        }
                    }
                    IconButton(
                        onClick = { showMenu = true }
                    ) {
                        Icon(
                            multipleActions
                                ?.endAction
                                ?.item
                                ?.icon
                                ?.invoke(true)
                                ?: Icons.Default.Add,
                            null
                        )
                    }
                },
            ) {
                FilledIconButton(
                    modifier = Modifier.width(64.dp),
                    onClick = {}
                ) {
                    Icon(
                        Icons.Filled.UnfoldLess,
                        contentDescription = "Localized description"
                    )
                }
            }
        }
    }
}