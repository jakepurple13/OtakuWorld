package com.programmersbox.anime.shared.episodeload

import androidx.compose.runtime.snapshotFlow
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

    private class Harness(val source: FakeSource) {
        val resolved = mutableListOf<Resolved>()
        val stateAtResolve = mutableListOf<EpisodeLoadState?>()
        val failures = mutableListOf<Pair<KmpStorage?, Throwable?>>()
        val steps = mutableListOf<EpisodeLoadStep>()
        lateinit var loader: EpisodeLoader

        init {
            // snapshotFlow can miss steps that change within one dispatch, so also capture at resolve time.
            source.onResolve = { loader.state?.step?.let(steps::add) }
            loader = EpisodeLoader(
                onResolved = { action, storage, _, _ ->
                    stateAtResolve += loader.state
                    resolved += Resolved(action, storage)
                },
                onDownloadFailed = { storage, e -> failures += storage to e },
            )
        }

        fun chapter(name: String = "Ep 1", url: String = "u") = KmpChapterModel(name, url, "", url, source)
        fun info() = KmpInfoModel("Show", "", "u", "http://img", emptyList(), emptyList(), emptyList(), source)
        fun step() = loader.state?.step
    }

    private fun storage(quality: String) = KmpStorage(link = "http://v/$quality", quality = quality, source = "src")

    private fun TestScope.record(h: Harness) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            snapshotFlow { h.loader.state?.step }.collect { it?.let(h.steps::add) }
        }
    }

    private fun TestScope.startLoad(h: Harness, action: EpisodeAction, chapter: KmpChapterModel = h.chapter()): Job =
        launch { h.loader.load(action, chapter, h.info()) }

    @Test
    fun singleSourcePlaysWithoutSheet() = runTest {
        val h = Harness(FakeSource(listOf(storage("720p"))))
        record(h)
        h.loader.load(EpisodeAction.Play, h.chapter(), h.info())
        advanceUntilIdle()
        assertEquals(1, h.resolved.size)
        assertEquals(EpisodeAction.Play, h.resolved[0].action)
        assertEquals("http://v/720p?resolved", h.resolved[0].storage.link)
        assertTrue(h.steps.none { it is EpisodeLoadStep.ChooseSource })
        assertNull(h.loader.state)
    }

    @Test
    fun stateClearedBeforePlay() = runTest {
        val h = Harness(FakeSource(listOf(storage("720p"))))
        h.loader.load(EpisodeAction.Play, h.chapter(), h.info())
        assertEquals(listOf<EpisodeLoadState?>(null), h.stateAtResolve)
    }

    @Test
    fun multipleSourcesWaitForPick() = runTest {
        val sources = listOf(storage("720p"), storage("1080p"))
        val h = Harness(FakeSource(sources))
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
        val h = Harness(FakeSource(listOf(storage("720p"), storage("1080p"))))
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
        val h = Harness(FakeSource(listOf(storage("720p")), gate = gate))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        assertIs<EpisodeLoadStep.FetchingSources>(h.step())
        job.cancel()
        advanceUntilIdle()
        assertNull(h.loader.state)
        gate.complete(Unit)
        h.loader.load(EpisodeAction.Play, h.chapter(), h.info())
        assertEquals(1, h.resolved.size)
    }

    @Test
    fun tapWhileLoadingIsIgnored() = runTest {
        val h = Harness(FakeSource(listOf(storage("720p"), storage("1080p"))))
        val job = startLoad(h, EpisodeAction.Play, h.chapter("Ep 1", "u1"))
        advanceUntilIdle()
        h.loader.load(EpisodeAction.Play, h.chapter("Ep 2", "u2"), h.info())
        assertEquals("u1", h.loader.state?.episodeUrl)
        assertIs<EpisodeLoadStep.ChooseSource>(h.step())
        job.cancel()
    }

    @Test
    fun playResolveFailureShowsError() = runTest {
        val h = Harness(FakeSource(listOf(storage("720p")), resolveError = IllegalStateException("boom")))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        assertEquals(EpisodeLoadStep.Failed("Couldn't load Ep 1", "boom", false), h.step())
        h.source.resolveError = null
        h.loader.decide(EpisodeLoadDecision.Retry)
        advanceUntilIdle()
        job.join()
        assertEquals(1, h.resolved.size)
        assertNull(h.loader.state)
    }

    @Test
    fun pickOtherReopensSheet() = runTest {
        val sources = listOf(storage("720p"), storage("1080p"))
        val h = Harness(FakeSource(sources, resolveError = IllegalStateException("boom")))
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
        val h = Harness(FakeSource(listOf(storage("720p")), resolveError = IllegalStateException("boom")))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        assertFalse(assertIs<EpisodeLoadStep.Failed>(h.step()).canPickOther)
        job.cancel()
    }

    @Test
    fun playNoSourcesShowsError() = runTest {
        val h = Harness(FakeSource(emptyList()))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        assertEquals(EpisodeLoadStep.Failed("No sources found for Ep 1", null, false), h.step())
        h.loader.decide(EpisodeLoadDecision.Cancel)
        advanceUntilIdle()
        assertTrue(job.isCompleted)
        assertNull(h.loader.state)
    }

    @Test
    fun blankLinkIsFailure() = runTest {
        val h = Harness(FakeSource(listOf(storage("720p")), resolveLink = { "" }))
        val job = startLoad(h, EpisodeAction.Play)
        advanceUntilIdle()
        assertIs<EpisodeLoadStep.Failed>(h.step())
        job.cancel()
        advanceUntilIdle()

        h.loader.load(EpisodeAction.Download, h.chapter(), h.info())
        val (failed, error) = h.failures.single()
        assertEquals("720p", failed?.quality)
        assertTrue(error != null)
        assertTrue(h.resolved.isEmpty())
    }

    @Test
    fun downloadFailureUsesCallback() = runTest {
        val error = IllegalStateException("boom")
        val h = Harness(FakeSource(listOf(storage("720p")), resolveError = error))
        record(h)
        h.loader.load(EpisodeAction.Download, h.chapter(), h.info())
        advanceUntilIdle()
        val (failed, e) = h.failures.single()
        assertEquals("720p", failed?.quality)
        assertEquals(error, e)
        assertNull(h.loader.state)
        assertTrue(h.steps.none { it is EpisodeLoadStep.Failed })
    }

    @Test
    fun downloadNoSourcesUsesCallback() = runTest {
        val h = Harness(FakeSource(emptyList()))
        h.loader.load(EpisodeAction.Download, h.chapter(), h.info())
        assertEquals(listOf<Pair<KmpStorage?, Throwable?>>(null to null), h.failures)
        assertNull(h.loader.state)
    }
}
