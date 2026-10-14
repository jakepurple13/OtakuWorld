package com.programmersbox.kmpuiviews.presentation.settings.extensions

import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.KmpSources
import com.programmersbox.kmpmodels.KmpUntrustedApiService
import com.programmersbox.kmpuiviews.repository.DownloadAndInstallState
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import com.programmersbox.kmpuiviews.utils.InstallErrorReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExtensionStatesTest {
    private fun service(url: String) = object : KmpApiService {
        override val baseUrl = url
    }

    private fun untrustedService() = object : KmpUntrustedApiService {
        override val baseUrl = ""
        override val reason = "Signing key not trusted"
        override val canTrust = true
        override suspend fun trust() {}
    }

    private fun info(pkg: String, url: String = "https://$pkg.example", service: KmpApiService = service(url)) =
        KmpSourceInformation(service, pkg, null, pkg)

    private fun remote(
        pkg: String,
        version: String = "1.4.10",
        url: String = "https://$pkg.example",
        lang: String = "en",
        nsfw: Boolean = false,
        name: String = pkg,
    ) = KmpRemoteSources(name, pkg, version, "", "https://repo/$pkg.apk", listOf(KmpSources(name, url, version)), lang, nsfw)

    private val versions = mapOf("dex" to "1.4.8", "fire" to "1.4.10")
    private val versionOf: (KmpSourceInformation) -> String = { versions[it.packageName] ?: "1.0.0" }

    @Test
    fun matchesByPackageName() {
        assertEquals("dex", remote("dex", url = "https://other").findInstalled(listOf(info("dex")))?.packageName)
    }

    @Test
    fun fallsBackToBaseUrl() {
        val installed = info("legacy.pkg", url = "https://dex.example")
        assertEquals(installed, remote("dex").findInstalled(listOf(installed)))
        assertEquals(remote("dex"), installed.findRemote(listOf(remote("dex"))))
    }

    @Test
    fun blankKeysNeverMatch() {
        val placeholder = KmpSourceInformation(service(""), "MangaWorld", null, "")
        val blankRemote = remote("").copy(sources = listOf(KmpSources("x", "", "1")))
        assertNull(blankRemote.findInstalled(listOf(placeholder)))
        assertNull(placeholder.findRemote(listOf(blankRemote)))
    }

    @Test
    fun resolvesUpdateInstalledAndNotInstalled() {
        val installed = listOf(info("dex"), info("fire"))
        val dex = resolveState(remote("dex"), installed, versionOf, emptyList())
        assertEquals("1.4.8", assertIs<RemoteItemState.UpdateAvailable>(dex).installedVersion)
        assertIs<RemoteItemState.Installed>(resolveState(remote("fire"), installed, versionOf, emptyList()))
        assertEquals(RemoteItemState.NotInstalled, resolveState(remote("new"), installed, versionOf, emptyList()))
    }

    @Test
    fun installingTakesPrecedence() {
        val downloads = listOf(DownloadAndInstallState("https://repo/dex.apk", "dex", "1", DownloadAndInstallStatus.Downloading(0.4f)))
        val state = resolveState(remote("dex"), listOf(info("dex")), versionOf, downloads)
        assertEquals(RemoteItemState.Installing(DownloadAndInstallStatus.Downloading(0.4f)), state)
    }

    @Test
    fun errorStaysVisibleInstalledDoesNot() {
        val err = DownloadAndInstallStatus.Error(InstallErrorReason.GENERIC, "boom")
        assertEquals(err, listOf(DownloadAndInstallState("u", "n", "1", err)).statusFor("u"))
        assertNull(listOf(DownloadAndInstallState("u", "n", "1", DownloadAndInstallStatus.Installed)).statusFor("u"))
        assertNull(listOf(DownloadAndInstallState("u", "n", "1", DownloadAndInstallStatus.Cancelled)).statusFor("u"))
        // latest entry for a url wins
        val list = listOf(
            DownloadAndInstallState("u", "n", "1", err),
            DownloadAndInstallState("u", "n", "2", DownloadAndInstallStatus.Installing),
        )
        assertEquals(DownloadAndInstallStatus.Installing, list.statusFor("u"))
    }

    @Test
    fun summaryAndFilters() {
        val untrusted = KmpSourceInformation(untrustedService(), "asura", null, "asura")
        val items = installedItems(
            listOf(info("dex"), info("dex"), info("fire"), untrusted),
            listOf(remote("dex"), remote("fire")),
            versionOf,
            emptyList(),
        )
        assertEquals(InstalledSummary(installed = 3, updates = 1, untrusted = 1), items.summary())
        assertEquals(listOf("dex"), items.filteredBy(InstalledFilter.Updates).map { it.info.packageName })
        assertEquals(listOf("asura"), items.filteredBy(InstalledFilter.Untrusted).map { it.info.packageName })
        assertEquals(3, items.filteredBy(InstalledFilter.None).size)
    }

    @Test
    fun filterToggle() {
        assertEquals(InstalledFilter.Updates, InstalledFilter.None.toggle(InstalledFilter.Updates))
        assertEquals(InstalledFilter.None, InstalledFilter.Updates.toggle(InstalledFilter.Updates))
        assertEquals(InstalledFilter.Untrusted, InstalledFilter.Updates.toggle(InstalledFilter.Untrusted))
        assertEquals(InstalledFilter.None, InstalledFilter.Untrusted.toggle(InstalledFilter.None))
    }

    @Test
    fun updateLinksOnePerUpdate() {
        val items = installedItems(listOf(info("dex"), info("fire")), listOf(remote("dex"), remote("fire")), versionOf, emptyList())
        assertEquals(listOf("https://repo/dex.apk"), items.updateLinks())
    }

    @Test
    fun updateLinksSkipInstalling() {
        val downloads = listOf(DownloadAndInstallState("https://repo/dex.apk", "dex", "1", DownloadAndInstallStatus.Installing))
        val items = installedItems(listOf(info("dex")), listOf(remote("dex")), versionOf, downloads)
        assertEquals(emptyList(), items.updateLinks())
        assertEquals(DownloadAndInstallStatus.Installing, items.single().installing)
    }

    @Test
    fun duplicatePackagesInCatalogKeepHighestVersion() {
        val ui = catalogUi(
            mapOf("c" to RemoteViewState(listOf(remote("dex", version = "1.4.9"), remote("dex", version = "1.4.12"), remote("fire")))),
            emptyList(), versionOf, emptyList(),
        )
        val items = assertIs<CatalogUi.Loaded>(ui["c"]).items
        assertEquals(items.size, items.map { it.key }.distinct().size)
        assertEquals("1.4.12", items.single { it.remote.packageName == "dex" }.remote.version)
    }

    @Test
    fun blankUrlNeverMatchesDownloads() {
        val list = listOf(DownloadAndInstallState("", "", "1", DownloadAndInstallStatus.Error(InstallErrorReason.UNKNOWN, "x")))
        assertNull(list.statusFor(""))
    }

    @Test
    fun requestedLinkShowsAsQueuedUntilListReportsIt() {
        val requested = setOf("https://repo/dex.apk")
        // queued WorkInfos carry no url, so only the requested set knows about it
        val queued = listOf(DownloadAndInstallState("", "", "1", DownloadAndInstallStatus.Error(InstallErrorReason.UNKNOWN, "x")))
        assertIs<RemoteItemState.Installing>(resolveState(remote("dex"), listOf(info("dex")), versionOf, queued, requested))
        assertIs<RemoteItemState.Installing>(resolveState(remote("new"), emptyList(), versionOf, emptyList(), setOf("https://repo/new.apk")))
        // once installed and up to date, a stale request no longer holds it
        assertIs<RemoteItemState.Installed>(resolveState(remote("fire"), listOf(info("fire")), versionOf, emptyList(), setOf("https://repo/fire.apk")))
        val items = installedItems(listOf(info("dex")), listOf(remote("dex")), versionOf, queued, requested)
        assertEquals(emptyList(), items.updateLinks())
    }

    @Test
    fun pendingDropsTerminalLinks() {
        val requested = setOf("a", "b", "c", "d")
        val downloads = listOf(
            DownloadAndInstallState("a", "", "1", DownloadAndInstallStatus.Installed),
            DownloadAndInstallState("b", "", "2", DownloadAndInstallStatus.Cancelled),
            DownloadAndInstallState("c", "", "3", DownloadAndInstallStatus.Error(InstallErrorReason.GENERIC, "x")),
            DownloadAndInstallState("d", "", "4", DownloadAndInstallStatus.Downloading(0.5f)),
        )
        assertEquals(setOf("d"), pendingAfter(requested, downloads))
    }

    @Test
    fun failedUpdateCanBeRetriedFromUpdateAll() {
        val err = DownloadAndInstallStatus.Error(InstallErrorReason.GENERIC, "boom")
        val downloads = listOf(DownloadAndInstallState("https://repo/dex.apk", "dex", "1", err))
        val items = installedItems(listOf(info("dex")), listOf(remote("dex")), versionOf, downloads)
        assertEquals(listOf("https://repo/dex.apk"), items.updateLinks())
    }

    @Test
    fun canUninstallAllOnlyForSinglePackage() {
        assertTrue(listOf(info("a"), info("a")).canUninstallAll())
        assertFalse(listOf(info("a"), info("b")).canUninstallAll())
    }

    @Test
    fun shelfOrderRanksThenName() {
        val items = listOf(
            RemoteItem(remote("b"), "c", RemoteItemState.Installed(info("b"))),
            RemoteItem(remote("Zeta"), "c", RemoteItemState.NotInstalled),
            RemoteItem(remote("alpha"), "c", RemoteItemState.NotInstalled),
            RemoteItem(remote("i"), "c", RemoteItemState.Installing(DownloadAndInstallStatus.Installing)),
            RemoteItem(remote("u"), "c", RemoteItemState.UpdateAvailable(info("u"), "1.0.0")),
        )
        assertEquals(listOf("u", "i", "alpha", "Zeta", "b"), items.shelfOrder().map { it.remote.name })
    }

    @Test
    fun searchTrimsAndIgnoresCase() {
        val items = listOf(RemoteItem(remote("x", name = "MangaDex"), "c", RemoteItemState.NotInstalled))
        assertEquals(1, items.search(" dex ").size)
        assertEquals(1, items.search("").size)
        assertEquals(0, items.search("fire").size)
    }

    @Test
    fun languagesAndCatalogFilter() {
        val items = listOf(
            RemoteItem(remote("a", lang = "es"), "c", RemoteItemState.NotInstalled),
            RemoteItem(remote("b", lang = "all", nsfw = true), "c", RemoteItemState.NotInstalled),
            RemoteItem(remote("c", lang = "en"), "c", RemoteItemState.NotInstalled),
            RemoteItem(remote("d", lang = ""), "c", RemoteItemState.NotInstalled),
        )
        assertEquals(listOf("all", "en", "es"), items.languages())
        assertEquals(listOf("a"), items.filterCatalog("es", hideNsfw = false).map { it.remote.packageName })
        assertEquals(listOf("a", "c", "d"), items.filterCatalog(null, hideNsfw = true).map { it.remote.packageName })
        assertEquals("Multi", displayLang("all"))
        assertEquals("en", displayLang("en"))
    }

    @Test
    fun catalogUiMapsStates() {
        val ui = catalogUi(
            mapOf(
                "ok" to RemoteViewState(listOf(remote("dex"))),
                "bad" to RemoteErrorState("404"),
                "wait" to RemoteLoadingState,
            ),
            listOf(info("dex")), versionOf, emptyList(),
        )
        val loaded = assertIs<CatalogUi.Loaded>(ui["ok"])
        assertEquals("ok", loaded.items.single().catalog)
        assertIs<RemoteItemState.UpdateAvailable>(loaded.items.single().state)
        assertEquals(CatalogUi.Error("404"), ui["bad"])
        assertEquals(CatalogUi.Loading, ui["wait"])
    }
}
