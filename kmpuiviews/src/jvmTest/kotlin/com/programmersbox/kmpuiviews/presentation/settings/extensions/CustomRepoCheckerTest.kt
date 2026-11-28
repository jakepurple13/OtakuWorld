@file:OptIn(ExperimentalCoroutinesApi::class)

package com.programmersbox.kmpuiviews.presentation.settings.extensions

import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.RepoCheck
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
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
    ) = CustomRepoChecker({ catalogs.toList() }, CoroutineScope(coroutineContext), clock, testDispatcher())

    private fun TestScope.testDispatcher() = StandardTestDispatcher(testScheduler)

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
            "  HTTPS://REPO.example/repo  ",
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
    fun repoKeyKeepsPathCase() {
        assertNotEquals(repoKey("https://repo.example/Repo"), repoKey("https://repo.example/repo"))
    }

    @Test
    fun savedRepoMatchingFindsSavedUrlWithSameKey() {
        assertEquals("https://x/repo", savedRepoMatching("https://X/repo/index.min.json", listOf("https://y", "https://x/repo")))
        assertEquals(null, savedRepoMatching("https://z", listOf("https://y")))
    }

    @Test
    fun mergePrefersNamedFoundOverBlank() {
        val blank = listOf(remote("legacy"))
        val named = listOf(remote("native"))
        val catalogs = listOf(FakeCatalog("Legacy"), FakeCatalog("Native"), FakeCatalog("Other"))
        val status = mergeRepoChecks(
            listOf(RepoCheck.Found("", blank), RepoCheck.Found("Repo", named), RepoCheck.Found("Second", named)),
            catalogs,
            now,
        )
        assertEquals(CustomRepoStatus.Found(1, "Native", "Repo", named, now), status)
    }

    @Test
    fun fallbackTitleUsesOwnerRepoForGithub() {
        assertEquals("yuzono/anime-repo", repoFallbackTitle("https://raw.githubusercontent.com/yuzono/anime-repo/repo"))
        assertEquals("keiyoushi/extensions", repoFallbackTitle("https://github.com/keiyoushi/extensions"))
        assertEquals("github.com", repoFallbackTitle("https://github.com/keiyoushi"))
        assertEquals("repo.example", repoFallbackTitle("https://repo.example/a/b/repo.json"))
    }

    @Test
    fun reemissionWithEqualNamesDoesNotRecheck() = runTest {
        val calls = mutableListOf<String>()
        fun catalogs() = listOf("A", "B").map { name ->
            FakeCatalog(name) { calls += "$name $it"; RepoCheck.NotThisFormat }
        }
        var list = catalogs()
        val checker = CustomRepoChecker({ list }, CoroutineScope(coroutineContext), { now }, testDispatcher())
        checker.sync(listOf("https://a"))
        advanceUntilIdle()
        assertEquals(listOf("A https://a", "B https://a"), calls.sorted())
        list = catalogs()
        checker.sync(listOf("https://a"))
        advanceUntilIdle()
        assertEquals(2, calls.size)
    }

    @Test
    fun orderCustomCatalogsPutsNativeFirstAndDedupesByName() {
        val native = FakeCatalog("Native")
        val legacy1 = FakeCatalog("Legacy")
        val legacy2 = FakeCatalog("Legacy")
        val dupNative = FakeCatalog("Native")
        val result = orderCustomCatalogs(native = listOf(native), installed = listOf(legacy1, dupNative, legacy2))
        assertEquals(listOf<KmpExternalCustomApiServicesCatalog>(native, legacy1), result)
    }

    @Test
    fun catalogListChangeRechecksAll() = runTest {
        var list = emptyList<KmpExternalCustomApiServicesCatalog>()
        val checker = CustomRepoChecker({ list }, CoroutineScope(coroutineContext), { now }, testDispatcher())
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
