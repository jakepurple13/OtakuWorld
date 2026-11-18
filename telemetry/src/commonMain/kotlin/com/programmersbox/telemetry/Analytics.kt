package com.programmersbox.telemetry

/**
 * Product analytics. Implementations must never throw.
 *
 * Never pass item titles, URLs, search text, note text, list names, emails or file paths.
 */
interface Analytics {
    /** False for the no-op implementations, so UI can hide analytics settings. */
    val isAvailable: Boolean
    fun log(event: AnalyticsEvent)
    fun screen(name: String)
    fun setUserProperty(name: String, value: String?)
    fun setEnabled(enabled: Boolean)
}

/** One analytics event. Names are snake_case, at most 40 characters, with at most 25 params. */
abstract class AnalyticsEvent(
    val name: String,
    val params: Map<String, Any> = emptyMap(),
)
