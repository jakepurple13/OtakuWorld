package com.programmersbox.desktop

import androidx.compose.ui.window.TrayState
import com.programmersbox.anime.shared.StorageHolder
import com.programmersbox.anime.shared.VideoScreen
import com.programmersbox.anime.shared.downloads.EpisodeDownloadQueue
import com.programmersbox.anime.shared.episodeload.EpisodeAction
import com.programmersbox.anime.shared.episodeload.EpisodeLoadDecision
import com.programmersbox.anime.shared.episodeload.EpisodeLoadStep
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Proxy
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GenericAnimeDesktopTest {

    private class FakeSource(
        var storages: List<KmpStorage>,
        var resolveError: Exception? = null,
    ) : KmpApiService {
        override val baseUrl = "http://test"
        override suspend fun chapterInfo(chapterModel: KmpChapterModel) = storages
        override suspend fun resolve(storage: KmpStorage): KmpStorage {
            resolveError?.let { throw it }
            return storage.copy(link = storage.link + "?resolved").also { it.headers.putAll(storage.headers) }
        }
    }

    private class TestInfo(
        navigated: MutableList<Any>,
        holder: StorageHolder,
    ) : GenericAnimeDesktop(
        appConfig = AppConfig("test", BuildType.NoFirebase, false),
        navigationActions = nav(navigated),
        episodeDownloads = EpisodeDownloadQueue { _, _, _, _ -> error("enqueueDownload is overridden") },
        storageHolder = holder,
        trayState = TrayState(),
    ) {
        val messages = mutableListOf<String>()
        val enqueued = mutableListOf<KmpStorage>()

        override fun notifyUser(title: String, message: String, isError: Boolean) {
            messages += "$title: $message"
        }

        override fun enqueueDownload(storage: KmpStorage, model: KmpChapterModel, infoModel: KmpInfoModel) {
            enqueued += storage
        }
    }

    private val navigated = mutableListOf<Any>()
    private val holder = StorageHolder()
    private val info = TestInfo(navigated, holder)

    @AfterTest
    fun reset() {
        info.episodeLoader.decide(EpisodeLoadDecision.Cancel)
    }

    /** Waits until the loader shows a step that is waiting on the user. */
    private suspend fun awaitWaiting(): com.programmersbox.anime.shared.episodeload.EpisodeLoadState {
        repeat(200) {
            val st = info.episodeLoader.state
            if (st != null && (st.step is EpisodeLoadStep.ChooseSource || st.step is EpisodeLoadStep.Failed)) return st
            kotlinx.coroutines.delay(10)
        }
        error("loader never waited, state=${info.episodeLoader.state}")
    }

    private fun storage(quality: String) = KmpStorage(link = "http://v/$quality", quality = quality, source = "src")

    private fun fixtures(source: KmpApiService): Pair<KmpChapterModel, KmpInfoModel> {
        val chapter = KmpChapterModel("Ep 1", "u", "", "u", source)
        return chapter to KmpInfoModel("Show", "", "u", "http://img", listOf(chapter), emptyList(), emptyList(), source)
    }

    @Test
    fun singleVideoPlaysImmediately() = runBlocking {
        val source = FakeSource(listOf(storage("720p")))
        val (chapter, show) = fixtures(source)

        info.chapterOnClick(chapter, listOf(chapter), show, nav(navigated))

        val screen = navigated.single() as VideoScreen
        assertEquals("http://v/720p?resolved", screen.showPath)
        assertEquals("http://v/720p?resolved", holder.storageModel?.link)
        assertEquals("Show", holder.showTitle)
        assertEquals("http://img", holder.posterUrl)
        assertNull(info.episodeLoader.state)
    }

    @Test
    fun multipleVideosOpenChoice() = runBlocking {
        val source = FakeSource(listOf(storage("720p"), storage("1080p")))
        val (chapter, show) = fixtures(source)

        val job = launch { info.chapterOnClick(chapter, listOf(chapter), show, nav(navigated)) }
        val st = awaitWaiting()

        val step = st.step as EpisodeLoadStep.ChooseSource
        assertEquals(2, step.sources.size)
        assertEquals(EpisodeAction.Play, st.action)
        assertTrue(navigated.isEmpty())
        info.episodeLoader.decide(EpisodeLoadDecision.Cancel)
        job.join()
    }

    @Test
    fun resolveFailureReported() = runBlocking {
        val source = FakeSource(listOf(storage("720p")), resolveError = IllegalStateException("boom"))
        val (chapter, show) = fixtures(source)

        val job = launch { info.chapterOnClick(chapter, listOf(chapter), show, nav(navigated)) }
        val st = awaitWaiting()

        assertTrue(st.step is EpisodeLoadStep.Failed)
        assertTrue(navigated.isEmpty())
        assertNull(holder.storageModel)
        assertTrue(info.messages.isEmpty(), info.messages.toString())
        info.episodeLoader.decide(EpisodeLoadDecision.Cancel)
        job.join()
    }

    @Test
    fun downloadWithMultipleVideosOpensChoice() = runBlocking {
        val source = FakeSource(listOf(storage("720p"), storage("1080p")))
        val (chapter, show) = fixtures(source)

        val job = launch { info.startDownload(chapter, show) }
        val st = awaitWaiting()

        assertEquals(EpisodeAction.Download, st.action)
        assertTrue(st.step is EpisodeLoadStep.ChooseSource)
        assertTrue(info.enqueued.isEmpty())
        info.episodeLoader.decide(EpisodeLoadDecision.Cancel)
        job.join()
    }

    @Test
    fun downloadResolveFailureReported() = runBlocking {
        val source = FakeSource(listOf(storage("720p")), resolveError = IllegalStateException("boom"))
        val (chapter, show) = fixtures(source)

        info.startDownload(chapter, show)

        assertTrue(info.messages.single().contains("boom"), info.messages.toString())
        assertTrue(info.enqueued.isEmpty())
    }

    @Test
    fun downloadWithSingleVideoResolvesThenEnqueues() = runBlocking {
        val source = FakeSource(listOf(storage("720p")))
        val (chapter, show) = fixtures(source)

        info.startDownload(chapter, show)

        assertEquals("http://v/720p?resolved", info.enqueued.single().link)
    }

    @Test
    fun downloadWithNoVideosReportsError() = runBlocking {
        val source = FakeSource(emptyList())
        val (chapter, show) = fixtures(source)

        info.startDownload(chapter, show)

        assertTrue(info.messages.single().contains("no videos"), info.messages.toString())
        assertTrue(info.enqueued.isEmpty())
    }
}

private fun nav(log: MutableList<Any>): NavigationActions =
    Proxy.newProxyInstance(
        NavigationActions::class.java.classLoader,
        arrayOf(NavigationActions::class.java)
    ) { _, method, args ->
        if (method.name == "navigate") log += args!![0]
        null
    } as NavigationActions
