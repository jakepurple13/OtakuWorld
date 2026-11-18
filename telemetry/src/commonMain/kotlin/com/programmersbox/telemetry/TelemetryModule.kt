package com.programmersbox.telemetry

import org.koin.core.module.Module
import org.koin.dsl.module

/** Telemetry bindings for platforms without a backend (desktop, iOS). */
fun noOpTelemetryModule(printToConsole: Boolean): Module = module {
    single<Analytics> { NoOpAnalytics }
    single<CrashReporter> { if (printToConsole) ConsoleCrashReporter() else NoOpCrashReporter }
}
