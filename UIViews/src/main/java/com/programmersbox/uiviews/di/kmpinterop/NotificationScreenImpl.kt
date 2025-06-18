package com.programmersbox.uiviews.di.kmpinterop

import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.datepicker.DateValidatorPointForward
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.gsonutils.toJson
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.notifications.NotificationScreenInterface
import com.programmersbox.kmpuiviews.utils.LocalSystemDateTimeFormat
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.checkers.NotifySingleWorker
import com.programmersbox.uiviews.checkers.SavedNotifications
import com.programmersbox.uiviews.checkers.UpdateNotification
import com.programmersbox.uiviews.utils.NotificationLogo
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationScreenImpl(
    private val updateNotification: UpdateNotification,
    private val genericInfo: GenericInfo,
    private val sourceRepository: SourceRepository,
    private val itemDao: ItemDao,
    private val context: Context,
    private val notificationLogo: NotificationLogo,
) : NotificationScreenInterface {
    override suspend fun notifyItem(notificationItem: NotificationItem) {
        SavedNotifications.viewNotificationFromDb(
            context = context,
            n = notificationItem,
            notificationLogo = notificationLogo,
            info = genericInfo,
            sourceRepository = sourceRepository,
            itemDao = itemDao,
            update = updateNotification
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun NotifyAt(
        item: NotificationItem,
        content: @Composable ((() -> Unit) -> Unit),
    ) {
        val context = LocalContext.current
        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }

        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
            selectableDates = remember {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return DateValidatorPointForward.now().isValid(utcTimeMillis)
                    }
                }
            }
        )
        val calendar = remember { Calendar.getInstance() }
        val is24HourFormat by rememberUpdatedState(DateFormat.is24HourFormat(context))
        val timeState = rememberTimePickerState(
            initialHour = calendar[Calendar.HOUR_OF_DAY],
            initialMinute = calendar[Calendar.MINUTE],
            is24Hour = is24HourFormat
        )

        if (showTimePicker) {
            TimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text(stringResource(id = R.string.selectTime)) },
                dismissButton = {
                    TextButton(
                        onClick = { showTimePicker = false }
                    ) { Text(stringResource(R.string.cancel)) }
                },
                confirmButton = {
                    val dateTimeFormatter = LocalSystemDateTimeFormat.current
                    TextButton(
                        onClick = {
                            showTimePicker = false
                            val c = Calendar.getInstance()
                            c.timeInMillis = dateState.selectedDateMillis ?: 0L
                            c[Calendar.DAY_OF_YEAR] += 1
                            c[Calendar.HOUR_OF_DAY] = timeState.hour
                            c[Calendar.MINUTE] = timeState.minute
                            c[Calendar.SECOND] = 0
                            c[Calendar.MILLISECOND] = 0

                            WorkManager.getInstance(context)
                                .enqueueUniqueWork(
                                    item.notiTitle,
                                    ExistingWorkPolicy.REPLACE,
                                    OneTimeWorkRequestBuilder<NotifySingleWorker>()
                                        .setInputData(
                                            workDataOf(
                                                "notiData" to item.toJson()
                                            )
                                        )
                                        .setInitialDelay(
                                            duration = c.timeInMillis - System.currentTimeMillis(),
                                            timeUnit = TimeUnit.MILLISECONDS
                                        )
                                        .build()
                                )

                            Toast.makeText(
                                context,
                                context.getString(
                                    R.string.willNotifyAt,
                                    dateTimeFormatter.format(c.timeInMillis.toLocalDateTime())
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) { Text(stringResource(R.string.ok)) }
                }
            ) { TimePicker(state = timeState) }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                dismissButton = {
                    TextButton(
                        onClick = { showDatePicker = false }
                    ) { Text(stringResource(R.string.cancel)) }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDatePicker = false
                            showTimePicker = true
                        }
                    ) { Text(stringResource(R.string.ok)) }
                }
            ) {
                DatePicker(
                    state = dateState,
                    title = { Text(stringResource(R.string.selectDate)) }
                )
            }
        }

        content(
            { showDatePicker = true }
        )
    }
}