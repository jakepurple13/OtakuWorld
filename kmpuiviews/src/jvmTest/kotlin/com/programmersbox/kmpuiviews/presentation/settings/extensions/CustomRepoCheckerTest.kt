@file:OptIn(ExperimentalCoroutinesApi::class)

package com.programmersbox.kmpuiviews.presentation.settings.extensions

import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.RepoCheck
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

class CustomRepoCheckerTest {
    private val now = Instant.fromEpochSeconds(1_000)
    private val later = Instant.fromEpochSeconds(2_000)

    private fun remote(pkg: String) = KmpRemoteSources(pkg, pkg, "1.0.0", "", "", emptyList())

    private class FakeCatalog(
        override val name: String,
        private val answer: suspend (String) -> RepoCheck = { RepoCheck.NotThisFormat },
    ) : KmpExternalCustomApiServicesCatalog {
        val calls = mutableListOf<String>()
        override suspend fun initialize() = Unit
        override fun getSources(): List<KmpSourceInformation> = emptyList()
        override val hasRemoteSources = true
        override suspend fun checkRepo(url: String): RepoCheck {
            calls += url
            return answer(url)
        }
    }

    private fun TestScope.checker(
        vararg catalogs: KmpExternalCustomApiServicesCatalog,
        clock: () -> Instant = { now },
    ) = CustomRepoChecker({ catalogs.toList() }, CoroutineScope(coroutineContext), clock)

    @Test
    fun mergeFoundBeatsFailedAndUnsupported() {
        val l = listOf(remote("p"))
        val catalogs = listOf(FakeCatalog("A"), FakeCatalog("B"), FakeCatalog("C"))
        val status = mergeRepoChecks(
            listOf(RepoCheck.NotThisFormat, RepoCheck.Failed("x"), RepoCheck.Found("R", l)),
            catalogs,
            now,
        )
        assertEquals(CustomRepoStatus.Found(2, "C", "R", l, now), status)
    }

    @Test
    fun mergeFailedBeatsUnsupported() {
        val catalogs = listOf(FakeCatalog("A"), FakeCatalog("B"))
        val status = mergeRepoChecks(listOf(RepoCheck.NotThisFormat, RepoCheck.Failed("timeout")), catalogs, now)
        assertEquals(CustomRepoStatus.Failed("timeout", now), status)
    }

    @Test
    fun mergeBlankFailedMessageBecomesUnknownError() {
        val status = mergeRepoChecks(listOf(RepoCheck.Failed("  ")), listOf(FakeCatalog("A")), now)
        assertEquals(CustomRepoStatus.Failed("Unknown error", now), status)
    }

    @Test
    fun mergeAllNotThisFormatIsUnsupported() {
        val catalogs = listOf(FakeCatalog("A"), FakeCatalog("B"))
        val status = mergeRepoChecks(listOf(RepoCheck.NotThisFormat, RepoCheck.NotThisFormat), catalogs, now)
        assertEquals(CustomRepoStatus.Unsupported(now), status)
    }

    @Test
    fun mergeFoundBlankNameFallsBackToHost() {
        val status = mergeRepoChecks(
            listOf(RepoCheck.Found("", listOf(remote("p")))),
            listOf(FakeCatalog("A")),
            now,
            url = "https://repo.example/x",
        )
        assertEquals("repo.example", assertIs<CustomRepoStatus.Found>(status).repoName)
    }

    @Test
    fun repoKeyMatchesVariants() {
        val keys = listOf(
            "https://repo.example/repo",
            "https://repo.example/repo/",
            "https://repo.example/repo/index.min.json",
            "https://repo.example/repo/repo.json",
            "  https://repo.example/REPO  ",
        ).map(::repoKey)
        assertEquals(1, keys.toSet().size, keys.toString())
    }

