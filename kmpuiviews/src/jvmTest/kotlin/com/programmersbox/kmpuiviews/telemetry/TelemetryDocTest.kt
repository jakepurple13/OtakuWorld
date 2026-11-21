package com.programmersbox.kmpuiviews.telemetry

import com.programmersbox.telemetry.events.telemetryEventNames
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Keeps TELEMETRY.md in step with the event catalog. jvmTest runs from the module directory. */
class TelemetryDocTest {
    private val doc by lazy { File("../TELEMETRY.md").readText() }

    @Test
    fun `every event is documented`() = telemetryEventNames.forEach {
        assertTrue("`$it`" in doc, "$it missing from TELEMETRY.md")
    }

    @Test
    fun `readmes link the doc`() {
        assertTrue("TELEMETRY.md" in File("../README.md").readText())
        assertTrue("TELEMETRY.md" in File("../README-zh.md").readText())
    }
}
