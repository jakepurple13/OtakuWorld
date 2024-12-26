package com.programmersbox.uiviews.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.hasKeyWithValueOfType
import androidx.work.workDataOf
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
import org.koin.androidx.compose.koinViewModel
import java.text.DateFormat
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationSettings(
    dao: ItemDao = LocalItemDao.current,
    context: Context = LocalContext.current,
    notiViewModel: NotificationViewModel = koinViewModel(),
) {
    val work = remember { WorkManager.getInstance(context) }
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
                work.enqueueUniqueWork(
                    "oneTimeUpdate",
                    ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<UpdateFlowWorker>()
                        .setInputData(workDataOf(UpdateFlowWorker.CHECK_ALL to true))
                        .addTag("ManualCheck")
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
                        work.cancelUniqueWork("updateFlowChecks")
                        work.pruneWork()
                        OtakuApp.updateSetup(context)
                        Toast
                            .makeText(context, R.string.cleared, Toast.LENGTH_SHORT)
                            .show()
                    }
            )

            Spacer(Modifier.padding(16.dp))

            val dateFormat = remember { SimpleDateFormat.getDateTimeInstance() }

            val workInfo by work
                .getWorkInfosForUniqueWorkFlow("updateFlowChecks")
                .collectAsStateWithLifecycle(emptyList())

            workInfo.forEach { WorkInfoItem(it, dateFormat) }
        }

        val manualWorkInfo by work
            .getWorkInfosByTagFlow("ManualCheck")
            .collectAsStateWithLifecycle(emptyList())

        manualWorkInfo.forEach { workInfo ->
            val (progress, max) = if (workInfo.progress.hasKeyWithValueOfType<Int>("progress") && workInfo.progress.hasKeyWithValueOfType<Int>("max")) {
                workInfo.progress.getInt("progress", 0) to workInfo.progress.getInt("max", 0)
            } else null to null
            PreferenceSetting(
                settingTitle = { Text("Manual Check:") },
                summaryValue = {
                    Column(Modifier.animateContentSize()) {
                        Text(workInfo.state.toString())
                        Text(workInfo.progress.getString("source").orEmpty())
                        if (progress != null && max != null) {
                            if (progress == 0) {
                                LinearWavyProgressIndicator()
                            } else {
                                val animatedProgress by animateFloatAsState(progress.toFloat() / max.toFloat())
                                LinearWavyProgressIndicator(progress = { animatedProgress })
                            }
                        }
                    }
                },
                endIcon = {
                    if (progress != null && max != null) {
                        Text("$progress/$max")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WorkInfoItem(workInfo: WorkInfo, dateFormat: DateFormat) {
    val (progress, max) = if (workInfo.progress.hasKeyWithValueOfType<Int>("progress") && workInfo.progress.hasKeyWithValueOfType<Int>("max")) {
        workInfo.progress.getInt("progress", 0) to workInfo.progress.getInt("max", 0)
    } else null to null
    PreferenceSetting(
        settingTitle = { Text("Scheduled Check:") },
        summaryValue = {
            Column(Modifier.animateContentSize()) {
                Text(dateFormat.format(workInfo.nextScheduleTimeMillis))
                Text(workInfo.state.toString())
                Text(workInfo.progress.getString("source").orEmpty())
                if (progress != null && max != null) {
                    if (progress == 0) {
                        LinearWavyProgressIndicator()
                    } else {
                        val animatedProgress by animateFloatAsState(progress.toFloat() / max.toFloat())
                        LinearWavyProgressIndicator(progress = { animatedProgress })
                    }
                }
            }
        },
        endIcon = {
            if (progress != null && max != null) {
                Text("$progress/$max")
            }
        }
    )
}

@LightAndDarkPreviews
@Composable
private fun NotificationSettingsPreview() {
    PreviewTheme {
        NotificationSettings()
    }
}