    @Test
    fun syncChecksNewAndDropsRemoved() = runTest {
        val catalog = FakeCatalog("A") { RepoCheck.Found("R", listOf(remote("p"))) }
        val checker = checker(catalog)
        checker.sync(listOf("https://a", "https://b"))
        advanceUntilIdle()
        assertIs<CustomRepoStatus.Found>(checker.statuses["https://a"])
        assertIs<CustomRepoStatus.Found>(checker.statuses["https://b"])
        checker.sync(listOf("https://b"))
        advanceUntilIdle()
        assertEquals(setOf("https://b"), checker.statuses.keys)
        assertEquals(listOf("https://a", "https://b"), catalog.calls.sorted())
    }

    @Test
    fun removedDuringCheckIsDropped() = runTest {
        val gate = CompletableDeferred<RepoCheck>()
        val checker = checker(FakeCatalog("A") { gate.await() })
        checker.sync(listOf("https://a"))
        advanceUntilIdle()
        assertEquals(CustomRepoStatus.Checking, checker.statuses["https://a"])
        checker.sync(emptyList())
        gate.complete(RepoCheck.Found("R", listOf(remote("p"))))
        advanceUntilIdle()
        assertEquals(emptyMap(), checker.statuses.toMap())
    }

    @Test
    fun recheckGoesThroughChecking() = runTest {
        var gate = CompletableDeferred<RepoCheck>()
        var t = now
        val checker = checker(FakeCatalog("A") { gate.await() }, clock = { t })
        gate.complete(RepoCheck.NotThisFormat)
        checker.sync(listOf("https://a"))
        advanceUntilIdle()
        assertEquals(CustomRepoStatus.Unsupported(now), checker.statuses["https://a"])

        gate = CompletableDeferred()
        t = later
        checker.recheck("https://a")
        advanceUntilIdle()
        assertEquals(CustomRepoStatus.Checking, checker.statuses["https://a"])
        gate.complete(RepoCheck.Failed("down"))
        advanceUntilIdle()
        assertEquals(CustomRepoStatus.Failed("down", later), checker.statuses["https://a"])
    }

    @Test
    fun existingFindsSavedUrlWithSameKey() = runTest {
        val checker = checker()
        assertEquals("https://x/repo", checker.existing("https://x/REPO/index.min.json", listOf("https://y", "https://x/repo")))
        assertEquals(null, checker.existing("https://z", listOf("https://y")))
    }

    @Test
    fun catalogListChangeRechecksAll() = runTest {
        var list = emptyList<KmpExternalCustomApiServicesCatalog>()
        val checker = CustomRepoChecker({ list }, CoroutineScope(coroutineContext)) { now }
        checker.sync(listOf("https://a"))
        advanceUntilIdle()
        assertEquals(CustomRepoStatus.Unsupported(now), checker.statuses["https://a"])
        list = listOf(FakeCatalog("A") { RepoCheck.Found("R", listOf(remote("p"))) })
        checker.sync(listOf("https://a"))
        advanceUntilIdle()
        assertIs<CustomRepoStatus.Found>(checker.statuses["https://a"])
    }

    @Test
    fun httpLinkFailsWithoutAskingCatalogs() = runTest {
        val catalog = FakeCatalog("A") { RepoCheck.Found("R", listOf(remote("p"))) }
        val checker = checker(catalog)
        checker.sync(listOf("http://a", "  HTTP://b"))
        advanceUntilIdle()
        assertEquals(CustomRepoStatus.Failed("Only https links are supported", now), checker.statuses["http://a"])
        assertEquals(CustomRepoStatus.Failed("Only https links are supported", now), checker.statuses["  HTTP://b"])
        assertEquals(emptyList(), catalog.calls)
    }

    @Test
    fun throwingCatalogBecomesFailed() = runTest {
        val checker = checker(FakeCatalog("A") { error("boom") })
        checker.sync(listOf("https://a"))
        advanceUntilIdle()
        assertEquals(CustomRepoStatus.Failed("boom", now), checker.statuses["https://a"])
    }
}
