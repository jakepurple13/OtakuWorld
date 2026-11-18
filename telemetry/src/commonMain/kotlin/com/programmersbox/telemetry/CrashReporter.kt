package com.programmersbox.telemetry

/**
 * Crash reporting: breadcrumbs, non-fatal exceptions and custom keys. Implementations must never throw.
 *
 * Breadcrumbs follow the same rules as [Analytics]: no titles, URLs or user-entered text.
 * Log an exception's class name, not its message.
 */
interface CrashReporter {
    /** False for the no-op and console implementations, so UI can hide crash settings. */
    val isAvailable: Boolean
    fun log(message: String)
    fun recordException(throwable: Throwable)
    fun setKey(key: String, value: String)
    fun setEnabled(enabled: Boolean)
}
