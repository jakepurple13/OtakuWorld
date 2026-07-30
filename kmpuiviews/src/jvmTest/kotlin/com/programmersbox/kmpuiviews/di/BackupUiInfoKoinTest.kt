package com.programmersbox.kmpuiviews.di

import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
import okio.BufferedSink
import okio.BufferedSource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeProcessor : BackupProcessor(), BackupUiInfo {
    override val fileName = "fake.json"
    override val key = "fake.json"
    override val displayName = "Fake"
    override val description: String? = null
    override val icon = null
    override suspend fun backup(sink: BufferedSink): ProcessorResult = ProcessorResult(successCount = 1)
    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult = ProcessorResult(successCount = 1)
    override suspend fun currentSummary() = BackupDataSummary()
    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary()
}

class BackupUiInfoKoinTest {
    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun `binds both BackupProcessor and BackupUiInfo`() {
        val koin = koinApplication {
            modules(module { backupProcessorWithUiInfo("fake", ::FakeProcessor) })
        }.koin

        val asProcessor = koin.getAll<BackupProcessor>()
        val asUiInfo = koin.getAll<BackupUiInfo>()

        assertEquals(1, asProcessor.size)
        assertEquals(1, asUiInfo.size)
        assertTrue(asProcessor.single().fileName == "fake.json")
        assertTrue(asUiInfo.single().key == "fake.json")
    }
}
