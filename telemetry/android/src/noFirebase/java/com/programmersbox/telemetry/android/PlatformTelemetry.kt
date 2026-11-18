package com.programmersbox.telemetry.android

import com.programmersbox.telemetry.Analytics
import com.programmersbox.telemetry.ConsoleCrashReporter
import com.programmersbox.telemetry.CrashReporter
import com.programmersbox.telemetry.NoOpAnalytics
import com.programmersbox.telemetry.NoOpCrashReporter

internal fun platformAnalytics(): Analytics = NoOpAnalytics

internal fun platformCrashReporter(printToConsole: Boolean): CrashReporter =
    if (printToConsole) ConsoleCrashReporter() else NoOpCrashReporter
