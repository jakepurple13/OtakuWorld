package com.programmersbox.kmpuiviews.presentation.settings.workerinfo

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDateTime

class WorkerInfoViewModel(
    backgroundWorkHandler: BackgroundWorkHandler,
) : ViewModel() {
    val workers = mutableStateListOf<WorkerInfoModel>()

    init {
        backgroundWorkHandler
            .workerInfoFlow()
            .onEach {
                workers.clear()
                workers.addAll(it)
            }
            .launchIn(viewModelScope)
    }
}

data class WorkerInfoModel(
    val id: String,
    val progress: Map<String, Any?>,
    val status: String,
    val nextScheduleTimeMillis: LocalDateTime,
    val tags: Set<String>,
    val isPeriodic: Boolean,
)