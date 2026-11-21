package com.programmersbox.kmpuiviews.telemetry

import com.programmersbox.telemetry.Analytics
import com.programmersbox.telemetry.CrashReporter
import com.programmersbox.telemetry.NoOpAnalytics
import com.programmersbox.telemetry.NoOpCrashReporter
import org.koin.mp.KoinPlatform

/**
 * The Koin [CrashReporter] for top-level helpers that have no injection scope.
 * Falls back to [NoOpCrashReporter] when Koin is not started or has no binding.
 */
fun koinCrashReporter(): CrashReporter =
    KoinPlatform.getKoinOrNull()?.getOrNull<CrashReporter>() ?: NoOpCrashReporter

/** The Koin [Analytics] for code with no injection scope; [NoOpAnalytics] when Koin is not started. */
fun koinAnalytics(): Analytics =
    KoinPlatform.getKoinOrNull()?.getOrNull<Analytics>() ?: NoOpAnalytics

/** A source name telemetry may send (see [TelemetrySourceNamer]); the raw name when Koin has no namer. */
fun koinSourceName(serviceName: String): String =
    KoinPlatform.getKoinOrNull()?.getOrNull<TelemetrySourceNamer>()?.name(serviceName) ?: serviceName
