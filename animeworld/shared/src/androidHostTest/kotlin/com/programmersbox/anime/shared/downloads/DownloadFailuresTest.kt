package com.programmersbox.anime.shared.downloads

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadFailuresTest {
    private lateinit var dir: File

    @BeforeTest
    fun setUp() {
        dir = createTempDirectory("failures").toFile()
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun request(title: String = "Ep 1") = EpisodeDownloadRequest(
        url = "https://example.com/v.mp4",
        headers = emptyMap(),
        mimeType = null,
        qualityHint = null,
        subtitles = emptyList(),
        title = title,
        folder = "/folder",
    )

    private fun fail(key: String, atMs: Long = 1L, title: String = "Ep 1"): File {
        val file = writeRequestFile(dir, key, request(title))
        writeFailure(file, DownloadFailure(key, title, "boom", atMs))
        return file
    }

    @Test
    fun failureSurvivesRereadFromDisk() {
        fail("/folderEp 1")
        val failures = listFailures(dir)
        assertEquals(1, failures.size)
        assertEquals("/folderEp 1", failures.single().key)
        assertEquals("Ep 1", failures.single().title)
        assertEquals("boom", failures.single().error)
    }

    @Test
    fun clearFailureRemovesOnlyMarker() {
        val file = fail("k")
        clearFailure(dir, "k")
        assertTrue(listFailures(dir).isEmpty())
        assertTrue(file.exists())
    }

    @Test
    fun failureWithoutRequestFileIsSkipped() {
        fail("k").delete()
        assertTrue(listFailures(dir).isEmpty())
    }

    @Test
    fun corruptMarkerIsSkipped() {
        val file = writeRequestFile(dir, "k", request())
        File(dir, file.nameWithoutExtension + ".failed.json").writeText("{")
        assertTrue(listFailures(dir).isEmpty())
    }

    @Test
    fun requestFileForMatchesWriteRequestFile() {
        assertEquals(writeRequestFile(dir, "k", request()), requestFileFor(dir, "k"))
    }

    @Test
    fun newestFailureFirst() {
        fail("a", atMs = 1)
        fail("b", atMs = 2)
        assertEquals(listOf(2L, 1L), listFailures(dir).map { it.atMs })
    }

    @Test
    fun successDeletesRequestNoMarker() {
        val o = requestOutcome(succeeded = true, failed = false, isStopped = false, cancelledByApp = false)
        assertTrue(o.deleteRequest)
        assertFalse(o.writeFailure)
    }

    @Test
    fun failureKeepsRequestAndWritesMarker() {
        val o = requestOutcome(succeeded = false, failed = true, isStopped = false, cancelledByApp = false)
        assertFalse(o.deleteRequest)
        assertTrue(o.writeFailure)
    }

    @Test
    fun cancelledByAppDeletesRequestNoMarker() {
        val o = requestOutcome(succeeded = false, failed = false, isStopped = true, cancelledByApp = true)
        assertTrue(o.deleteRequest)
        assertFalse(o.writeFailure)
    }

    @Test
    fun stoppedBySystemKeepsRequestNoMarker() {
        val o = requestOutcome(succeeded = false, failed = false, isStopped = true, cancelledByApp = false)
        assertFalse(o.deleteRequest)
        assertFalse(o.writeFailure)
    }

    @Test
    fun removeDownloadDeletesRecordedPart() {
        val folder = File(dir, "videos").apply { mkdirs() }
        val part = File(folder, "Ep 1.ts.part").apply { writeText("x") }
        val done = File(folder, "Ep 0.ts").apply { writeText("x") }
        val requestFile = fail("k")
        val record = partsRecordFor(requestFile)
        record.writeText(part.absolutePath + "\n" + done.absolutePath + "\n")

        removeDownload(dir, "k")

        assertFalse(part.exists())
        assertTrue(done.exists())
        assertFalse(record.exists())
        assertFalse(requestFile.exists())
        assertTrue(listFailures(dir).isEmpty())
    }
}
