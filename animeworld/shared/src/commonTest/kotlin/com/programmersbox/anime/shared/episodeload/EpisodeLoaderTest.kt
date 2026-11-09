package com.programmersbox.anime.shared.episodeload

import androidx.compose.runtime.snapshotFlow
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeLoaderTest {

    private class FakeSource(
        var storages: List<KmpStorage>,
        var resolveError: Exception? = null,
        var resolveLink: ((KmpStorage) -> String?)? = null,
        var gate: CompletableDeferred<Unit>? = null,
        var onResolve: () -> Unit = {},
    ) : KmpApiService {
        override val baseUrl = "http://test"

        // The default flow hops to Dispatchers.IO, which escapes the test scheduler's virtual time.
        override fun getChapterInfoFlow(chapterModel: KmpChapterModel): Flow<List<KmpStorage>> = flow {
            gate?.await()
            emit(storages)
        }

        override suspend fun resolve(storage: KmpStorage): KmpStorage {
            onResolve()
            resolveError?.let { throw it }
            val link = resolveLink?.invoke(storage) ?: (storage.link + "?resolved")
            return storage.copy(link = link).also { it.headers.putAll(storage.headers) }
        }
    }

    private class Resolved(val action: EpisodeAction, val storage: KmpStorage)

    private class Harness(val source: FakeSource, handoffScope: CoroutineScope) {
        val resolved = mutableListOf<Resolved>()
        val stateAtResolve = mutableListOf<EpisodeLoadState?>()
        val failures = mutableListOf<Pair<KmpStorage?, Throwable?>>()
        val steps = mutableListOf<EpisodeLoadStep>()
        lateinit var loader: EpisodeLoader

        init {
            // snapshotFlow can miss steps that change within one dispatch, so also capture at resolve time.
            source.onResolve = { loader.state?.step?.let(steps::add) }
            loader = EpisodeLoader(handoffScope)
        }

        suspend fun load(action: EpisodeAction, chapter: KmpChapterModel = chapter(), info: KmpInfoModel = info()) =
            loader.load(
                action, chapter, info,
                onResolved = { storage ->
                    stateAtResolve += loader.state
                    resolved += Resolved(action, storage)
                },
                onDownloadFailed = { storage, e -> failures += storage to e },
            )

        fun chapter(name: String = "Ep 1", url: String = "u") = KmpChapterModel(name, url, "", url, source)
        fun info() = KmpInfoModel("Show", "", "u", "http://img", emptyList(), emptyList(), emptyList(), source)
        fun step() = loader.state?.step
    }

    /** The hand-off runs on the test scheduler, so its timeout uses virtual time. */
    private fun TestScope.harness(source: FakeSource) = Harness(source, backgroundScope)

    private fun storage(quality: String) = KmpStorage(link = "http://v/$quality", quality = quality, source = "src")

    private fun TestScope.record(h: Harness) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            snapshotFlow { h.loader.state?.step }.collect { it?.let(h.steps::add) }
        }
    }

    private fun TestScope.startLoad(h: Harness, action: EpisodeAction, chapter: KmpChapterModel = h.chapter()): Job =
        launch { h.load(action, chapter) }

    @Test
    fun singleSourcePlaysWithoutSheet() = runTest {
        val h = harness(FakeSource(listOf(storage("720p"))))
        record(h)
        h.load(EpisodeAction.Play, h.chapter())
        advanceUntilIdle()
        assertEquals(1, h.resolved.size)
        assertEquals(EpisodeAction.Play, h.resolved[0].action)
        assertEquals("http://v/720p?resolved", h.resolved[0].storage.link)
        assertTrue(h.steps.none { it is EpisodeLoadStep.ChooseSource })
        assertEquals(EpisodeLoadStep.OpeningPlayer, h.step())
    }

    @Test
    fun playEntersOpeningPlayerBeforeOnResolved() = runTest {
        val h = harness(FakeSource(listOf(storage("720p"))))
        val job = startLoad(h, EpisodeAction.Play)
        runCurrent()
        assertEquals(listOf<EpisodeLoadStep?>(EpisodeLoadStep.OpeningPlayer), h.stateAtResolve.map { it?.step })
        h.loader.finishHandoff()
        advanceUntilIdle()
        job.join()
    }

    @Test
    fun playHoldsStateUntilHandoff() = runTest {
        val h = harness(FakeSource(listOf(storage("720p"))))
        val job = startLoad(h, EpisodeAction.Play)
        runCurrent()
        assertEquals(1, h.resolved.size)
        assertEquals(EpisodeLoadStep.OpeningPlayer, h.step())
        // load() returns to the caller right after onResolved; the hand-off is held by the loader.
        assertTrue(job.isCompleted)
        h.load(EpisodeAction.Play, h.chapter("Ep 2", "u2"))
        assertEquals(1, h.resolved.size)
        h.loader.finishHandoff()
        assertNull(h.loader.state)
        runCurrent()
        h.load(EpisodeAction.Download, h.chapter())
        assertEquals(2, h.resolved.size)
    }

    @Test
    fun callerCancelledDuringHandoffKeepsStateUntilFinish() = runTest {
        val h = harness(FakeSource(listOf(storage("720p"))))
        // Stands in for the details screen's scope, which goes away when the activity rotates for the player.
        val caller = Job()
        var afterLoad = false
        launch(caller) {
            h.load(EpisodeAction.Play)
            afterLoad = true
        }
        runCurrent()
        assertTrue(afterLoad)
        assertEquals(EpisodeLoadStep.OpeningPlayer, h.step())
        caller.cancel()
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(EpisodeLoadStep.OpeningPlayer, h.step())
        h.loader.finishHandoff()
        assertNull(h.loader.state)
        runCurrent()
        h.load(EpisodeAction.Download, h.chapter())
        assertEquals(2, h.resolved.size)
    }

    @Test
    fun handoffTimesOut() = runTest {
        val h = harness(FakeSource(listOf(storage("720p"))))
        val job = startLoad(h, EpisodeAction.Play)
        runCurrent()
        assertEquals(EpisodeLoadStep.OpeningPlayer, h.step())
        advanceTimeBy(9_999)
        runCurrent()
        assertEquals(EpisodeLoadStep.OpeningPlayer, h.step())
        advanceTimeBy(2)
        runCurrent()
        assertNull(h.loader.state)
        assertTrue(job.isCompleted)
    }

    @Test
    fun cancelDuringHandoffClearsWithoutCancellingResolved() = runTest {
        val h = harness(FakeSource(listOf(storage("720p"))))
        var cancelled = false
        var completed = false
        val gate = CompletableDeferred<Unit>()
        val job = launch {
            h.loader.load(EpisodeAction.Play, h.chapter(), h.info(), onResolved = {
                try {
                    gate.await()
                    completed = true
                } catch (e: kotlinx.coroutines.CancellationException) {
                    cancelled = true
                    throw e
                }
            })
        }
        runCurrent()
        assertEquals(EpisodeLoadStep.OpeningPlayer, h.step())
        h.loader.decide(EpisodeLoadDecision.Cancel)
        assertNull(h.loader.state)
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(completed)
        assertFalse(cancelled)
        assertTrue(job.isCompleted)
        h.load(EpisodeAction.Play, h.chapter())
        assertEquals(1, h.resolved.size)
    }

    @Test
    fun downloadDoesNotHandoff() = runTest {
        val h = harness(FakeSource(listOf(storage("720p"))))
        h.load(EpisodeAction.Download, h.chapter())
        assertEquals(listOf<EpisodeLoadState?>(null), h.stateAtResolve)
        assertNull(h.loader.state)
    }

    @Test
    fun multipleSourcesWaitForPick() = runTest {
        val sources = listOf(storage("720p"), storage("1080p"))
        val h = harness(FakeSource(sources))
        record(h)
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        val step = assertIs<EpisodeLoadStep.ChooseSource>(h.step())
        assertEquals(sources, step.sources)
        h.loader.decide(EpisodeLoadDecision.Pick(sources[1]))
        advanceUntilIdle()
        job.join()
        assertEquals("http://v/1080p?resolved", h.resolved.single().storage.link)
        assertTrue(EpisodeLoadStep.Resolving("src 1080p") in h.steps)
    }

    @Test
    fun dismissingSheetEndsLoad() = runTest {
        val h = harness(FakeSource(listOf(storage("720p"), storage("1080p"))))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        assertIs<EpisodeLoadStep.ChooseSource>(h.step())
        h.loader.decide(EpisodeLoadDecision.Cancel)
        advanceUntilIdle()
        assertTrue(job.isCompleted)
        assertNull(h.loader.state)
        assertTrue(h.resolved.isEmpty())
    }

    @Test
    fun cancelClearsStateAndAllowsNextLoad() = runTest {
        val gate = CompletableDeferred<Unit>()
        val h = harness(FakeSource(listOf(storage("720p")), gate = gate))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        assertIs<EpisodeLoadStep.FetchingSources>(h.step())
        job.cancel()
        advanceUntilIdle()
        assertNull(h.loader.state)
        gate.complete(Unit)
        h.load(EpisodeAction.Play, h.chapter())
        assertEquals(1, h.resolved.size)
    }

    @Test
    fun cancelDuringFetchStopsLoad() = runTest {
        val gate = CompletableDeferred<Unit>()
        val h = harness(FakeSource(listOf(storage("720p")), gate = gate))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        assertIs<EpisodeLoadStep.FetchingSources>(h.step())
        h.loader.decide(EpisodeLoadDecision.Cancel)
        advanceUntilIdle()
        assertTrue(job.isCompleted)
        assertFalse(job.isCancelled)
        assertNull(h.loader.state)
        assertTrue(h.resolved.isEmpty())
        gate.complete(Unit)
        h.load(EpisodeAction.Play, h.chapter())
        assertEquals(1, h.resolved.size)
    }

    @Test
    fun cancelDuringResolveStopsLoad() = runTest {
        val resolveGate = CompletableDeferred<Unit>()
        val h = harness(FakeSource(listOf(storage("720p"))))
        val blocking = object : KmpApiService {
            override val baseUrl = "http://test"
            override fun getChapterInfoFlow(chapterModel: KmpChapterModel): Flow<List<KmpStorage>> =
                flow { emit(listOf(storage("720p"))) }

            override suspend fun resolve(storage: KmpStorage): KmpStorage {
                resolveGate.await()
                return storage
            }
        }
        val chapter = KmpChapterModel("Ep 1", "u", "", "u", blocking)
        val second = launch { h.loader.load(EpisodeAction.Play, chapter, h.info(), onResolved = { h.resolved += Resolved(EpisodeAction.Play, it) }) }
        advanceUntilIdle()
        assertIs<EpisodeLoadStep.Resolving>(h.step())
        h.loader.decide(EpisodeLoadDecision.Cancel)
        advanceUntilIdle()
        assertTrue(second.isCompleted)
        assertFalse(second.isCancelled)
        assertNull(h.loader.state)
        assertTrue(h.resolved.isEmpty())
        h.load(EpisodeAction.Play, h.chapter())
        assertEquals(1, h.resolved.size)
    }

    @Test
    fun tapWhileLoadingIsIgnored() = runTest {
        val h = harness(FakeSource(listOf(storage("720p"), storage("1080p"))))
        val job = startLoad(h, EpisodeAction.Play, h.chapter("Ep 1", "u1"))
        advanceUntilIdle()
        h.load(EpisodeAction.Play, h.chapter("Ep 2", "u2"))
        assertEquals("u1", h.loader.state?.episodeUrl)
        assertIs<EpisodeLoadStep.ChooseSource>(h.step())
        job.cancel()
    }

    @Test
    fun playResolveFailureShowsError() = runTest {
        val h = harness(FakeSource(listOf(storage("720p")), resolveError = IllegalStateException("boom")))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        assertEquals(EpisodeLoadStep.Failed("Couldn't load Ep 1", "boom", false), h.step())
        h.source.resolveError = null
        h.loader.decide(EpisodeLoadDecision.Retry)
        advanceUntilIdle()
        job.join()
        assertEquals(1, h.resolved.size)
        assertEquals(EpisodeLoadStep.OpeningPlayer, h.step())
    }

    @Test
    fun pickOtherReopensSheet() = runTest {
        val sources = listOf(storage("720p"), storage("1080p"))
        val h = harness(FakeSource(sources, resolveError = IllegalStateException("boom")))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        h.loader.decide(EpisodeLoadDecision.Pick(sources[0]))
        advanceUntilIdle()
        assertTrue(assertIs<EpisodeLoadStep.Failed>(h.step()).canPickOther)
        h.loader.decide(EpisodeLoadDecision.PickOther)
        advanceUntilIdle()
        assertEquals(sources, assertIs<EpisodeLoadStep.ChooseSource>(h.step()).sources)
        job.cancel()
    }

    @Test
    fun singleSourceFailureCannotPickOther() = runTest {
        val h = harness(FakeSource(listOf(storage("720p")), resolveError = IllegalStateException("boom")))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        assertFalse(assertIs<EpisodeLoadStep.Failed>(h.step()).canPickOther)
        job.cancel()
    }

    @Test
    fun playNoSourcesShowsError() = runTest {
        val h = harness(FakeSource(emptyList()))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        assertEquals(EpisodeLoadStep.Failed("No sources found for Ep 1", null, false), h.step())
        h.loader.decide(EpisodeLoadDecision.Cancel)
        advanceUntilIdle()
        assertTrue(job.isCompleted)
        assertNull(h.loader.state)
    }

    @Test
    fun playNoSourcesRetryRefetches() = runTest {
        val h = harness(FakeSource(emptyList()))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        assertIs<EpisodeLoadStep.Failed>(h.step())
        h.source.storages = listOf(storage("720p"))
        h.loader.decide(EpisodeLoadDecision.Retry)
        runCurrent()
        assertTrue(job.isCompleted)
        assertEquals("http://v/720p?resolved", h.resolved.single().storage.link)
        assertEquals(EpisodeLoadStep.OpeningPlayer, h.step())
    }

    @Test
    fun strayPickOtherDuringChooseSourceIsIgnored() = runTest {
        val sources = listOf(storage("720p"), storage("1080p"))
        val h = harness(FakeSource(sources, resolveError = IllegalStateException("boom")))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        h.loader.decide(EpisodeLoadDecision.Pick(sources[0]))
        advanceUntilIdle()
        assertIs<EpisodeLoadStep.Failed>(h.step())
        h.loader.decide(EpisodeLoadDecision.PickOther)
        advanceUntilIdle()
        assertIs<EpisodeLoadStep.ChooseSource>(h.step())
        // Second tap on the fading-out "Try another source".
        h.loader.decide(EpisodeLoadDecision.PickOther)
        advanceUntilIdle()
        assertIs<EpisodeLoadStep.ChooseSource>(h.step())
        assertTrue(job.isActive)
        h.source.resolveError = null
        h.loader.decide(EpisodeLoadDecision.Pick(sources[1]))
        runCurrent()
        assertEquals("http://v/1080p?resolved", h.resolved.single().storage.link)
    }

    @Test
    fun stalePickIgnoredOnFailed() = runTest {
        val sources = listOf(storage("720p"), storage("1080p"))
        val h = harness(FakeSource(sources, resolveError = IllegalStateException("boom")))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        h.loader.decide(EpisodeLoadDecision.Pick(sources[0]))
        advanceUntilIdle()
        assertIs<EpisodeLoadStep.Failed>(h.step())
        // Second tap on a sheet row lands after the failure shows.
        h.loader.decide(EpisodeLoadDecision.Pick(sources[0]))
        advanceUntilIdle()
        assertIs<EpisodeLoadStep.Failed>(h.step())
        assertTrue(job.isActive)
        assertTrue(h.resolved.isEmpty())
        h.source.resolveError = null
        h.loader.decide(EpisodeLoadDecision.Retry)
        runCurrent()
        assertEquals("http://v/720p?resolved", h.resolved.single().storage.link)
    }

    @Test
    fun staleDecisionDrainedAtLoadStart() = runTest {
        val sources = listOf(storage("720p"), storage("1080p"))
        val h = harness(FakeSource(sources))
        h.loader.decide(EpisodeLoadDecision.Pick(sources[1]))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        assertIs<EpisodeLoadStep.ChooseSource>(h.step())
        assertTrue(h.resolved.isEmpty())
        job.cancel()
    }

    @Test
    fun blankLinkIsFailure() = runTest {
        val h = harness(FakeSource(listOf(storage("720p")), resolveLink = { "" }))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        assertIs<EpisodeLoadStep.Failed>(h.step())
        job.cancel()
        advanceUntilIdle()

        h.load(EpisodeAction.Download, h.chapter())
        val (failed, error) = h.failures.single()
        assertEquals("720p", failed?.quality)
        assertTrue(error != null)
        assertTrue(h.resolved.isEmpty())
    }

    @Test
    fun downloadFailureUsesCallback() = runTest {
        val error = IllegalStateException("boom")
        val h = harness(FakeSource(listOf(storage("720p")), resolveError = error))
        record(h)
        h.load(EpisodeAction.Download, h.chapter())
        advanceUntilIdle()
        val (failed, e) = h.failures.single()
        assertEquals("720p", failed?.quality)
        assertEquals(error, e)
        assertNull(h.loader.state)
        assertTrue(h.steps.none { it is EpisodeLoadStep.Failed })
    }

    @Test
    fun downloadNoSourcesUsesCallback() = runTest {
        val h = harness(FakeSource(emptyList()))
        h.load(EpisodeAction.Download, h.chapter())
        assertEquals(listOf<Pair<KmpStorage?, Throwable?>>(null to null), h.failures)
        assertNull(h.loader.state)
    }

    @Test
    fun onResolvedThrowingLeavesStateClearedAndNotDownloadFailure() = runTest {
        for (action in EpisodeAction.entries) {
            var throwing = true
            val failures = mutableListOf<KmpStorage?>()
            val source = FakeSource(listOf(storage("720p")))
            val loader = EpisodeLoader(backgroundScope)
            val chapter = KmpChapterModel("Ep 1", "u", "", "u", source)
            val info = KmpInfoModel("Show", "", "u", "http://img", emptyList(), emptyList(), emptyList(), source)
            val result = runCatching {
                loader.load(action, chapter, info, onResolved = { if (throwing) throw IllegalStateException("callback") }, onDownloadFailed = { s, _ -> failures += s })
            }
            assertEquals("callback", result.exceptionOrNull()?.message)
            assertNull(loader.state)
            assertTrue(failures.isEmpty())
            throwing = false
            var called = false
            loader.load(action, chapter, info, onResolved = { called = true })
            assertTrue(called)
            loader.finishHandoff()
            advanceUntilIdle()
        }
    }

    @Test
    fun concurrentLoadsOnlyOneRuns() = runTest {
        val gate = CompletableDeferred<Unit>()
        val h = harness(FakeSource(listOf(storage("720p")), gate = gate))
        val first = startLoad(h, EpisodeAction.Play, h.chapter("Ep 1", "u1"))
        val second = startLoad(h, EpisodeAction.Play, h.chapter("Ep 2", "u2"))
        advanceUntilIdle()
        assertTrue(second.isCompleted)
        assertEquals("u1", h.loader.state?.episodeUrl)
        gate.complete(Unit)
        advanceUntilIdle()
        first.join()
        assertEquals(1, h.resolved.size)
        h.loader.finishHandoff()
        assertNull(h.loader.state)
    }
}
