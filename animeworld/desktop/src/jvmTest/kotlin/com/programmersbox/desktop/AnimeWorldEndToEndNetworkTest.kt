package com.programmersbox.desktop

import android.app.Application
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.anime.shared.downloads.AnimeDownloadManager
import com.programmersbox.anime.shared.downloads.QueuedDownloadState
import com.programmersbox.anime.shared.videoplayer.DesktopPlayer
import com.programmersbox.anime.shared.videoplayer.LocalStreamProxy
import com.programmersbox.anime.shared.videoplayer.PlayerSource
import com.programmersbox.anime.shared.videoplayer.VlcRuntime
import com.programmersbox.anime.shared.videoplayer.playerHeaders
import com.programmersbox.anime.shared.videoplayer.streamMime
import com.programmersbox.aniyomi.AniyomiApiService
import com.programmersbox.aniyomi.AniyomiApkStore
import com.programmersbox.aniyomi.AniyomiCatalog
import com.programmersbox.aniyomi.AniyomiTrust
import com.programmersbox.aniyomi.repo.AniyomiRepoClient
import com.programmersbox.datastore.DataStoreSettings
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createDataStore
import com.programmersbox.kmpmodels.KmpStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.Assume.assumeTrue
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Real network run: AnimeOnsen fixture extension -> first episode -> resolved (DASH) stream -> [DesktopPlayer]
 * until a frame arrives -> [AnimeDownloadManager] download -> the downloaded file plays.
 * Skipped unless ANIMEWORLD_E2E=1, so normal test runs stay offline.
 */
class AnimeWorldEndToEndNetworkTest {
    private val root = createTempDirectory("animeworld-e2e").toFile()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val proxy = LocalStreamProxy()
    private val players = mutableListOf<DesktopPlayer>()

    @AfterTest
    fun cleanup() {
        scope.cancel()
        players.forEach { it.close() }
        proxy.close()
        root.deleteRecursively()
    }

    @Test
    fun animeOnsenResolvesPlaysAndDownloads() = runBlocking {
        assumeTrue("set ANIMEWORLD_E2E=1 to run", System.getenv("ANIMEWORLD_E2E") == "1")
        val factory = VlcRuntime.factory().getOrThrow()

        val storage = resolveFirstEpisode()
        val link = storage.link.orEmpty()
        println("E2E resolved: link=$link mime=${storage.mimeType} headers=${storage.headers.keys}")
        assertTrue(link.isNotBlank(), "blank link")

        val player = DesktopPlayer(factory, proxy).also(players::add)
        player.open(PlayerSource.Stream(link, playerHeaders(storage.headers, null), streamMime(storage)))
        awaitTrue("a video frame from the stream", 60_000) { player.frame.value != null }
        println("E2E stream frame: ${player.frame.value!!.width}x${player.frame.value!!.height}, duration=${player.state.value.durationMs} ms")
        player.close()

        val settings = animeDesktopSettings()
        val downloads = File(root, "downloads").also { it.mkdirs() }
        settings.downloadsDirectory.set(downloads.absolutePath)
        val manager = AnimeDownloadManager(scope, settings, trayState = null)
        manager.download(storage, null, "AnimeOnsen E2E", "Episode 1")
        awaitTrue("download finished", 600_000) {
            val queue = manager.queue.value
            if (queue.any { it.state == QueuedDownloadState.Failed }) fail("download failed: $queue")
            queue.isEmpty()
        }
        val file = downloads.walkTopDown().firstOrNull { it.isFile && it.extension in setOf("mp4", "mkv") }
            ?: fail("no downloaded video under $downloads: ${downloads.walkTopDown().toList()}")
        println("E2E downloaded: ${file.name} ${file.length()} bytes")

        val local = DesktopPlayer(factory, proxy).also(players::add)
        local.open(PlayerSource.LocalFile(file.absolutePath))
        awaitTrue("a video frame from the download", 60_000) { local.frame.value != null }
        println("E2E download frame: ${local.frame.value!!.width}x${local.frame.value!!.height}, duration=${local.state.value.durationMs} ms")
    }

    private suspend fun resolveFirstEpisode(): KmpStorage {
        val apkStore = AniyomiApkStore(File(root, "apks"), File(root, "jars"))
        apkStore.install(File(root, "dl.apk").also { fixtureApk().copyTo(it) })
        val settings = NewSettingsHandling(
            createDataStore(FileSystem.SYSTEM, SettingsSerializer()) { File(root, "settings.pb").toOkioPath() },
        )
        // The repo fetch is mocked offline, so trust the fixture explicitly.
        settings.addTrustedAniyomiExtension(AniyomiTrust.trustId(PACKAGE, VERSION_CODE, REPO_FINGERPRINT))
        val catalog = AniyomiCatalog(
            application = Application("aniyomi-e2e", File(root, "data")),
            settings = settings,
            repoClient = AniyomiRepoClient(HttpClient(MockEngine { throw IOException("offline") })),
            store = apkStore,
        )
        catalog.initialize()
        val api = assertIs<AniyomiApiService>(catalog.getSources().single().apiService)
        val recent = api.recent(1)
        assertTrue(recent.isNotEmpty(), "recent(1) empty")
        val info = api.itemInfo(recent.first())
        assertTrue(info.chapters.isNotEmpty(), "no episodes for ${info.title}")
        val storages = api.chapterInfo(info.chapters.first())
        assertTrue(storages.isNotEmpty(), "no videos for ${info.chapters.first().url}")
        return api.resolve(storages.first())
    }

    private fun animeDesktopSettings(): AnimeDesktopSettings {
        DataStoreSettings { File(root, it).absolutePath }
        return AnimeDesktopSettings(AppDirs { appName = "AnimeWorldE2E"; appAuthor = "test" })
    }

    /** Same fixture the Aniyomi module's tests use, read from the repo (Gradle runs tests from `animeworld/desktop`). */
    private fun fixtureApk(): File =
        File("../../source_utilities/aniyomi/src/jvmTest/resources/fixtures/aniyomi-all.animeonsen-v14.10.apk")
            .also { assertTrue(it.isFile, "fixture missing at ${it.absolutePath}") }

    private fun awaitTrue(what: String, timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(100)
        }
        if (!condition()) fail("timed out after $timeoutMs ms waiting for: $what")
    }

    private companion object {
        // Mirrors AniyomiFixture in :source_utilities:aniyomi's jvmTest (not visible from this module).
        const val PACKAGE = "eu.kanade.tachiyomi.animeextension.all.animeonsen"
        const val VERSION_CODE = 10L
        const val REPO_FINGERPRINT = "cbec121aa82ebb02aaa73806992e0368a97d47b5451ed6524816d03084c45905"
    }
}
