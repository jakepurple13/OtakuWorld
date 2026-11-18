package com.programmersbox.kmpuiviews.telemetry

import com.programmersbox.kmpuiviews.BuildKonfig
import com.programmersbox.telemetry.Analytics
import com.programmersbox.telemetry.CrashReporter

/** Sets the build keys every crash report and analytics session carries. Call once at app start. */
fun startTelemetry(
    crashReporter: CrashReporter,
    analytics: Analytics,
    buildType: String,
    buildFlavor: String,
) {
    crashReporter.setKey("buildType", buildType)
    crashReporter.setKey("buildFlavor", buildFlavor)
    crashReporter.setKey("commit_sha", BuildKonfig.COMMIT_SHA)
    analytics.setUserProperty("buildType", buildType)
    analytics.setUserProperty("buildFlavor", buildFlavor)
}
