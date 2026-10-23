package com.programmersbox.kmpuiviews.repository

import com.programmersbox.kmpextensionloader.ApkExtensionStore
import com.programmersbox.kmpextensionloader.ApkInstallException
import com.programmersbox.kmpextensionloader.ApkInstallFailure
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import com.programmersbox.kmpuiviews.utils.InstallErrorReason
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DownloadStateRepositoryTest {
    private lateinit var tempDir: File
    private var reloads = 0
    private val installed = mutableListOf<Long>()

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("dl-test").toFile().resolve("downloads")
        reloads = 0
        installed.clear()
    }

    @AfterTest
    fun tearDown() {
        tempDir.parentFile.deleteRecursively()
    }

    private val body = ByteArray(200_000) { it.toByte() }

    private fun okClient() = HttpClient(MockEngine {
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, body.size.toString()))
    })

    private class CountingStore(
        private val accepting: Boolean,
        private val failure: ApkInstallFailure?,
        private val onInstall: (File) -> Unit,
    ) : ApkExtensionStore {
        var installCount = 0

        override fun accepts(file: File): Boolean = accepting

        override suspend fun install(apk: File): String {
            installCount++
            onInstall(apk)
            if (failure != null) throw ApkInstallException(failure, "boom-$failure")
            return "pkg"
        }

        override fun versionName(packageName: String): String? = null
    }

    private fun store(failure: ApkInstallFailure? = null, accepts: Boolean = true) =
        CountingStore(accepts, failure) { installed += it.length() }

    private fun repo(
        client: HttpClient = okClient(),
        stores: List<ApkExtensionStore> = listOf(store()),
    ) = DownloadStateRepository(client, stores, tempDir, reload = { reloads++ })

    private fun DownloadStateRepository.start(url: String = "https://x.test/path/ext.apk") = downloadAndInstall(url)

    private suspend fun DownloadStateRepository.terminal(): DownloadAndInstallState = withTimeout(10_000) {
        downloadList.first { l ->
            l.isNotEmpty() && l.last().status.let {
                it is DownloadAndInstallStatus.Installed ||
                        it is DownloadAndInstallStatus.Error ||
                        it is DownloadAndInstallStatus.Cancelled
            }
        }.last()
    }

    @Test
    fun successGoesDownloadingInstallingInstalledAndReloads() = runBlocking {
        val r = repo()
        r.start()
        val final = r.terminal()
        assertEquals(DownloadAndInstallStatus.Installed, final.status)
        assertEquals("ext.apk", final.name)
        assertEquals(listOf(body.size.toLong()), installed)
        assertEquals(1, reloads)
    }

    @Test
    fun picksFirstStoreThatAcceptsFile() = runBlocking {
        val rejecting = store(accepts = false)
        val accepting = store()
        val r = repo(stores = listOf(rejecting, accepting))
        r.start()
        assertEquals(DownloadAndInstallStatus.Installed, r.terminal().status)
        assertEquals(0, rejecting.installCount)
        assertEquals(1, accepting.installCount)
    }

    @Test
    fun noStoreAcceptsFileReportsInvalid() = runBlocking {
        val r = repo(stores = listOf(store(accepts = false)))
        r.start()
        val status = r.terminal().status
        assertEquals(InstallErrorReason.INVALID, (status as DownloadAndInstallStatus.Error).reason)
        assertEquals(0, reloads)
    }

    @Test
    fun invalidApkReportsInvalid() = runBlocking {
        val r = repo(stores = listOf(store(ApkInstallFailure.INVALID)))
        r.start()
        assertEquals(
            DownloadAndInstallStatus.Error(InstallErrorReason.INVALID, "boom-INVALID"),
            r.terminal().status,
        )
        assertEquals(0, reloads)
    }

    @Test
    fun incompatibleReportsIncompatible() = runBlocking {
        val r = repo(stores = listOf(store(ApkInstallFailure.INCOMPATIBLE)))
        r.start()
        assertEquals(
            DownloadAndInstallStatus.Error(InstallErrorReason.INCOMPATIBLE, "boom-INCOMPATIBLE"),
            r.terminal().status,
        )
    }

    @Test
    fun http404ReportsGeneric() = runBlocking {
        val r = repo(client = HttpClient(MockEngine { respond("", HttpStatusCode.NotFound) }))
        r.start()
        assertEquals(
            DownloadAndInstallStatus.Error(InstallErrorReason.GENERIC, "HTTP 404"),
            r.terminal().status,
        )
        assertEquals(0, reloads)
    }

    @Test
    fun noStoreReportsUnknown() = runBlocking {
        val r = repo(stores = emptyList())
        r.start()
        assertEquals(
            DownloadAndInstallStatus.Error(InstallErrorReason.UNKNOWN, "No extension installer available"),
            r.terminal().status,
        )
    }

    @Test
    fun cancelSetsCancelled() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val r = repo(client = HttpClient(MockEngine {
            started.complete(Unit)
            awaitCancellation()
        }))
        r.start()
        withTimeout(10_000) { started.await() }
        r.cancelDownload(r.downloadList.first().single().id)
        assertEquals(DownloadAndInstallStatus.Cancelled, r.terminal().status)
        assertEquals(0, reloads)
    }

    @Test
    fun tempFileDeletedAfterwards() = runBlocking {
        val ok = repo()
        ok.start()
        ok.terminal()
        assertTrue(tempDir.listFiles().orEmpty().isEmpty())

        val bad = repo(stores = listOf(store(ApkInstallFailure.INVALID)))
        bad.start()
        bad.terminal()
        assertTrue(tempDir.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun cancelWhileInstallingFinishesTheInstall() = runBlocking {
        val installStarted = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val slowStore = object : ApkExtensionStore {
            override fun accepts(file: File): Boolean = true

            override suspend fun install(apk: File): String {
                installStarted.complete(Unit)
                gate.await()
                installed += apk.length()
                return "pkg"
            }

            override fun versionName(packageName: String): String? = null
        }
        val r = repo(stores = listOf(slowStore))
        r.start()
        withTimeout(10_000) { installStarted.await() }
        r.cancelDownload(r.downloadList.first().single().id)
        assertEquals(DownloadAndInstallStatus.Installing, r.downloadList.first().single().status)
        gate.complete(Unit)
        assertEquals(DownloadAndInstallStatus.Installed, r.terminal().status)
        assertEquals(listOf(body.size.toLong()), installed)
        assertEquals(1, reloads)
    }
}
