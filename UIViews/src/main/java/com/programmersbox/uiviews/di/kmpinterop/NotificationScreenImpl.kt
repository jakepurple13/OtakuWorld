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

    override fun scheduleNotification(item: NotificationItem, time: Long) {
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
                        duration = time,
                        timeUnit = TimeUnit.MILLISECONDS
                    )
                    .build()
            )
    }
}