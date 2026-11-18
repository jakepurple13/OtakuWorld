package com.programmersbox.telemetry.android

import com.programmersbox.telemetry.Analytics
import com.programmersbox.telemetry.CrashReporter

internal fun platformAnalytics(): Analytics = FirebaseAnalyticsImpl()

internal fun platformCrashReporter(printToConsole: Boolean): CrashReporter = CrashlyticsReporter(printToConsole)
