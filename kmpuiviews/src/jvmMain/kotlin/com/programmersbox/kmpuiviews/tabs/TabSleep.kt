package com.programmersbox.kmpuiviews.tabs

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/** How often [runSleepChecks] looks for idle tabs. */
const val SLEEP_CHECK_INTERVAL_MS = 60_000L

/** "Put unused tabs to sleep" as a delay in ms, or null for Never. */
val SleepAfter.millis: Long?
    get() = when (this) {
        SleepAfter.Never -> null
        SleepAfter.M15 -> 15 * 60_000L
        SleepAfter.H1 -> 60 * 60_000L
        SleepAfter.H4 -> 4 * 60 * 60_000L
    }

/**
 * Puts to sleep every tab left at least [after] ms before [now]; null [after] (Never) does nothing. A tab never sleeps
 * while it is the active tab of its window, while it is playing or in the mini player, or when it is pinned (Home).
 *
 * A Notes panel with an unsaved draft needs no check: the panel is composed only in the active tab, and leaving the
 * tab flushes the draft onto [PendingWrites], which no ViewModel clear can drop.
 */
fun TabsController.sleepIdleTabs(now: Long, after: Long?) {
    if (after == null) return
    for (window in windows.toList()) {
        for (tab in window.tabs.toList()) {
            if (tab.sleeping.value || tab.pinned || tab.id == window.activeId.value || tab.playing.value) continue
            if (sleepGuards.any { it(tab.id) }) continue
            val left = lastShownAt(tab.id) ?: continue
            if (now - left >= after) sleep(tab, window)
        }
    }
}

/**
 * Runs [sleepIdleTabs] every [SLEEP_CHECK_INTERVAL_MS], and at once whenever [after] changes. Call it on the UI thread;
 * it runs until cancelled.
 */
suspend fun TabsController.runSleepChecks(after: Flow<SleepAfter>, clock: () -> Long) {
    after.collectLatest { pref ->
        val ms = pref.millis ?: return@collectLatest
        while (true) {
            sleepIdleTabs(clock(), ms)
            delay(SLEEP_CHECK_INTERVAL_MS)
        }
    }
}
