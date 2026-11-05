package com.programmersbox.kmpuiviews.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Tracks the desktop manual update check so it reports progress like Android's WorkManager
 * and cannot be started twice. State strings match Android's `WorkInfo.State.toString()`.
 */
class DesktopManualCheckTracker {
    private val _state = MutableStateFlow<List<WorkInfoKmp>>(emptyList())
    val state: StateFlow<List<WorkInfoKmp>> = _state.asStateFlow()

    fun tryStart(): Boolean {
        var started = false
        _state.update { current ->
            if (current.any { it.state == RUNNING }) {
                started = false
                current
            } else {
                started = true
                listOf(info(RUNNING, source = "", progress = null, max = null))
            }
        }
        return started
    }

    fun progress(max: Int, progress: Int, source: String) {
        _state.update { current ->
            if (current.any { it.state == RUNNING }) {
                listOf(info(RUNNING, source, progress, max))
            } else {
                current
            }
        }
    }

    fun finish(success: Boolean) {
        _state.update { current ->
            val last = current.firstOrNull()
            listOf(info(if (success) SUCCEEDED else FAILED, last?.source.orEmpty(), last?.progress, last?.max))
        }
    }

    private fun info(state: String, source: String, progress: Int?, max: Int?) = WorkInfoKmp(
        state = state,
        source = source,
        progress = progress,
        max = max,
        nextScheduleTimeMillis = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
    )

    private companion object {
        const val RUNNING = "RUNNING"
        const val SUCCEEDED = "SUCCEEDED"
        const val FAILED = "FAILED"
    }
}
