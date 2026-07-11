package com.programmersbox.kmpuiviews.utils

import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.runBlocking
import okio.BufferedSink
import okio.BufferedSource
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class RecordingProcessor(
    override val fileName: String,
    private val payload: String,
) : BackupProcessor(), BackupUiInfo {
    var restoredWith: String? = null
    override val key get() = fileName
    override val displayName get() = fileName
    override val description: String? = null
    override val icon = null
    override suspend fun backup(sink: BufferedSink) { sink.writeUtf8(payload) }
    override suspend fun restore(json: String, bufferedSource: BufferedSource) { restoredWith = json }
    override suspend fun currentSummary() = BackupDataSummary(itemCount = 1)
    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) =
        BackupDataSummary(itemCount = 1, sizeBytes = rawBytes?.size?.toLong())
}

class ZipperTest {
    private val tempFile = File.createTempFile("zippertest", ".zip")

    @AfterTest
    fun cleanup() { tempFile.delete() }

    @Test
    fun `zipFile only writes selected keys, readZip only restores selected keys`() = runBlocking {
        val a = RecordingProcessor("a.json", "payload-a")
        val b = RecordingProcessor("b.json", "payload-b")
        val zipper = Zipper(listOf(a, b))
        val platformFile = PlatformFile(tempFile.absolutePath)

        val zipResults = zipper.zipFile(platformFile, setOf("a.json")) { }
        assertEquals(listOf(true), zipResults.map { it.success })
        assertEquals("a.json", zipResults.single().key)

        val restoreResults = zipper.readZip(platformFile, setOf("a.json")) { }
        assertEquals("payload-a", a.restoredWith)
        assertEquals(null, b.restoredWith)
        assertEquals(1, restoreResults.size)
    }

    @Test
    fun `peekZip reports summaries without calling restore`() = runBlocking {
        val a = RecordingProcessor("a.json", "payload-a")
        val zipper = Zipper(listOf(a))
        val platformFile = PlatformFile(tempFile.absolutePath)

        zipper.zipFile(platformFile, setOf("a.json")) { }
        val summaries = zipper.peekZip(platformFile, listOf(a))

        assertTrue(summaries["a.json"]?.sizeBytes != null && summaries["a.json"]!!.sizeBytes!! > 0)
        assertEquals(null, a.restoredWith)
    }
}
