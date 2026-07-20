package com.programmersbox.jsextensionloader

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.otakuDataStore
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import okio.Path.Companion.toPath
import java.io.File

class CoroutineExtensionUpdateSchedulerTest {

    @BeforeTest
    fun setUp() {
        val tempFile = File.createTempFile("test-datastore", ".preferences_pb").also {
            it.delete()
            it.deleteOnExit()
        }
        otakuDataStore = PreferenceDataStoreFactory.createWithPath(
            produceFile = { tempFile.absolutePath.toPath() }
        )
    }

    @Test
    fun ticksOncePerIntervalWhileEnabled() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling()).apply { setMode(ExtensionUpdateMode.NOTIFY) }
        var checkCount = 0
        val scheduler = CoroutineExtensionUpdateScheduler(
            scope = TestScope(testScheduler),
            checkInterval = 24.hours,
            settings = settings,
            onCheck = { checkCount++ },
        )

        scheduler.start()
        advanceTimeBy(24.hours.inWholeMilliseconds + 1_000)
        advanceTimeBy(24.hours.inWholeMilliseconds + 1_000)
        scheduler.stop()

        assertEquals(2, checkCount)
    }

    @Test
    fun doesNotCheckWhenDisabled() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling()).apply { setMode(ExtensionUpdateMode.DISABLED) }
        var checkCount = 0
        val scheduler = CoroutineExtensionUpdateScheduler(
            scope = TestScope(testScheduler),
            checkInterval = 24.hours,
            settings = settings,
            onCheck = { checkCount++ },
        )

        scheduler.start()
        advanceTimeBy(24.hours.inWholeMilliseconds + 1_000)
        scheduler.stop()

        assertEquals(0, checkCount)
    }

    @Test
    fun stopCancelsFurtherChecks() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling()).apply { setMode(ExtensionUpdateMode.NOTIFY) }
        var checkCount = 0
        val scheduler = CoroutineExtensionUpdateScheduler(
            scope = TestScope(testScheduler),
            checkInterval = 24.hours,
            settings = settings,
            onCheck = { checkCount++ },
        )

        scheduler.start()
        scheduler.stop()
        advanceTimeBy(48.hours.inWholeMilliseconds)

        assertEquals(0, checkCount)
    }
}
