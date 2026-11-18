package com.programmersbox.telemetry

import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TelemetryTest {
    @Test
    fun consoleReporterPrintsBreadcrumbs() {
        val out = mutableListOf<String>()
        ConsoleCrashReporter(out::add).log("nav → details")
        assertEquals(listOf("nav → details"), out)
    }

    @Test
    fun consoleReporterPrintsStackTrace() {
        val out = mutableListOf<String>()
        ConsoleCrashReporter(out::add).recordException(IllegalStateException("boom"))
        assertTrue(out.single().contains("IllegalStateException: boom"))
    }

    @Test
    fun noOpsAreUnavailable() {
        assertFalse(NoOpAnalytics.isAvailable)
        assertFalse(NoOpCrashReporter.isAvailable)
        assertFalse(ConsoleCrashReporter().isAvailable)
    }

    @Test
    fun moduleBindsConsoleWhenPrinting() {
        val koin = koinApplication { modules(noOpTelemetryModule(printToConsole = true)) }.koin
        assertIs<ConsoleCrashReporter>(koin.get<CrashReporter>())
        assertSame(NoOpAnalytics, koin.get<Analytics>())
    }

    @Test
    fun moduleBindsSilentWhenNotPrinting() {
        val koin = koinApplication { modules(noOpTelemetryModule(printToConsole = false)) }.koin
        assertSame(NoOpCrashReporter, koin.get<CrashReporter>())
    }
}
