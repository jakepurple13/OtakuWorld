package com.programmersbox.kmpuiviews.domain

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.SkippedUpdateSource
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.updatecheck.FailureKind
import com.programmersbox.kmpuiviews.domain.updatecheck.ItemFailure
import com.programmersbox.kmpuiviews.domain.updatecheck.SourceState
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckMode
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckProgress
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckResult
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdatedItem
import com.programmersbox.kmpuiviews.testing.FakeApiService
import com.programmersbox.kmpuiviews.testing.FakeExceptionDao
import com.programmersbox.kmpuiviews.testing.FakeUpdateCheckReportStore
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import com.programmersbox.kmpuiviews.testing.register
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class MediaUpdateCheckerTest {

    private lateinit var database: ItemDatabase
    private lateinit var sourceRepository: SourceRepository

    private val favorite = DbModel(
        title = "Title",
        description = "Description",
        url = "https://example.com/1",
        imageUrl = "https://example.com/1.jpg",
        source = "ExampleService",
    )

    private fun checker(store: FakeUpdateCheckReportStore = FakeUpdateCheckReportStore()): MediaUpdateChecker {
        return MediaUpdateChecker(
            dao = database.itemDao(),
            sourceRepository = sourceRepository,
            sourceLoader = SourceLoader(
                extensionsDir = { File.createTempFile("ext", "").also { it.delete(); it.mkdirs() } },
                sourceType = "test",
                sourceRepository = sourceRepository,
                appDirs = AppDirs {
                    appName = "MediaUpdateCheckerTest"
                    appAuthor = "jakepurple13"
                },
            ),
            exceptionDao = FakeExceptionDao(),
            reportStore = store,
        )
    }

    private suspend fun favorite(url: String, source: String = "FakeService", numChapters: Int = 0) =
        DbModel(
            title = "Title $url",
            description = "",
            url = url,
            imageUrl = "$url.jpg",
            source = source,
            numChapters = numChapters,
        ).also { database.itemDao().insertFavorite(it) }

    private fun storedFailure(url: String) = ItemFailure(
        url = url,
        title = "Title $url",
        imageUrl = "$url.jpg",
        source = "FakeService",
        kind = FailureKind.Http,
        httpCode = 500,
        detail = "boom",
        sourceFailed = false,
    )

    private fun storedReport(vararg failures: ItemFailure) = UpdateCheckReport(
        startedAt = 1L,
        finishedAt = 2L,
        favoriteCount = 3,
        checkedCount = 3,
        updatedCount = 0,
        sourceFailures = emptyList(),
        itemFailures = failures.toList(),
    )

    private suspend fun MediaUpdateChecker.run(
        mode: UpdateCheckMode = UpdateCheckMode.Full,
        putMetric: suspend (String, Long) -> Unit = { _, _ -> },
        onProgress: suspend (UpdateCheckProgress) -> Unit = {},
    ): UpdateCheckResult = getFavoritesThatNeedUpdates(
        checkAll = true,
        mode = mode,
        putMetric = putMetric,
        onProgress = onProgress,
        setProgress = { _, _, _ -> },
    )

    @BeforeTest
    fun setUp() {
        database = createTestItemDatabase()
        sourceRepository = SourceRepository()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `mapDbModel refreshes an existing saved notification so it sorts to the top`() = runTest {
        val dao = database.itemDao()
        dao.insertNotification(
            NotificationItem(
                id = 42,
                url = favorite.url,
                summaryText = "old summary",
                notiTitle = favorite.title,
                imageUrl = favorite.imageUrl,
                source = favorite.source,
                contentTitle = favorite.title,
                isShowing = false,
                supabaseId = "remote-id",
                createdAt = 1L,
            )
        )

        val info = FakeApiService().infoModel(favorite.url, chapters = 0)
        checker().mapDbModel(listOf(UpdatedItem(info, favorite, emptyList()))) { _, _, _ -> }

        val updated = dao.getNotificationItem(favorite.url)!!
        assertTrue(updated.createdAt > 1L, "createdAt should be bumped to now")
        assertNotEquals("old summary", updated.summaryText)
        assertTrue(updated.isShowing)
        assertEquals("remote-id", updated.supabaseId)
        assertEquals(1, dao.getAllNotifications().size)
    }

    @Test
    fun `recent list failure marks source failed and lists its favorites as not checked`() = runTest {
        val service = FakeApiService(recentError = IllegalStateException("HTTP 403 Forbidden"))
        sourceRepository.register(service)
        favorite("a")
        favorite("b")

        val report = checker().run().report!!

        assertEquals(1, report.sourceFailures.size)
        val sourceFailure = report.sourceFailures.single()
        assertEquals("FakeService", sourceFailure.source)
        assertEquals(FailureKind.Http, sourceFailure.kind)
        assertEquals(403, sourceFailure.httpCode)
        assertEquals(setOf("a", "b"), report.itemFailures.map { it.url }.toSet())
        assertTrue(report.itemFailures.all { it.sourceFailed && it.kind == FailureKind.Http && it.httpCode == 403 })
        assertTrue(service.fetchedUrls.isEmpty())
    }

    @Test
    fun `item info timeout is recorded as TimedOut item failure`() = runTest {
        sourceRepository.register(
            FakeApiService(
                recentUrls = listOf("a"),
                onItemInfo = { withTimeout(1.milliseconds) { awaitCancellation() } },
            )
        )
        favorite("a")

        val report = checker().run().report!!

        val failure = report.itemFailures.single()
        assertEquals("a", failure.url)
        assertEquals(FailureKind.TimedOut, failure.kind)
        assertEquals(false, failure.sourceFailed)
        assertTrue(report.sourceFailures.isEmpty())
    }

    @Test
    fun `progress marks failed source Failed and finished sources Done`() = runTest {
        sourceRepository.register(
            FakeApiService(serviceName = "Broken", recentError = IllegalStateException("HTTP 403 Forbidden")),
            FakeApiService(serviceName = "Working", recentUrls = listOf("ok"), onItemInfo = { infoModel(it.url, chapters = 2) }),
        )
        favorite("bad", source = "Broken")
        favorite("ok", source = "Working")
        val snapshots = mutableListOf<UpdateCheckProgress>()

        checker().run(onProgress = { snapshots += it })

        val last = snapshots.last()
        assertEquals(false, last.loadingRecents)
        assertEquals(2, last.favoriteCount)
        assertEquals(1, last.total)
        assertEquals(1, last.checked)
        assertEquals(1, last.updatesFound)
        assertEquals(0, last.itemFailures)
        val broken = last.sources.single { it.name == "Broken Display" }
        assertEquals(SourceState.Failed, broken.state)
        assertEquals(403, broken.failure?.httpCode)
        val working = last.sources.single { it.name == "Working Display" }
        assertEquals(SourceState.Done, working.state)
        assertEquals(1, working.favorites)
        assertEquals(1, working.toCheck)
        assertEquals(1, working.checked)
        assertEquals(1, working.updates)
        assertNull(working.failure)
        assertTrue(snapshots.first().loadingRecents)
    }

    @Test
    fun `skipped extension leaves out favorites from every one of its languages`() = runTest {
        val en = FakeApiService(serviceName = "dex-en")
        val ja = FakeApiService(serviceName = "dex-ja")
        val other = FakeApiService(serviceName = "asura")
        sourceRepository.addSources(
            listOf(
                KmpSourceInformation(apiService = en, name = "MangaDex (en)", icon = null, packageName = "mangadex"),
                KmpSourceInformation(apiService = ja, name = "MangaDex (ja)", icon = null, packageName = "mangadex"),
                KmpSourceInformation(apiService = other, name = "Asura", icon = null, packageName = "asura"),
            )
        )
        favorite("a", source = "dex-en")
        favorite("b", source = "dex-ja")
        favorite("c", source = "asura")
        database.itemDao().insertSkippedUpdateSources(listOf(SkippedUpdateSource("mangadex")))

        val report = checker().run().report!!

        assertEquals(1, report.favoriteCount)
        assertEquals(0, en.recentCalls.get())
        assertEquals(0, ja.recentCalls.get())
        assertEquals(1, other.recentCalls.get())
    }

    @Test
    fun `full run replaces stored report`() = runTest {
        sourceRepository.register(
            FakeApiService(recentUrls = listOf("a"), onItemInfo = { infoModel(it.url, chapters = 2) })
        )
        favorite("a")
        val store = FakeUpdateCheckReportStore(storedReport(storedFailure("old")))

        val result = checker(store).run()

        assertEquals(1, result.updates.size)
        assertEquals(2, result.updates.single().newChapters.size)
        val report = assertNotNull(result.report)
        assertEquals(1, report.favoriteCount)
        assertEquals(1, report.checkedCount)
        assertEquals(1, report.updatedCount)
        assertTrue(report.itemFailures.isEmpty())
        assertEquals(report, store.current())
    }

    @Test
    fun `cancelled run keeps previous report`() = runTest {
        val started = CompletableDeferred<Unit>()
        sourceRepository.register(
            FakeApiService(
                recentUrls = listOf("a"),
                onItemInfo = {
                    started.complete(Unit)
                    awaitCancellation()
                },
            )
        )
        favorite("a")
        val seeded = storedReport(storedFailure("old"))
        val store = FakeUpdateCheckReportStore(seeded)
        val checker = checker(store)

        val job = launch { checker.run() }
        started.await()
        job.cancelAndJoin()

        assertEquals(seeded, store.current())
    }

    @Test
    fun `crash partway saves report with stoppedEarly`() = runTest {
        sourceRepository.register(FakeApiService(recentError = IllegalStateException("HTTP 503")))
        favorite("a")
        val store = FakeUpdateCheckReportStore(storedReport(storedFailure("old")))

        val error = assertFailsWith<IllegalStateException> {
            checker(store).run(
                putMetric = { name, _ -> if (name == "updateCheckSize") throw IllegalStateException("disk full") }
            )
        }

        assertEquals("disk full", error.message)
        val saved = assertNotNull(store.current())
        assertEquals("disk full", saved.stoppedEarly)
        assertEquals(listOf("FakeService"), saved.sourceFailures.map { it.source })
        assertEquals(listOf("a"), saved.itemFailures.map { it.url })
    }

    @Test
    fun `retry only checks failed urls and merges`() = runTest {
        val service = FakeApiService(
            recentUrls = listOf("a", "b", "c"),
            onItemInfo = {
                if (it.url == "b") error("HTTP 500") else infoModel(it.url, chapters = 1)
            },
        )
        sourceRepository.register(service)
        favorite("a")
        favorite("b")
        favorite("c")
        val store = FakeUpdateCheckReportStore(storedReport(storedFailure("a"), storedFailure("b")))

        val result = checker(store).run(mode = UpdateCheckMode.RetryFailed)

        val stored = assertNotNull(store.current())
        assertEquals(listOf("b"), stored.itemFailures.map { it.url })
        assertEquals(3, stored.favoriteCount, "summary keeps the original run's context")
        assertEquals(1, stored.updatedCount)
        assertNotNull(stored.retriedAt)
        assertEquals(setOf("a", "b"), service.fetchedUrls.toSet())
        assertEquals(listOf("a"), result.updates.map { it.dbModel.url })
        assertEquals(listOf("b"), result.report?.itemFailures?.map { it.url })
    }

    @Test
    fun `retry drops failures for items no longer favorited`() = runTest {
        val service = FakeApiService(recentUrls = listOf("a", "b"), onItemInfo = { infoModel(it.url, chapters = 0) })
        sourceRepository.register(service)
        favorite("a")
        val store = FakeUpdateCheckReportStore(storedReport(storedFailure("a"), storedFailure("b")))

        checker(store).run(mode = UpdateCheckMode.RetryFailed)

        val stored = assertNotNull(store.current())
        assertTrue(stored.itemFailures.isEmpty())
        assertEquals(listOf("a"), service.fetchedUrls.toList())
    }

    @Test
    fun `retry with no stored report does nothing`() = runTest {
        val service = FakeApiService(recentUrls = listOf("a"), onItemInfo = { infoModel(it.url, chapters = 3) })
        sourceRepository.register(service)
        favorite("a")
        val store = FakeUpdateCheckReportStore()

        val result = checker(store).run(mode = UpdateCheckMode.RetryFailed)

        assertEquals(UpdateCheckResult(emptyList(), null), result)
        assertEquals(0, service.recentCalls.get())
        assertTrue(service.fetchedUrls.isEmpty())
        assertNull(store.current())
    }

    @Test
    fun `source throwing CancellationException becomes an item failure`() = runTest {
        sourceRepository.register(
            FakeApiService(recentUrls = listOf("a"), onItemInfo = { throw CancellationException("x") })
        )
        favorite("a")
        val store = FakeUpdateCheckReportStore()

        val report = assertNotNull(checker(store).run().report)

        val failure = report.itemFailures.single()
        assertEquals("a", failure.url)
        assertEquals(false, failure.sourceFailed)
        assertEquals(1, report.checkedCount)
        assertNull(report.stoppedEarly)
        assertEquals(report, store.current())
    }

    @Test
    fun `failed source favorites leave the detail to the source failure`() = runTest {
        sourceRepository.register(FakeApiService(recentError = IllegalStateException("HTTP 403 Forbidden")))
        favorite("a")

        val report = checker().run().report!!

        assertTrue(report.sourceFailures.single().detail.isNotBlank())
        assertEquals("", report.itemFailures.single().detail)
    }

    @Test
    fun `retry does not overwrite a newer report`() = runTest {
        val newer = storedReport().copy(startedAt = 5L, finishedAt = 6L)
        val store = FakeUpdateCheckReportStore(storedReport(storedFailure("a")))
        sourceRepository.register(
            FakeApiService(
                recentUrls = listOf("a"),
                onItemInfo = {
                    // A full check finishes while the retry is still running.
                    store.save(newer)
                    infoModel(it.url, chapters = 0)
                },
            )
        )
        favorite("a")

        checker(store).run(mode = UpdateCheckMode.RetryFailed)

        assertEquals(newer, store.current())
    }
}
