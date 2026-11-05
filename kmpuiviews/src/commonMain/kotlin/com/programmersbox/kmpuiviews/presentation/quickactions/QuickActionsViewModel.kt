package com.programmersbox.kmpuiviews.presentation.quickactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReportStore
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.repository.WorkInfoKmp
import com.programmersbox.kmpuiviews.repository.WorkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Clock

data class QuickActionsUiState(
    val checkState: ManualCheckState,
    val lastChecked: LastChecked,
    /** Null when there are no scheduled workers; the UI hides the count. */
    val scheduledCount: Int?,
)

class QuickActionsViewModel(
    private val controller: QuickActionsController,
    private val workRepository: WorkRepository,
    private val reportStore: UpdateCheckReportStore,
    private val backgroundWorkHandler: BackgroundWorkHandler,
    private val navigationActions: NavigationActions,
) : ViewModel() {
    private val sawRunning = MutableStateFlow(false)

    // Each source starts with an initial value: on desktop allWorkCheck is emptyFlow(), and
    // combine would never emit otherwise.
    private val work: Flow<List<WorkInfoKmp>> = combine(
        workRepository.manualCheck.onStart { emit(emptyList()) },
        workRepository.allWorkCheck.onStart { emit(emptyList()) },
    ) { manual, all ->
        // Periodic work sits ENQUEUED between runs, so only count it while it actually runs.
        manual + all.filter { it.state == "RUNNING" }
    }

    // Collected eagerly so run() can read it without a uiState subscriber.
    private val isRunning: StateFlow<Boolean> = work
        .map { list -> list.any { it.state in activeStates } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val uiState: StateFlow<QuickActionsUiState> = combine(
        work,
        reportStore.report.onStart { emit(null) },
        backgroundWorkHandler.workerInfoFlow().onStart { emit(emptyList()) },
        sawRunning,
    ) { work, report, workers, saw ->
        QuickActionsUiState(
            checkState = manualCheckState(work, report, saw),
            lastChecked = lastCheckedLabel(report, Clock.System.now().toEpochMilliseconds()),
            scheduledCount = workers.size.takeIf { it > 0 },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        QuickActionsUiState(ManualCheckState.Idle, LastChecked.Never, null),
    )

    fun run(action: QuickAction) {
        controller.close()
        when (action) {
            QuickAction.CheckForUpdates -> {
                if (!isRunning.value) {
                    sawRunning.value = true
                    workRepository.checkManually()
                }
            }

            QuickAction.ScanQrCode -> navigationActions.scanQrCode()
            QuickAction.BackgroundTasks -> navigationActions.workerInfo()
        }
    }

    fun openErrors() {
        controller.close()
        navigationActions.navigate(Screen.UpdateCheckErrors)
    }

    fun dismissResult() {
        sawRunning.value = false
    }

    private companion object {
        val activeStates = setOf("RUNNING", "ENQUEUED", "BLOCKED")
    }
}
