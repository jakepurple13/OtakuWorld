package com.programmersbox.kmpuiviews.motion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Quit state machine: plays the quit motion, then calls [exit] exactly once. */
class QuitController(
    private val plan: () -> QuitPlan,
    private val launch: () -> MotionRun?,
    private val exit: () -> Unit,
) {
    sealed interface State {
        data object Idle : State
        data class Playing(val plan: QuitPlan, val run: MotionRun) : State
        data object Exited : State
    }

    var state: State by mutableStateOf(State.Idle)
        private set

    var hideMainWindow: Boolean by mutableStateOf(false)
        private set

    fun requestQuit() {
        when (state) {
            State.Exited -> return
            is State.Playing -> exitNow()
            State.Idle -> {
                launch()?.skip()
                val quitPlan = plan()
                if (quitPlan.motion.durationMillis <= 0) {
                    exitNow()
                } else {
                    state = State.Playing(quitPlan, MotionRun(quitPlan.motion))
                }
            }
        }
    }

    fun overlayFirstFrame() {
        hideMainWindow = true
    }

    fun finished() = exitNow()

    private fun exitNow() {
        if (state == State.Exited) return
        state = State.Exited
        exit()
    }
}
