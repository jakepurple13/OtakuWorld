package com.programmersbox.kmpuiviews.presentation.settings.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.settings.SliderSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.repository.WorkInfoKmp
import com.programmersbox.kmpuiviews.utils.LocalItemDao
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.DateTimeFormat
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.are_you_sure_delete_notifications
import otakuworld.kmpuiviews.generated.resources.are_you_sure_stop_checking
import otakuworld.kmpuiviews.generated.resources.check_for_periodic_updates
import otakuworld.kmpuiviews.generated.resources.clear_update_queue
import otakuworld.kmpuiviews.generated.resources.clear_update_queue_summary
import otakuworld.kmpuiviews.generated.resources.cleared
import otakuworld.kmpuiviews.generated.resources.delete_notifications_summary
import otakuworld.kmpuiviews.generated.resources.delete_saved_notifications_title
import otakuworld.kmpuiviews.generated.resources.deleted_notifications
import otakuworld.kmpuiviews.generated.resources.last_update_check_time
import otakuworld.kmpuiviews.generated.resources.no
import otakuworld.kmpuiviews.generated.resources.notification_settings
import otakuworld.kmpuiviews.generated.resources.yes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettings(
    dao: ItemDao = LocalItemDao.current,
    viewModel: NotificationSettingsViewModel = koinViewModel(),
) {
    val snackbarHost = remember { SnackbarHostState() }

    val workInfo by viewModel
        .allWorkCheck
        .collectAsStateWithLifecycle(emptyList())

    SettingsScaffold(
        title = stringResource(Res.string.notification_settings),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier.padding(LocalNavHostPadding.current)
            )
        }
    ) {
        val scope = rememberCoroutineScope()
        ShowWhen(viewModel.savedNotifications > 0) {
            CategoryGroup {
                item {
                    var showDialog by remember { mutableStateOf(false) }

                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { Text(stringResource(Res.string.are_you_sure_delete_notifications)) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            val number = dao.deleteAllNotifications()
                                            snackbarHost.showSnackbar(
                                                getString(Res.string.deleted_notifications, number)
                                            )
                                            viewModel.cancelGroup()
                                        }
                                        showDialog = false
                                    }
                                ) { Text(stringResource(Res.string.yes)) }
                            },
                            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(Res.string.no)) } }
                        )
                    }

                    PreferenceSetting(
                        settingTitle = { Text(stringResource(Res.string.delete_saved_notifications_title)) },
                        summaryValue = { Text(stringResource(Res.string.delete_notifications_summary)) },
                        settingIcon = { Icon(Icons.Default.Delete, null) },
                        modifier = Modifier.clickable(
                            indication = ripple(),
                            interactionSource = null
                        ) { showDialog = true }
                    )
                }
            }
        }

        CategoryGroup {
            item {
                PreferenceSetting(
                    settingTitle = { Text(stringResource(Res.string.last_update_check_time)) },
                    summaryValue = { Text(viewModel.time) },
                    settingIcon = { Icon(Icons.Default.PublishedWithChanges, null) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null,
                        onClick = viewModel::checkManually
                    )
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
                                    viewModel.updateShouldCheck(false)
                                    showDialog = false
                                }
                            ) { Text(stringResource(Res.string.yes)) }
                        },
                        dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(Res.string.no)) } }
                    )
                }

                SwitchSetting(
                    settingTitle = { Text(stringResource(Res.string.check_for_periodic_updates)) },
                    settingIcon = {
                        Crossfade(viewModel.canCheck) { target ->
                            if (target) {
                                Icon(Icons.Default.NotificationsActive, null)
                            } else {
                                Icon(Icons.Default.NotificationsOff, null)
                            }
                        }
                    },
                    value = viewModel.canCheck,
                    updateValue = {
                        if (!it) {
                            showDialog = true
                        } else {
                            viewModel.updateShouldCheck(it)
                        }
                    }
                )

                val manualWorkInfo by viewModel
                    .manualCheck
                    .collectAsStateWithLifecycle(emptyList())

                if (manualWorkInfo.isNotEmpty()) {
                    manualWorkInfo.forEach { workInfo ->
                        item {
                            WorkInfoItem(
                                workInfo = workInfo,
                                title = "Manual Check:",
                                dateFormat = viewModel.dateTimeFormatter
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(viewModel.canCheck) {
            CategoryGroup {
                item {
                    var sliderValue by remember(viewModel.updateHourCheck) {
                        mutableFloatStateOf(viewModel.updateHourCheck.toFloat())
                    }

                    SliderSetting(
                        settingTitle = { Text("Check Every ${viewModel.updateHourCheck} hours") },
                        settingSummary = { Text("How often do you want to check for updates? Default is 1 hour.") },
                        sliderValue = sliderValue,
                        updateValue = { sliderValue = it },
                        range = 1f..24f,
                        steps = 23,
                        settingIcon = { Icon(Icons.Default.Schedule, null) },
                        onValueChangedFinished = { viewModel.updateHourCheck(sliderValue.toLong()) }
                    )
                }

                workInfo.forEach {
                    item {
                        WorkInfoItem(
                            workInfo = it,
                            title = "Scheduled Check:",
                            dateFormat = viewModel.dateTimeFormatter
                        )
                    }
                }

                item {
                    PreferenceSetting(
                        settingTitle = { Text(stringResource(Res.string.clear_update_queue)) },
                        summaryValue = { Text(stringResource(Res.string.clear_update_queue_summary)) },
                        settingIcon = { Icon(Icons.Default.DeleteSweep, null) },
                        modifier = Modifier
                            .alpha(if (viewModel.canCheck) 1f else .38f)
                            .clickable(
                                enabled = viewModel.canCheck,
                                indication = ripple(),
                                interactionSource = null
                            ) {
                                scope.launch {
                                    viewModel.pruneWork()
                                    viewModel.updateShouldCheck(!viewModel.canCheck)
                                    viewModel.updateShouldCheck(!viewModel.canCheck)
                                    snackbarHost.showSnackbar(getString(Res.string.cleared))
                                }
                            }
                    )
                }
            }
        }

        CategoryGroup {
            item {
                SwitchSetting(
                    settingTitle = { Text("Notify on Boot") },
                    settingIcon = { Icon(Icons.Default.PowerSettingsNew, null) },
                    value = viewModel.notifyOnBoot.rememberPreference().value,
                    updateValue = { scope.launch { viewModel.notifyOnBoot.set(it) } }
                )
            }
        }
    }
}

@Composable
private fun WorkInfoItem(
    workInfo: WorkInfoKmp,
    title: String,
    dateFormat: DateTimeFormat<LocalDateTime>,
) {
    PreferenceSetting(
        settingTitle = { Text(title) },
        summaryValue = {
            Column(Modifier.animateContentSize()) {
                Text(dateFormat.format(workInfo.nextScheduleTimeMillis))
                Text(workInfo.state)
                Text(workInfo.source)
                if (workInfo.progress != null && workInfo.max != null) {
                    if (workInfo.progress == 0) {
                        //TODO: Put back when supported
                        //LinearWavyProgressIndicator()
                        LinearProgressIndicator()
                    } else {
                        val animatedProgress by animateFloatAsState(workInfo.progress.toFloat() / workInfo.max.toFloat())
                        LinearProgressIndicator(progress = { animatedProgress })
                    }
                }
            }
        },
        endIcon = {
            if (workInfo.progress != null && workInfo.max != null) {
                Text("${workInfo.progress}/${workInfo.max}")
            }
        }
    )
}
