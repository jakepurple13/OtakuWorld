package com.programmersbox.kmpuiviews.presentation.settings.extensions

import com.programmersbox.kmpmodels.ExampleService
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import com.programmersbox.kmpuiviews.utils.InstallErrorReason
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.coroutines.cancellation.CancellationException

private class FakeCatalog(
    override val name: String,
    private val claims: Boolean,
    private val calls: MutableList<String>,
    private val failure: Throwable? = null,
) : KmpExternalCustomApiServicesCatalog {
    override suspend fun initialize() = Unit
    override fun getSources(): List<KmpSourceInformation> = emptyList()
    override val hasRemoteSources: Boolean = true
    override suspend fun install(remote: KmpRemoteSources): Boolean {
        calls += name
        failure?.let { throw it }
        return claims
    }
}

private fun remote(link: String, version: String = "1.0") = KmpRemoteSources(
    name = link,
    packageName = "pkg.$link",
    version = version,
    iconUrl = "",
    downloadLink = link,
    sources = emptyList(),
)

private fun info(pkg: String) = KmpSourceInformation(
    apiService = ExampleService(),
    name = pkg,
    icon = null,
    packageName = pkg,
)

class CatalogInstallTest {

    @Test
    fun firstClaimingCatalogWins() = runTest {
        val calls = mutableListOf<String>()
        val catalogs = listOf(
            FakeCatalog("a", false, calls),
            FakeCatalog("b", true, calls),
            FakeCatalog("c", true, calls),
        )

        assertTrue(installViaCatalogs(catalogs, remote("x")))
        assertEquals(listOf("a", "b"), calls)
    }

    @Test
    fun noCatalogClaims_returnsFalse() = runTest {
        val calls = mutableListOf<String>()
        val catalogs = listOf(FakeCatalog("a", false, calls), FakeCatalog("b", false, calls))

        assertFalse(installViaCatalogs(catalogs, remote("x")))
        assertEquals(listOf("a", "b"), calls)
    }

    @Test
    fun throwingCatalog_countsAsClaimed() = runTest {
        val calls = mutableListOf<String>()
        val catalogs = listOf(
            FakeCatalog("a", false, calls, failure = IllegalStateException("boom")),
            FakeCatalog("b", true, calls),
        )

        assertTrue(installViaCatalogs(catalogs, remote("x")))
        assertEquals(listOf("a"), calls)
    }

    @Test
    fun cancellationIsRethrown() = runTest {
        val calls = mutableListOf<String>()
        val catalogs = listOf(FakeCatalog("a", false, calls, failure = CancellationException("cancelled")))

        assertFailsWith<CancellationException> { installViaCatalogs(catalogs, remote("x")) }
    }

    @Test
    fun installAll_installsEveryRemoteAndSendsUnclaimedToFallback() = runTest {
        val catalog = object : KmpExternalCustomApiServicesCatalog {
            val installed = mutableListOf<String>()
            override val name = "c"
            override val hasRemoteSources = true
            override suspend fun initialize() = Unit
            override fun getSources(): List<KmpSourceInformation> = emptyList()
            override suspend fun install(remote: KmpRemoteSources): Boolean =
                (remote.downloadLink != "apk").also { if (it) installed += remote.downloadLink }
        }
        val fallback = mutableListOf<String>()

        val claimed = installAllViaCatalogs(listOf(catalog), listOf(remote("x"), remote("apk"), remote("y"))) {
            fallback += it.downloadLink
        }

        assertTrue(claimed)
        assertEquals(listOf("x", "y"), catalog.installed)
        assertEquals(listOf("apk"), fallback)
    }

    @Test
    fun installAll_noneClaimed_returnsFalse() = runTest {
        val fallback = mutableListOf<String>()

        val claimed = installAllViaCatalogs(listOf(FakeCatalog("a", false, mutableListOf())), listOf(remote("x"))) {
            fallback += it.downloadLink
        }

        assertFalse(claimed)
        assertEquals(listOf("x"), fallback)
    }

    @Test
    fun logFailures_swallowsExceptions() = runTest {
        logFailures { throw IllegalStateException("reload failed") }
    }

    @Test
    fun logFailures_rethrowsCancellation() = runTest {
        assertFailsWith<CancellationException> { logFailures { throw CancellationException("cancelled") } }
    }

    @Test
    fun updateRemotes_matchesUpdateLinks() {
        val items = listOf(
            InstalledItem(info("one"), "1.0", remote("l1", "2.0"), null),
            InstalledItem(info("two"), "1.0", null, null),
            InstalledItem(info("three"), "1.0", remote("l3", "2.0"), DownloadAndInstallStatus.Downloading(0.5f)),
            InstalledItem(info("four"), "1.0", remote("l4", "2.0"), DownloadAndInstallStatus.Error(InstallErrorReason.GENERIC, "x")),
        )

        assertEquals(items.updateLinks(), items.updateRemotes().map { it.downloadLink })
        assertEquals(listOf("l1", "l4"), items.updateRemotes().map { it.downloadLink })
    }
}
