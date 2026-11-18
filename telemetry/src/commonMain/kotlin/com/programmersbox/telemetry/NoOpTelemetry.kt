package com.programmersbox.telemetry

object NoOpAnalytics : Analytics {
    override val isAvailable: Boolean = false
    override fun log(event: AnalyticsEvent) = Unit
    override fun screen(name: String) = Unit
    override fun setUserProperty(name: String, value: String?) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
}

object NoOpCrashReporter : CrashReporter {
    override val isAvailable: Boolean = false
    override fun log(message: String) = Unit
    override fun recordException(throwable: Throwable) = Unit
    override fun setKey(key: String, value: String) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
}

/** Sends nothing anywhere; prints breadcrumbs and stack traces for local development. */
class ConsoleCrashReporter(
    private val print: (String) -> Unit = ::println,
) : CrashReporter {
    override val isAvailable: Boolean = false
    override fun log(message: String) = print(message)
    override fun recordException(throwable: Throwable) = print(throwable.stackTraceToString())
    override fun setKey(key: String, value: String) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
}
