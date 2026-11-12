package com.programmersbox.jsextensionloader

import app.cash.zipline.InterruptHandler
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Interrupts a QuickJs evaluation that runs longer than [timeout]. Coroutine cancellation cannot stop
 * native evaluate(), so without this a looping script would hold its thread forever.
 * Install it as the QuickJs interruptHandler and run each evaluation through [guard].
 */
class JsEvalDeadline(private val timeout: Duration) : InterruptHandler {
    @Volatile
    private var deadline: TimeMark? = null

    fun <T> guard(block: () -> T): T {
        deadline = TimeSource.Monotonic.markNow() + timeout
        try {
            return block()
        } finally {
            deadline = null
        }
    }

    override fun poll(): Boolean = deadline?.hasPassedNow() == true
}
