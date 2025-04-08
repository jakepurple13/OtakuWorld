package com.programmersbox.uiviews.presentation.settings.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.uiviews.datastore.DataStoreHandling
import com.programmersbox.uiviews.datastore.SettingsHandling
import com.programmersbox.uiviews.utils.dispatchIo
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

class NotificationViewModel(
    dao: ItemDao,
    private val dataStoreHandling: DataStoreHandling,
    settingsHandling: SettingsHandling,
) : ViewModel() {

    var savedNotifications by mutableIntStateOf(0)
        private set

    var canCheck by mutableStateOf(false)
    var updateHourCheck by mutableLongStateOf(0L)

    var time by mutableStateOf("")

    val notifyOnBoot = settingsHandling.notifyOnReboot

    private val dateTimeFormatter by lazy { SimpleDateFormat.getDateTimeInstance() }

    init {
        dao.getAllNotificationCount()
            .dispatchIo()
            .onEach { savedNotifications = it }
            .launchIn(viewModelScope)

        dataStoreHandling
            .shouldCheck
            .asFlow()
            .onEach { canCheck = it }
            .launchIn(viewModelScope)

        dataStoreHandling
            .updateHourCheck
            .asFlow()
            .onEach { updateHourCheck = it }
            .launchIn(viewModelScope)

        combine(
            dataStoreHandling
                .updateCheckingStart
                .asFlow()
                .map { "Start: ${dateTimeFormatter.format(it)}" },
            dataStoreHandling
                .updateCheckingEnd
                .asFlow()
                .map { "End: ${dateTimeFormatter.format(it)}" }
        ) { s, e -> s to e }
            .map { "${it.first}\n${it.second}" }
            .onEach { time = it }
            .launchIn(viewModelScope)
    }

    fun updateShouldCheck(value: Boolean) {
        viewModelScope.launch { dataStoreHandling.shouldCheck.set(value) }
    }

    fun updateHourCheck(value: Long) {
        viewModelScope.launch { dataStoreHandling.updateHourCheck.set(value) }
    }
}