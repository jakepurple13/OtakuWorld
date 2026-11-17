package com.programmersbox.kmpuiviews.domain.aitext.modelimport

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ModelImporterTest {
    private val root = Files.createTempDirectory("importer").toFile()
    private val models = File(root, "models")
    private val cache = File(root, "cache")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @AfterTest
    fun tearDown() {
        scope.cancel()
        root.deleteRecursively()
    }

    private fun importer(
        verify: suspend (File) -> Unit = {},
        space: Long = Long.MAX_VALUE,
    ) = ModelImporter(models, cache, scope, verify, usableSpace = { space })

    /** [n] bytes of a model file: the LITERTLM header, then filler. */
    private fun bytes(n: Int) = ByteArray(n) { (it % 251).toByte() }.also { "LITERTLM".encodeToByteArray().copyInto(it) }

    /** A source that hands out [data] in small pieces, optionally waiting on [gate] after the first one. */
    private fun source(data: ByteArray, gate: CompletableDeferred<Unit>? = null): () -> RawSource = {
        object : RawSource {
            var pos = 0
            override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
                if (pos >= data.size) return -1
                if (pos > 0 && gate != null) runBlocking { gate.await() }
                val n = minOf(byteCount.toInt(), 1024, data.size - pos)
                sink.write(data, pos, pos + n)
                pos += n
                return n.toLong()
            }

            override fun close() = Unit
        }
    }

    private fun ModelImporter.awaitEnd(): ImportState = runBlocking {
        withTimeout(10_000) { state.first { it is ImportState.Idle || it is ImportState.Failed } }
    }

    /** Leftovers of an import: the staging folder's contents. */
    private fun partFiles() = File(models, ModelImporter.STAGING_DIR).listFiles().orEmpty().toList()

    @Test
    fun `wrong extension fails NotAModel without writing`() {
        val i = importer()
        i.import("model.task", 10, source(bytes(10)), replacing = null)
        assertEquals(ImportState.Failed(ImportError.NotAModel), i.state.value)
        assertTrue(models.listFiles().isNullOrEmpty())
    }

    // LiteRT aborts the whole process (CheckJNI) on some broken files, so they must never reach it
    @Test
    fun `file without the LITERTLM header fails NotAModel without verifying`() {
        var verified = false
        val i = importer(verify = { verified = true })
        val data = ByteArray(5000) { 7 }
        i.import("m.litertlm", data.size.toLong(), source(data), replacing = null)
        assertEquals(ImportState.Failed(ImportError.NotAModel), i.awaitEnd())
        assertFalse(verified)
        assertFalse(File(models, "m.litertlm").exists())
        assertTrue(partFiles().isEmpty())
    }

    @Test
    fun `leftover staging folder from a killed import is cleared on start`() {
        val staging = File(models, ModelImporter.STAGING_DIR).apply { mkdirs() }
        File(staging, "m.litertlm").writeText("half")
        importer()
        // The cleanup runs off the caller's thread
        runBlocking { withTimeout(5_000) { while (staging.exists()) kotlinx.coroutines.delay(10) } }
        assertFalse(staging.exists())
    }

    @Test
    fun `no space fails NoSpace with numbers`() {
        val i = importer(space = 50)
        i.import("m.litertlm", 10, source(bytes(10)), replacing = null)
        val failed = assertIs<ImportState.Failed>(i.state.value)
        assertEquals(ImportError.NoSpace(needed = 10 + ModelImporter.HEADROOM_BYTES, available = 50), failed.reason)
    }

    @Test
    fun `copies via part file then verifies and goes Idle`() {
        var verified: File? = null
        val i = importer(verify = { verified = it; assertTrue(it.exists()) })
        val data = bytes(5000)
        i.import("Gemma.LITERTLM", data.size.toLong(), source(data), replacing = null)
        assertEquals(ImportState.Idle, i.awaitEnd())
        val out = File(models, "Gemma.LITERTLM")
        // Checked in the staging folder, before it appears in the models folder
        assertEquals(File(File(models, ModelImporter.STAGING_DIR), "Gemma.LITERTLM"), verified)
        assertTrue(out.readBytes().contentEquals(data))
        assertTrue(partFiles().isEmpty())
    }

    @Test
    fun `progress reports copied bytes`() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val i = importer()
        val data = bytes(5000)
        i.import("m.litertlm", data.size.toLong(), source(data, gate), replacing = null)
        val copying = withTimeout(5_000) {
            i.state.first { it is ImportState.Copying && it.copied > 0 } as ImportState.Copying
        }
        assertEquals("m.litertlm", copying.name)
        assertEquals(5000L, copying.total)
        gate.complete(Unit)
        assertEquals(ImportState.Idle, i.awaitEnd())
    }

    @Test
    fun `cancel deletes part and returns Idle`() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val i = importer()
        i.import("m.litertlm", 5000, source(bytes(5000), gate), replacing = null)
        withTimeout(5_000) { i.state.first { it is ImportState.Copying && it.copied > 0 } }
        i.cancel()
        gate.complete(Unit)
        assertEquals(ImportState.Idle, i.awaitEnd())
        assertTrue(partFiles().isEmpty())
        assertFalse(File(models, "m.litertlm").exists())
    }

    @Test
    fun `failed verify deletes the copy and keeps the replaced file`() {
        models.mkdirs()
        val old = File(models, "old.litertlm").apply { writeText("old") }
        val i = importer(verify = { error("bad model") })
        i.import("new.litertlm", 10, source(bytes(10)), replacing = "old.litertlm")
        assertEquals(ImportState.Failed(ImportError.WontLoad("bad model")), i.awaitEnd())
        assertFalse(File(models, "new.litertlm").exists())
        assertTrue(old.exists())
        assertTrue(partFiles().isEmpty())
    }

    @Test
    fun `successful replace deletes the old file`() {
        models.mkdirs()
        val old = File(models, "old.litertlm").apply { writeText("old") }
        val i = importer()
        i.import("new.litertlm", 10, source(bytes(10)), replacing = "old.litertlm")
        assertEquals(ImportState.Idle, i.awaitEnd())
        assertFalse(old.exists())
        assertTrue(File(models, "new.litertlm").exists())
    }

    @Test
    fun `same name replace keeps old until verify passes`() {
        models.mkdirs()
        val existing = File(models, "m.litertlm").apply { writeText("old") }
        val failing = importer(verify = { error("nope") })
        failing.import("m.litertlm", 10, source(bytes(10)), replacing = "m.litertlm")
        assertIs<ImportState.Failed>(failing.awaitEnd())
        assertEquals("old", existing.readText())
        assertTrue(partFiles().isEmpty())

        val data = bytes(10)
        val passing = importer()
        passing.import("m.litertlm", 10, source(data), replacing = "m.litertlm")
        assertEquals(ImportState.Idle, passing.awaitEnd())
        assertTrue(existing.readBytes().contentEquals(data))
        assertTrue(partFiles().isEmpty())
    }

    @Test
    fun `remove deletes model and its cache files`() {
        models.mkdirs(); cache.mkdirs()
        val model = File(models, "m.litertlm").apply { writeText("x") }
        val c1 = File(cache, "m.litertlm.xnnpack_cache_abc").apply { writeText("x") }
        val other = File(cache, "other.litertlm.xnnpack_cache").apply { writeText("x") }
        importer().remove("m.litertlm")
        assertFalse(model.exists())
        assertFalse(c1.exists())
        assertTrue(other.exists())
    }
}
