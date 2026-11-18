package com.programmersbox.kmpuiviews.telemetry

import com.programmersbox.kmpuiviews.BuildKonfig
import com.programmersbox.kmpuiviews.testing.FakeAnalytics
import com.programmersbox.kmpuiviews.testing.FakeCrashReporter
import kotlin.test.Test
import kotlin.test.assertEquals

class TelemetryStartupTest {
    @Test
    fun `startTelemetry sets build and commit keys`() {
        val crash = FakeCrashReporter()
        val analytics = FakeAnalytics()

        startTelemetry(crash, analytics, buildType = "debug", buildFlavor = "full")

        assertEquals("debug", crash.keys["buildType"])
        assertEquals("full", crash.keys["buildFlavor"])
        assertEquals(BuildKonfig.COMMIT_SHA, crash.keys["commit_sha"])
        assertEquals("debug", analytics.userProperties["buildType"])
        assertEquals("full", analytics.userProperties["buildFlavor"])
    }
}
