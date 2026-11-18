package com.programmersbox.kmpuiviews.testing

import com.programmersbox.telemetry.Analytics
import com.programmersbox.telemetry.AnalyticsEvent
import com.programmersbox.telemetry.CrashReporter

class FakeCrashReporter : CrashReporter {
    override val isAvailable: Boolean = true
    val logs = mutableListOf<String>()
    val exceptions = mutableListOf<Throwable>()
    val keys = mutableMapOf<String, String>()
    var collecting = true

    override fun log(message: String) {
        logs += message
    }

    override fun recordException(throwable: Throwable) {
        exceptions += throwable
    }

    override fun setKey(key: String, value: String) {
        keys[key] = value
    }

    override fun setEnabled(enabled: Boolean) {
        collecting = enabled
    }
}

class FakeAnalytics : Analytics {
    override val isAvailable: Boolean = true
    val events = mutableListOf<AnalyticsEvent>()
    val screens = mutableListOf<String>()
    val userProperties = mutableMapOf<String, String?>()
    var collecting = true

    override fun log(event: AnalyticsEvent) {
        events += event
    }

    override fun screen(name: String) {
        screens += name
    }

    override fun setUserProperty(name: String, value: String?) {
        userProperties[name] = value
    }

    override fun setEnabled(enabled: Boolean) {
        collecting = enabled
    }
}
