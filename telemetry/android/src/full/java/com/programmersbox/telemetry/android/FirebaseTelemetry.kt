package com.programmersbox.telemetry.android

import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.crashlytics
import com.programmersbox.telemetry.Analytics
import com.programmersbox.telemetry.AnalyticsEvent
import com.programmersbox.telemetry.CrashReporter

internal class CrashlyticsReporter(private val printToConsole: Boolean) : CrashReporter {
    override val isAvailable: Boolean = true

    override fun log(message: String) {
        if (printToConsole) println(message)
        runCatching { Firebase.crashlytics.log(message) }
    }

    override fun recordException(throwable: Throwable) {
        if (printToConsole) throwable.printStackTrace()
        runCatching { Firebase.crashlytics.recordException(throwable) }
    }

    override fun setKey(key: String, value: String) {
        runCatching { Firebase.crashlytics.setCustomKey(key, value) }
    }

    override fun setEnabled(enabled: Boolean) {
        runCatching { Firebase.crashlytics.isCrashlyticsCollectionEnabled = enabled }
    }
}

internal class FirebaseAnalyticsImpl : Analytics {
    override val isAvailable: Boolean = true

    override fun log(event: AnalyticsEvent) {
        runCatching {
            Firebase.analytics.logEvent(event.name) {
                event.params.forEach { (key, value) ->
                    when (value) {
                        is String -> param(key, value)
                        is Long -> param(key, value)
                        is Int -> param(key, value.toLong())
                        is Double -> param(key, value)
                        is Float -> param(key, value.toDouble())
                        is Boolean -> param(key, value.toString())
                        else -> param(key, value.toString())
                    }
                }
            }
        }
    }

    override fun screen(name: String) {
        runCatching {
            Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, name)
            }
        }
    }

    override fun setUserProperty(name: String, value: String?) {
        runCatching { Firebase.analytics.setUserProperty(name, value) }
    }

    override fun setEnabled(enabled: Boolean) {
        runCatching { Firebase.analytics.setAnalyticsCollectionEnabled(enabled) }
    }
}
