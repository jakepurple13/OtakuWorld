package com.programmersbox.kmpuiviews.presentation.settings.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.MediaCheckerNetworkType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpuiviews.DateTimeFormatHandler
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.kmpuiviews.repository.WorkRepository
import com.programmersbox.kmpuiviews.utils.DateTimeFormatItem
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class NotificationSettingsViewModel(
    dao: ItemDao,
    dataStoreHandling: DataStoreHandling,
    settingsHandling: NewSettingsHandling,
    dateTimeFormatHandler: DateTimeFormatHandler,
    workRepository: WorkRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel(), WorkRepository by workRepository {

    var savedNotifications by mutableIntStateOf(0)
        private set

    var canCheck by mutableStateOf(false)
    var updateHourCheck by mutableLongStateOf(0L)

    var time by mutableStateOf("")

    val notifyOnBoot = settingsHandling.notifyOnReboot

    val mediaCheckerSettings = settingsHandling.mediaCheckerSettings

    val dateTimeFormatter by lazy {
        DateTimeFormatItem(dateTimeFormatHandler.is24HourTime())
    }

    init {
        dao.getAllNotificationCount()
            .dispatchIo()
            .onEach { savedNotifications = it }
            .launchIn(viewModelScope)

        mediaCheckerSettings
            .asFlow()
            .onEach {
                canCheck = it.shouldRun
                updateHourCheck = it.interval
            }
            .launchIn(viewModelScope)

        combine(
            dataStoreHandling
                .updateCheckingStart
                .asFlow()
                .map { "Start: ${dateTimeFormatter.format(it.toLocalDateTime())}" },
            dataStoreHandling
                .updateCheckingEnd
                .asFlow()
                .map { "End: ${dateTimeFormatter.format(it.toLocalDateTime())}" }
        ) { s, e -> s to e }
            .map { "${it.first}\n${it.second}" }
            .onEach { time = it }
            .launchIn(viewModelScope)
    }

    fun updateShouldCheck(value: Boolean) {
        viewModelScope.launch {
            mediaCheckerSettings
                .get()
                .copy(shouldRun = value)
                .let { mediaCheckerSettings.set(it) }
        }
    }

    fun updateHourCheck(value: Long) {
        viewModelScope.launch {
            mediaCheckerSettings
                .get()
                .copy(interval = value)
                .let { mediaCheckerSettings.set(it) }
        }
    }

    fun updateNetworkType(value: MediaCheckerNetworkType) {
        viewModelScope.launch {
            mediaCheckerSettings
                .get()
                .copy(networkType = value)
                .let { mediaCheckerSettings.set(it) }
        }
    }

    fun updateRequiresCharging(value: Boolean) {
        viewModelScope.launch {
            mediaCheckerSettings
                .get()
                .copy(requiresCharging = value)
                .let { mediaCheckerSettings.set(it) }
        }
    }

    fun updateRequiresBatteryNotLow(value: Boolean) {
        viewModelScope.launch {
            mediaCheckerSettings
                .get()
                .copy(requiresBatteryNotLow = value)
                .let { mediaCheckerSettings.set(it) }
        }
    }

    fun cancelGroup() = notificationRepository.cancelGroup()
}