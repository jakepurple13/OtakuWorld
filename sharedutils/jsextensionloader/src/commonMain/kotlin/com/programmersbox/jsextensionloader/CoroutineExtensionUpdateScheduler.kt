package com.programmersbox.jsextensionloader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class CoroutineExtensionUpdateScheduler(
    private val scope: CoroutineScope,
    private val checkInterval: Duration = 24.hours,
    private val settings: JsExtensionUpdateSettings,
    private val onCheck: suspend () -> Unit,
) {
    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch {
            while (true) {
                delay(checkInterval)
                if (settings.getMode() != ExtensionUpdateMode.DISABLED) {
                    onCheck()
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
