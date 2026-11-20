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

/**
 * The OS's deferred answer to a quit it asked for (macOS: logout, restart, shutdown, Dock or ⌘Q). Refusing it would
 * make macOS abort a logout ("<App> interrupted logout"), so the quit handler [hold]s the answer and [releaseAfter]
 * answers it once the app loop has ended. Answering exits the JVM at once (macOS: `System.exit(0)`), so it must never
 * run while windows are still being disposed. Quits the OS did not ask for (close button) hold nothing.
 */
class PendingQuitReply {
    private var performQuit: (() -> Unit)? = null

    /**
     * Keeps [performQuit] until [release]; a newer request replaces an older unanswered one. AppKit has only one
     * pending terminate reply, so answering the newer one answers both; answering the older one here would exit now.
     */
    @Synchronized
    fun hold(performQuit: () -> Unit) {
        this.performQuit = performQuit
    }

    /** Calls the held answer once, if any. */
    fun release() {
        val reply = synchronized(this) { performQuit.also { performQuit = null } }
        reply?.invoke()
    }

    /**
     * Runs [appLoop], which must block until the app has exited and its composition is disposed (all `onDispose`
     * teardown done), then answers any held quit, including one that arrived while the app was already exiting.
     */
    inline fun releaseAfter(appLoop: () -> Unit) {
        appLoop()
        release()
    }
}
