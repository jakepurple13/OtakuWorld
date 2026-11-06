package com.programmersbox.kmpuiviews.translation.bergamot

import com.programmersbox.kmpuiviews.domain.ModelState
import com.programmersbox.kmpuiviews.translation.TranslationUnavailable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BergamotModelStoreTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dir: File = Files.createTempDirectory("bergamot-store").toFile()

    private val jaEn = fakePair("ja", "en", 10)
    private val koEn = fakePair("ko", "en", 20)
    private val enFr = fakePair("en", "fr", 40)
    private val server = FakeServer(jaEn + koEn + enFr)

    @AfterTest
    fun tearDown() {
        scope.cancel()
        server.close()
        dir.deleteRecursively()
    }

    /** Waits until a `.part` file exists, i.e. a gated download is mid-body. */
    private suspend fun awaitPartFile() = withTimeout(10_000) {
        while (partFiles().isEmpty()) delay(10)
    }

    private suspend fun store(target: String = "fr"): BergamotModelStore {
        val store = BergamotModelStore(server.index(), dir, server.client, scope)
        store.setTarget(target)
        store.refresh()
        return store
    }

    private suspend fun BergamotModelStore.row(source: String, until: (BergamotRow) -> Boolean): BergamotRow =
        withTimeout(10_000) {
            rows.first { rows -> rows.firstOrNull { it.source == source }?.let(until) == true }
                .first { it.source == source }
        }

    private suspend fun BergamotModelStore.downloadAndWait(source: String) {
        download(source)
        row(source) { it.state == ModelState.Downloaded }
    }

    private suspend fun BergamotModelStore.total(expected: Long) =
        withTimeout(10_000) { totalBytes.first { it == expected } }

    private fun pairDir(pair: String) = File(dir, pair)

    private fun partFiles() = dir.walk().filter { it.name.endsWith(".part") }.toList()

    @Test
    fun `rows cover every source reachable into the target`() = runBlocking<Unit> {
        val store = store("fr")
        val rows = withTimeout(10_000) { store.rows.first { it.isNotEmpty() } }
        assertEquals(setOf("en", "ja", "ko"), rows.map { it.source }.toSet())
        val ja = rows.single { it.source == "ja" }
        assertEquals(listOf("ja-en", "en-fr"), ja.chain.map { "${it.from}-${it.to}" })
        assertEquals(ModelState.NotDownloaded, ja.state)
        assertEquals(150L, ja.ownBytes)
        assertEquals(0L, ja.sharedBytes)
        assertEquals(0L, ja.freedOnDelete)
    }

    @Test
    fun `download fetches files, checks them and writes no part files`() = runBlocking<Unit> {
        val store = store()
        store.downloadAndWait("ja")
        store.total(150)
        assertTrue(pairDir("ja-en").listFiles()!!.isNotEmpty())
        assertTrue(pairDir("en-fr").listFiles()!!.isNotEmpty())
        assertEquals(emptyList(), partFiles())
        val files = assertNotNull(store.filesFor("ja", "fr"))
        assertEquals(2, files.size)
        assertTrue(files.all { it.model.isFile && it.lex!!.isFile && it.srcVocab == it.trgVocab })
        // Marian loads a vocab as SentencePiece only when the name ends in .spm (found in the real run).
        assertTrue(files.all { it.srcVocab.name.endsWith(".spm") }, files.toString())
    }

    @Test
    fun `download fetches only missing pair files`() = runBlocking<Unit> {
        val store = store()
        store.downloadAndWait("ja")
        val before = server.requests.count { it.contains("enfr") }
        store.downloadAndWait("ko")
        assertEquals(before, server.requests.count { it.contains("enfr") })
        store.total(210)
    }

    @Test
    fun `downloads survive a new store`() = runBlocking<Unit> {
        store().downloadAndWait("ja")
        val again = store()
        assertEquals(ModelState.Downloaded, again.row("ja") { it.state == ModelState.Downloaded }.state)
        again.total(150)
    }

    @Test
    fun `delete keeps shared pivot file`() = runBlocking<Unit> {
        val store = store()
        store.downloadAndWait("ja")
        store.downloadAndWait("ko")
        store.total(210)

        store.delete("ja")

        assertTrue(pairDir("en-fr").listFiles()!!.isNotEmpty())
        assertFalse(pairDir("ja-en").exists())
        store.total(180) // 210 - ja-en (30)
        assertEquals(ModelState.NotDownloaded, store.row("ja") { it.state == ModelState.NotDownloaded }.state)
        assertNull(store.filesFor("ja", "fr"))
        assertNotNull(store.filesFor("ko", "fr"))
    }

    @Test
    fun `delete of the last row using a pivot removes it`() = runBlocking<Unit> {
        val store = store()
        store.downloadAndWait("ja")
        store.delete("ja")
        assertFalse(pairDir("ja-en").exists())
        assertFalse(pairDir("en-fr").exists())
        store.total(0)
    }

    @Test
    fun `freedOnDelete excludes shared files`() = runBlocking<Unit> {
        val store = store()
        store.downloadAndWait("ja")
        store.downloadAndWait("ko")
        val ja = store.row("ja") { it.state == ModelState.Downloaded && it.sharedBytes > 0 }
        assertEquals(30L, ja.freedOnDelete)
        assertEquals(30L, ja.ownBytes)
        assertEquals(120L, ja.sharedBytes)
        val ko = store.row("ko") { it.sharedBytes > 0 }
        assertEquals(60L, ko.freedOnDelete)
    }

    @Test
    fun `failed download leaves no part file and state Failed`() = runBlocking<Unit> {
        server.failures[enFr.first { it.type == "lex" }.location] = 1
        val store = store()
        store.download("ja")
        val row = store.row("ja") { it.state is ModelState.Failed }
        assertIs<ModelState.Failed>(row.state)
        assertEquals(emptyList(), partFiles())
        assertNull(store.filesFor("ja", "fr"))
    }

    @Test
    fun `retry after failure succeeds`() = runBlocking<Unit> {
        server.failures[enFr.first { it.type == "model" }.location] = 1
        val store = store()
        store.download("ja")
        store.row("ja") { it.state is ModelState.Failed }
        store.downloadAndWait("ja")
        assertNotNull(store.filesFor("ja", "fr"))
        assertEquals(emptyList(), partFiles())
    }

    @Test
    fun `cancel removes the part file and returns to NotDownloaded`() = runBlocking<Unit> {
        server.gates[jaEn.first { it.type == "model" }.location] = CompletableDeferred()
        val store = store()
        store.download("ja")
        awaitPartFile()
        assertIs<ModelState.Downloading>(store.row("ja") { it.state is ModelState.Downloading }.state)

        store.cancel("ja")

        assertEquals(ModelState.NotDownloaded, store.row("ja") { it.state == ModelState.NotDownloaded }.state)
        withTimeout(10_000) { while (partFiles().isNotEmpty()) delay(10) }
        assertNull(store.filesFor("ja", "fr"))
    }

    @Test
    fun `delete keeps a pivot an in-flight download needs`() = runBlocking<Unit> {
        val store = store()
        store.downloadAndWait("ja")
        // ko→fr only needs ko-en; hold it mid-body so the download is in flight during the delete.
        val gate = CompletableDeferred<Unit>()
        server.gates[koEn.first { it.type == "model" }.location] = gate
        store.download("ko")
        awaitPartFile()

        store.delete("ja")

        assertFalse(pairDir("ja-en").exists())
        assertTrue(pairDir("en-fr").listFiles()!!.isNotEmpty())
        gate.complete(Unit)
        store.row("ko") { it.state == ModelState.Downloaded }
        assertNotNull(store.filesFor("ko", "fr"))
        store.total(180)
    }

    @Test
    fun `sha mismatch fails download`() = runBlocking<Unit> {
        val bad = FakeFile("ja", "en", "model", ByteArray(10) { 1 }, hash = "0".repeat(64))
        server.files = listOf(bad) + jaEn.filter { it.type != "model" } + enFr
        val store = store()
        store.download("ja")
        val row = store.row("ja") { it.state is ModelState.Failed }
        assertIs<ModelState.Failed>(row.state)
        assertEquals(emptyList(), partFiles())
        assertTrue(pairDir("ja-en").listFiles().orEmpty().none { it.name.startsWith("model") })
        assertNull(store.filesFor("ja", "fr"))
    }

    @Test
    fun `engine runs each chain step in order`() = runBlocking<Unit> {
        val store = store()
        store.downloadAndWait("ja")
        val runtime = FakeRuntime()
        val engine = BergamotEngine(store, runtime)
        assertEquals("[[こんにちは|ja-en]|en-fr]", engine.translate("こんにちは", "ja", "fr"))
        assertEquals(listOf("ja-en", "en-fr"), runtime.calls.map { it.second.model.parentFile.name })
        assertTrue("fr" in engine.targets())
    }

    @Test
    fun `engine reports missing models`() = runBlocking<Unit> {
        val engine = BergamotEngine(store(), FakeRuntime())
        val e = assertFailsWith<TranslationUnavailable> { engine.translate("こんにちは", "ja", "fr") }
        assertEquals("Download Japanese → French in Settings → Translation", e.message)
    }

    @Test
    fun `engine reports runtime not installed`() = runBlocking<Unit> {
        val store = store()
        store.downloadAndWait("ja")
        val engine = BergamotEngine(store, FakeRuntime(reason = "Install the translator (9 MB)"))
        val e = assertFailsWith<TranslationUnavailable> { engine.translate("こんにちは", "ja", "fr") }
        assertEquals("Install the translator (9 MB)", e.message)
    }
}
