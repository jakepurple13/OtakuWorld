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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Clock

data class QuickActionsUiState(
    val checkState: ManualCheckState,
    /** Finish time of the last report in epoch millis; null when no check has run. */
    val lastCheckedAt: Long?,
    /** Null when there are no scheduled workers; the UI hides the count. */
    val scheduledCount: Int?,
    /** True from Run until the result is dismissed; scheduled checks alone leave it false. */
    val userRequested: Boolean = false,
    /** Null when there are no custom lists; the UI hides the count. */
    val listCount: Int? = null,
    /** Null when there are no saved notifications; the UI hides the count. */
    val savedNotificationCount: Int? = null,
    /** Heat map reads for yesterday; null when there were none, and the row shows its default text. */
    val yesterdayReads: Int? = null,
)

class QuickActionsViewModel(
    private val controller: QuickActionsController,
    private val workRepository: WorkRepository,
    private val reportStore: UpdateCheckReportStore,
    private val backgroundWorkHandler: BackgroundWorkHandler,
    private val navigationActions: NavigationActions,
    counts: QuickActionCounts,
    /** [com.programmersbox.kmpuiviews.domain.dailyoverview.DailyOverviewPrefs.enabled]. */
    dailyOverviewEnabled: Flow<Boolean>,
) : ViewModel() {
    /** Every surface (card, ⌘K, Actions menu) lists these; the Daily overview row leaves when the feature is off. */
    val visibleActions: StateFlow<List<QuickAction>> = dailyOverviewEnabled
        .map { enabled -> if (enabled) QuickAction.entries else QuickAction.entries - QuickAction.DailyOverview }
        // Eager so the list is settled before the card opens; starts hidden so a disabled row never flashes
        .stateIn(viewModelScope, SharingStarted.Eagerly, QuickAction.entries - QuickAction.DailyOverview)

    // Set by run(); null until a check is requested. sawRunning flips once work is actually active,
    // so stale finished work and reports from earlier runs never show as this run's result.
    // everRunning flips once that work is RUNNING, not just queued.
    private val requestedAt = MutableStateFlow<Long?>(null)
    private val sawRunning = MutableStateFlow(false)
    private val everRunning = MutableStateFlow(false)

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

    init {
        work.onEach { list ->
            if (requestedAt.value != null && list.any { it.state in activeStates }) sawRunning.value = true
            if (requestedAt.value != null && list.any { it.state == "RUNNING" }) everRunning.value = true
        }.launchIn(viewModelScope)
    }

    private val runFlags = combine(sawRunning, requestedAt, everRunning) { saw, requested, ever ->
        Triple(saw, requested, ever)
    }

    private val countFlags = combine(
        counts.lists.onStart { emit(0) },
        counts.savedNotifications.onStart { emit(0) },
        counts.yesterdayReads.onStart { emit(0) },
    ) { lists, saved, yesterday -> Triple(lists, saved, yesterday) }

    val uiState: StateFlow<QuickActionsUiState> = combine(
        work,
        reportStore.report.onStart { emit(null) },
        backgroundWorkHandler.workerInfoFlow().onStart { emit(emptyList()) },
        runFlags,
        countFlags,
    ) { work, report, workers, (saw, requested, ever), (lists, saved, yesterday) ->
        QuickActionsUiState(
            checkState = manualCheckState(work, report, saw, requested, ever),
            lastCheckedAt = report?.finishedAt,
            // Android keeps finished work in the list; only count work that is still scheduled.
            scheduledCount = workers.count { it.status in activeStates }.takeIf { it > 0 },
            userRequested = requested != null,
            listCount = lists.takeIf { it > 0 },
            savedNotificationCount = saved.takeIf { it > 0 },
            yesterdayReads = yesterday.takeIf { it > 0 },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        QuickActionsUiState(ManualCheckState.Idle, null, null),
    )

    fun run(action: QuickAction) {
        controller.closeAll()
        when (action) {
            QuickAction.CheckForUpdates -> {
                if (!isRunning.value) {
                    sawRunning.value = false
                    everRunning.value = false
                    requestedAt.value = Clock.System.now().toEpochMilliseconds()
                    workRepository.checkManually()
                }
            }

            // Already showing it: don't stack a second copy.
            QuickAction.DailyOverview -> if (!navigationActions.isOnTop(Screen.DailyOverview)) {
                navigationActions.navigate(Screen.DailyOverview)
            }
            QuickAction.ScanQrCode -> navigationActions.scanQrCode()
            QuickAction.GlobalSearch -> navigationActions.globalSearch()
            QuickAction.Lists -> navigationActions.customList()
            QuickAction.SavedNotifications -> navigationActions.notifications()
            QuickAction.BackgroundTasks -> navigationActions.workerInfo()
            QuickAction.UpdateErrors -> navigationActions.navigate(Screen.UpdateCheckErrors)
        }
    }

    fun openErrors() {
        controller.closeAll()
        navigationActions.navigate(Screen.UpdateCheckErrors)
    }

    fun dismissResult() {
        sawRunning.value = false
        everRunning.value = false
        requestedAt.value = null
    }
}
