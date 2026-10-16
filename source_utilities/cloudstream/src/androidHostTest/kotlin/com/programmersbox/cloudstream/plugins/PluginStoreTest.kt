package com.programmersbox.cloudstream.plugins

import com.programmersbox.cloudstream.repo.CsPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okio.Buffer
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PluginStoreTest {
    private lateinit var server: MockWebServer
    private lateinit var root: File
    private lateinit var store: PluginStore
    private val repo = "https://x/repo.json"

    @BeforeTest
    fun setUp() {
        server = MockWebServer().apply { start() }
        root = Files.createTempDirectory("csstore").toFile()
        store = PluginStore(root, OkHttpClient())
    }

    @AfterTest
    fun tearDown() {
        server.close()
        root.walkBottomUp().forEach { it.setWritable(true); it.delete() }
    }

    private fun serve(bytes: ByteArray) =
        server.enqueue(MockResponse.Builder().body(Buffer().write(bytes)).build())

    private fun sha(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun plugin(name: String = "Foo", version: Int = 1, hash: String? = null) = CsPlugin(
        url = server.url("/plugin.cs3").toString(), status = 1, version = version,
        apiVersion = 1, name = name, internalName = name, fileHash = hash,
    )

    @Test
    fun installsAndRecords() = runTest {
        val bytes = "dex".toByteArray()
        serve(bytes)
        val r = store.install(repo, plugin(hash = "sha256-" + sha(bytes).uppercase()))
        val f = File(r.path)
        assertTrue(f.readBytes().contentEquals(bytes))
        assertFalse(f.canWrite())
        assertTrue(f.absolutePath.startsWith(root.absolutePath))
        assertEquals(listOf(r), store.installed())
        assertEquals(listOf(r), PluginStore(root, OkHttpClient()).installed())
    }

    @Test
    fun hashMismatch_leavesNoFile() = runTest {
        serve("dex".toByteArray())
        assertFailsWith<IOException> { store.install(repo, plugin(hash = "sha256-" + "0".repeat(64))) }
        assertTrue(root.walkTopDown().none { it.isFile && it.name != "installed.json" })
        assertTrue(store.installed().isEmpty())
    }

    @Test
    fun noHash_installs() = runTest {
        serve("dex".toByteArray())
        assertEquals(1, store.install(repo, plugin()).version)
    }

    @Test
    fun updateReplacesEntry() = runTest {
        serve("v1".toByteArray())
        store.install(repo, plugin(version = 1))
        serve("v2".toByteArray())
        val r = store.install(repo, plugin(version = 2))
        assertEquals(listOf(r), store.installed())
        assertEquals("v2", File(r.path).readText())
    }

    @Test
    fun uninstallRemovesFileAndEntry() = runTest {
        serve("dex".toByteArray())
        val r = store.install(repo, plugin())
        store.uninstall("Foo")
        assertFalse(File(r.path).exists())
        assertTrue(store.installed().isEmpty())
    }

    @Test
    fun hashMismatchOnUpdateKeepsOldFileAndEntry() = runTest {
        serve("v1".toByteArray())
        val r = store.install(repo, plugin(version = 1))
        serve("v2".toByteArray())
        assertFailsWith<IOException> { store.install(repo, plugin(version = 2, hash = "sha256-" + "0".repeat(64))) }
        assertEquals(listOf(r), store.installed())
        assertEquals("v1", File(r.path).readText())
    }

    @Test
    fun parallelInstallsOfSamePluginBothSucceedAndLeaveOneValidFile() = runTest {
        val bytes = "same".toByteArray()
        repeat(2) { serve(bytes) }
        val results = (1..2).map {
            async(Dispatchers.IO) { store.install(repo, plugin(hash = "sha256-" + sha(bytes))) }
        }.awaitAll()
        assertEquals(results[0].path, results[1].path)
        assertEquals(1, store.installed().size)
        assertEquals("same", File(results[0].path).readText())
        assertEquals(1, root.walkTopDown().count { it.isFile && it.name.endsWith(".cs3") })
        assertEquals(0, root.walkTopDown().count { it.name.endsWith(".tmp") })
    }

    @Test
    fun corruptIndexIsRebuiltFromDisk() = runTest {
        serve("dex".toByteArray())
        val r = store.install(repo, plugin())
        File(root, "installed.json").writeText("{ not json")
        val rebuilt = store.installed()
        assertEquals(listOf(InstalledPlugin("", "Foo", 0, r.path)), rebuilt)
        assertTrue(root.listFiles().any { it.name.startsWith("installed.json.corrupt-") })
        assertEquals(rebuilt, PluginStore(root, OkHttpClient()).installed())
    }

    @Test
    fun internalNameCannotEscapeRoot() = runTest {
        for (bad in listOf("../evil", "a/b", "..", "/abs", "a\\b")) {
            assertFailsWith<IllegalArgumentException> { store.install(repo, plugin(name = bad)) }
        }
        assertEquals(0, server.requestCount)
    }
}
