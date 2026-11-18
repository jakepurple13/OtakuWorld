package com.programmersbox.kmpuiviews.utils

import com.programmersbox.kmpuiviews.testing.FakeCrashReporter
import com.programmersbox.telemetry.CrashReporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FlowUtilsTelemetryTest {
    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun `catch list emits empty without koin`() = runTest {
        stopKoin()
        val result = flow<List<Int>> { throw IllegalStateException("x") }.dispatchIoAndCatchList().first()
        assertEquals(emptyList(), result)
    }

    @Test
    fun `catch list records when koin has a reporter`() = runTest {
        val crash = FakeCrashReporter()
        startKoin { modules(module { single<CrashReporter> { crash } }) }

        flow<List<Int>> { throw IllegalStateException("x") }.dispatchIoAndCatchList().first()

        assertIs<IllegalStateException>(crash.exceptions.single())
    }
}
