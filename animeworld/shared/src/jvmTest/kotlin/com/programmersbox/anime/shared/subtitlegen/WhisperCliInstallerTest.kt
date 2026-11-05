package com.programmersbox.anime.shared.subtitlegen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okio.Buffer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A whisper.cpp-like Windows zip: the exe and a dll we need, an exe we don't, and an entry escaping `Release/`. */
internal fun testWhisperZip(): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
        listOf(
            "Release/whisper-cli.exe" to "exe",
            "Release/ggml.dll" to "dll",
            "Release/bench.exe" to "bench",
            "Release/../../evil.dll" to "evil",
        ).forEach { (name, body) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(body.encodeToByteArray())
            zip.closeEntry()
        }
    }
    return out.toByteArray()
}

internal fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

internal fun testWhisperAsset(zip: ByteArray) = WhisperCliAsset("b5454", "whisper-bin-x64.zip", sha256Hex(zip), "9 MB")

class WhisperCliInstallerTest {
    private val root = createTempDirectory("cli-installer-test").toFile()
    private val installRoot = File(root, "data/whisper-cli")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val server = MockWebServer().apply { start() }
    private val zip = testWhisperZip()

    private fun installer(
        isWindows: Boolean = true,
        asset: WhisperCliAsset? = testWhisperAsset(zip),
        rename: (File, File) -> Boolean = { from, to -> from.renameTo(to) },
    ) = WhisperCliInstaller(
        installRoot = installRoot,
        client = OkHttpClient(),
        scope = scope,
        isWindows = isWindows,
        arch = "amd64",
        baseUrl = server.url("/").toString(),
        asset = if (isWindows) asset else null,
        rename = rename,
    )

    @Test
    fun renameIsRetriedWhenTheFilesAreHeld() {
        serve(zip)
        val attempts = AtomicInteger()
        // The first attempt fails, as when an antivirus scanner still holds the new .exe.
        val installer = installer(rename = { from, to -> attempts.incrementAndGet() > 1 && from.renameTo(to) })
        installer.install()
        waitFor { installer.state.value !is CliInstall.Installing }

        assertEquals(CliInstall.Ready(File(installRoot, "b5454/whisper-cli.exe")), installer.state.value)
        assertEquals(2, attempts.get())
        assertEquals(listOf("b5454"), installRoot.list()!!.toList())
    }

    @Test
    fun renameGivesUpAfterFiveAttempts() {
        serve(zip)
        val attempts = AtomicInteger()
        val installer = installer(rename = { _, _ -> attempts.incrementAndGet(); false })
        installer.install()
        waitFor { installer.state.value !is CliInstall.Installing }

        assertEquals(CliInstall.Failed("Download failed: Couldn't move b5454.tmp into place"), installer.state.value)
        assertEquals(5, attempts.get())
        assertTrue(installRoot.list().orEmpty().isEmpty())
    }

    private fun serve(body: ByteArray) = server.enqueue(MockResponse.Builder().body(Buffer().write(body)).build())

    private fun waitFor(block: () -> Boolean) = runBlocking {
        withTimeout(10_000) { while (!block()) delay(20) }
    }

    @AfterTest
    fun cleanup() {
        scope.cancel()
        server.close()
        root.deleteRecursively()
    }

    @Test
    fun assetPerArch() {
        assertEquals("whisper-bin-x64.zip", windowsWhisperAsset("amd64")?.fileName)
        assertEquals("whisper-bin-x64.zip", windowsWhisperAsset("x86_64")?.fileName)
        assertEquals("whisper-bin-win-cpu-arm64.zip", windowsWhisperAsset("aarch64")?.fileName)
        assertEquals("whisper-bin-win-cpu-arm64.zip", windowsWhisperAsset("arm64")?.fileName)
        assertNull(windowsWhisperAsset("x86"))
        val x64 = windowsWhisperAsset("amd64")!!
        assertEquals("b5454", x64.version)
        assertEquals("6ba69e3482d7826214f90a6a9c84ca07782aec1e1d0c6a7c30c994fd5d816ccb", x64.sha256)
        assertEquals("9 MB", x64.sizeLabel)
        val arm = windowsWhisperAsset("arm64")!!
        assertEquals("28c37e7b598c3d9bbfef94f3bd67f2ee6f12b7c86f3da5edcf5e4308d615ff6d", arm.sha256)
        assertEquals("4 MB", arm.sizeLabel)
    }

