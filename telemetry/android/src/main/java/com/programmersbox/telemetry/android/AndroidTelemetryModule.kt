package com.programmersbox.telemetry.android

import com.programmersbox.telemetry.Analytics
import com.programmersbox.telemetry.CrashReporter
import org.koin.core.module.Module
import org.koin.dsl.module

/** Telemetry bindings for Android. The `full` flavor binds Firebase; `noFirebase` binds no-ops. */
fun androidTelemetryModule(printToConsole: Boolean): Module = module {
    single<Analytics> { platformAnalytics() }
    single<CrashReporter> { platformCrashReporter(printToConsole) }
}
