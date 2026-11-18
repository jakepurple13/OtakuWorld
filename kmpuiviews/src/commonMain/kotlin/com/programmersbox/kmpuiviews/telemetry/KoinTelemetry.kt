package com.programmersbox.kmpuiviews.telemetry

import com.programmersbox.telemetry.CrashReporter
import com.programmersbox.telemetry.NoOpCrashReporter
import org.koin.mp.KoinPlatform

/**
 * The Koin [CrashReporter] for top-level helpers that have no injection scope.
 * Falls back to [NoOpCrashReporter] when Koin is not started or has no binding.
 */
fun koinCrashReporter(): CrashReporter =
    KoinPlatform.getKoinOrNull()?.getOrNull<CrashReporter>() ?: NoOpCrashReporter