    @Test
    fun defaultAssetComesFromArch() {
        val x86 = WhisperCliInstaller(installRoot, OkHttpClient(), scope, isWindows = true, arch = "x86")
        assertEquals(CliInstall.NotNeeded, x86.state.value)
        assertEquals(CliInstall.Missing, WhisperCliInstaller(installRoot, OkHttpClient(), scope, isWindows = true, arch = "arm64").state.value)
    }

    @Test
    fun installExtractsOnlyCliAndDlls() {
        serve(zip)
        val installer = installer()
        assertEquals(CliInstall.Missing, installer.state.value)
        installer.install()
        waitFor { installer.state.value is CliInstall.Ready }

        val dir = File(installRoot, "b5454")
        assertEquals(File(dir, "whisper-cli.exe"), (installer.state.value as CliInstall.Ready).file)
        assertEquals(setOf("whisper-cli.exe", "ggml.dll"), dir.list()!!.toSet())
        assertEquals("exe", File(dir, "whisper-cli.exe").readText())
        assertEquals(listOf("b5454"), installRoot.list()!!.toList())
        assertFalse(root.walk().any { it.name == "evil.dll" })
        assertEquals("/b5454/whisper-bin-x64.zip", server.takeRequest().url.encodedPath)
    }

    @Test
    fun checksumMismatchFailsThenRetrySucceeds() {
        serve(zip + byteArrayOf(1))
        val installer = installer()
        installer.install()
        waitFor { installer.state.value is CliInstall.Failed }

        assertEquals(CliInstall.Failed("Download failed: checksum mismatch"), installer.state.value)
        assertTrue(installRoot.list().orEmpty().isEmpty())

        serve(zip)
        installer.install()
        waitFor { installer.state.value is CliInstall.Ready }
        assertEquals(setOf("b5454"), installRoot.list()!!.toSet())
    }

    @Test
    fun httpErrorFails() = runBlocking {
        server.enqueue(MockResponse.Builder().code(404).build())
        val installer = installer()
        val error = assertFailsWith<IOException> { installer.ensureInstalled {} }

        assertEquals("Download failed: HTTP 404", error.message)
        assertEquals(CliInstall.Failed("Download failed: HTTP 404"), installer.state.value)
        assertTrue(installRoot.list().orEmpty().isEmpty())
    }

    @Test
    fun ensureInstalledDownloadsAndReportsProgress() = runBlocking {
        serve(zip)
        val installer = installer()
        var last = 0f
        val file = installer.ensureInstalled { last = it }

        assertEquals(File(installRoot, "b5454/whisper-cli.exe"), file)
        assertEquals(1f, last)
        // Already installed: no second request.
        assertEquals(file, installer.ensureInstalled {})
        assertEquals(1, server.requestCount)
    }

    @Test
    fun notWindowsIsNotNeeded() = runBlocking {
        val installer = installer(isWindows = false)
        assertEquals(CliInstall.NotNeeded, installer.state.value)
        installer.install()
        assertNull(installer.ensureInstalled {})
        assertEquals(CliInstall.NotNeeded, installer.state.value)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun existingInstallIsReady() = runBlocking {
        File(installRoot, "b5454").mkdirs()
        val exe = File(installRoot, "b5454/whisper-cli.exe").apply { writeText("exe") }
        val installer = installer()

        assertEquals(CliInstall.Ready(exe), installer.state.value)
        assertEquals(exe, installer.ensureInstalled {})
        assertEquals(0, server.requestCount)
    }

    @Test
    fun staleTmpIsReplaced() {
        File(installRoot, "b5454.tmp").apply { mkdirs(); File(this, "old.dll").writeText("old") }
        serve(zip)
        val installer = installer()
        installer.install()
        waitFor { installer.state.value is CliInstall.Ready }

        assertEquals(setOf("whisper-cli.exe", "ggml.dll"), File(installRoot, "b5454").list()!!.toSet())
        assertFalse(File(installRoot, "b5454.tmp").exists())
    }
}
