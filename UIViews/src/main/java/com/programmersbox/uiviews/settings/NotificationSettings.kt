package com.programmersbox.uiviews.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.uiviews.OtakuApp
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.checkers.UpdateFlowWorker
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalItemDao
import com.programmersbox.uiviews.utils.PreferenceSetting
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.SHOULD_CHECK
import com.programmersbox.uiviews.utils.ShowWhen
import com.programmersbox.uiviews.utils.SwitchSetting
import com.programmersbox.uiviews.utils.updatePref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettings(
    dao: ItemDao = LocalItemDao.current,
    context: Context = LocalContext.current,
    notiViewModel: NotificationViewModel = viewModel { NotificationViewModel(dao, context) }
) {
    SettingsScaffold(stringResource(R.string.notification_settings)) {
        val scope = rememberCoroutineScope()
        ShowWhen(notiViewModel.savedNotifications > 0) {
            var showDialog by remember { mutableStateOf(false) }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text(stringResource(R.string.are_you_sure_delete_notifications)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val number = dao.deleteAllNotifications()
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.deleted_notifications, number),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        context.notificationManager.cancel(42)
                                    }
                                }
                                showDialog = false
                            }
                        ) { Text(stringResource(R.string.yes)) }
                    },
                    dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.no)) } }
                )
            }

            PreferenceSetting(
                settingTitle = { Text(stringResource(R.string.delete_saved_notifications_title)) },
                summaryValue = { Text(stringResource(R.string.delete_notifications_summary)) },
                modifier = Modifier
                    .clickable(
                        indication = ripple(),
                        interactionSource = null
                    ) { showDialog = true }
                    .padding(bottom = 16.dp, top = 8.dp)
            )
        }

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.last_update_check_time)) },
            summaryValue = { Text(notiViewModel.time) },
            modifier = Modifier.clickable(
                indication = ripple(),
                interactionSource = null
            ) {
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        "oneTimeUpdate",
                        ExistingWorkPolicy.KEEP,
                        OneTimeWorkRequestBuilder<UpdateFlowWorker>()
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .setRequiresBatteryNotLow(false)
                                    .setRequiresCharging(false)
                                    .setRequiresDeviceIdle(false)
                                    .setRequiresStorageNotLow(false)
                                    .build()
                            )
                            .build()
                    )
            }
        )

        var showDialog by remember { mutableStateOf(false) }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(stringResource(R.string.are_you_sure_stop_checking)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                context.updatePref(SHOULD_CHECK, false)
                                OtakuApp.updateSetupNow(context, false)
                            }
                            showDialog = false
                        }
                    ) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.no)) } }
            )
        }

        SwitchSetting(
            settingTitle = { Text(stringResource(R.string.check_for_periodic_updates)) },
            value = notiViewModel.canCheck,
            updateValue = {
                if (!it) {
                    showDialog = true
                } else {
                    scope.launch {
                        context.updatePref(SHOULD_CHECK, it)
                        OtakuApp.updateSetupNow(context, it)
                    }
                }
            }
        )

        ShowWhen(notiViewModel.canCheck) {
            PreferenceSetting(
                settingTitle = { Text(stringResource(R.string.clear_update_queue)) },
                summaryValue = { Text(stringResource(R.string.clear_update_queue_summary)) },
                modifier = Modifier
                    .alpha(if (notiViewModel.canCheck) 1f else .38f)
                    .clickable(
                        enabled = notiViewModel.canCheck,
                        indication = ripple(),
                        interactionSource = null
                    ) {
                        val work = WorkManager.getInstance(context)
                        work.cancelUniqueWork("updateFlowChecks")
                        work.pruneWork()
                        OtakuApp.updateSetup(context)
                        Toast
                            .makeText(context, R.string.cleared, Toast.LENGTH_SHORT)
                            .show()
                    }
            )
        }
    }
}

@LightAndDarkPreviews
@Composable
private fun NotificationSettingsPreview() {
    PreviewTheme {
        NotificationSettings()
    }
}