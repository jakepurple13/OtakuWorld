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
import com.programmersbox.cloudstream.CloudstreamApiService
import com.programmersbox.cloudstream.CloudstreamCatalog
import com.programmersbox.cloudstream.plugins.PluginLoader
import com.programmersbox.cloudstream.plugins.PluginStore
import com.programmersbox.cloudstream.repo.CloudstreamRepoClient
import com.programmersbox.datastore.DataStoreSettings
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createDataStore
import com.programmersbox.kmpmodels.KmpStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Real network run: add the phisher98 CloudStream repo -> trust it -> install HiAnime -> `recent` -> details -> first
 * episode's links -> the first one that plays a frame in [DesktopPlayer] -> [AnimeDownloadManager] download.
 * Skipped unless CLOUDSTREAM_E2E=1, so normal test runs stay offline. Plugin code is third-party and runs for real.
 */
class CloudstreamEndToEndNetworkTest {
    private val root = createTempDirectory("cloudstream-e2e").toFile()
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
    fun hiAnimeResolvesPlaysAndDownloads() = runBlocking {
        assumeTrue("set CLOUDSTREAM_E2E=1 to run", System.getenv("CLOUDSTREAM_E2E") == "1")
        val factory = VlcRuntime.factory().getOrThrow()

        val (api, storages) = resolveFirstEpisodeLinks()
        println("E2E ${storages.size} links: " + storages.joinToString { "${it.source}/${it.quality}=${it.link} (${it.mimeType})" })

        val playing = storages.firstNotNullOfOrNull { storage ->
            val link = storage.link.orEmpty()
            if (link.isBlank()) return@firstNotNullOfOrNull null
            val player = DesktopPlayer(factory, proxy).also(players::add)
            player.open(PlayerSource.Stream(link, playerHeaders(storage.headers, null), streamMime(storage)))
            val gotFrame = waitFor(30_000) { player.frame.value != null }
            if (gotFrame) {
                println("E2E stream frame: ${player.frame.value!!.width}x${player.frame.value!!.height}, duration=${player.state.value.durationMs} ms (${storage.link})")
            } else {
                println("E2E no frame within 30 s from ${storage.link}; trying the next link")
            }
            player.close()
            storage.takeIf { gotFrame }
        } ?: fail("none of ${storages.size} links played a frame; api=${api.api.name}")

        val settings = animeDesktopSettings()
        val downloads = File(root, "downloads").also { it.mkdirs() }
        settings.downloadsDirectory.set(downloads.absolutePath)
        val manager = AnimeDownloadManager(scope, settings, trayState = null)
        manager.download(playing, null, "CloudStream E2E", "Episode 1")
        awaitTrue("download finished", 600_000) {
            val queue = manager.queue.value
            if (queue.any { it.state == QueuedDownloadState.Failed }) fail("download failed: $queue")
            queue.isEmpty()
        }
        val file = downloads.walkTopDown().firstOrNull { it.isFile && it.extension in setOf("mp4", "mkv", "ts") }
            ?: fail("no downloaded video under $downloads: ${downloads.walkTopDown().toList()}")
        println("E2E downloaded: ${file.name} ${file.length()} bytes")
        assertTrue(file.length() > 0, "empty download")

        val local = DesktopPlayer(factory, proxy).also(players::add)
        local.open(PlayerSource.LocalFile(file.absolutePath))
        awaitTrue("a video frame from the download", 60_000) { local.frame.value != null }
        println("E2E download frame: ${local.frame.value!!.width}x${local.frame.value!!.height}, duration=${local.state.value.durationMs} ms")
    }

    private suspend fun resolveFirstEpisodeLinks(): Pair<CloudstreamApiService, List<KmpStorage>> {
        val application = Application("cloudstream-e2e", File(root, "data"))
        val settings = NewSettingsHandling(
            createDataStore(FileSystem.SYSTEM, SettingsSerializer()) { File(root, "settings.pb").toOkioPath() },
        )
        val repoClient = CloudstreamRepoClient(HttpClient(OkHttp) { engine { config { followSslRedirects(false) } } })
        val catalog = CloudstreamCatalog(
            application = application,
            settings = settings,
            repoClient = repoClient,
            store = PluginStore(File(root, "plugins"), OkHttpClient.Builder().followSslRedirects(false).build()),
            loader = PluginLoader(File(root, "jars"), application),
        )

        val remotes = catalog.getRemoteSources(listOf(REPO_URL))
        val remote = remotes.firstOrNull { it.packageName == "HiAnime" || it.name == "HiAnime" }
            ?: fail("HiAnime not in repo listing: ${remotes.map { it.name }}")
        println("E2E repo lists ${remotes.size} anime plugins; installing ${remote.name} ${remote.version} from ${remote.downloadLink}")
        // Adding a repo is not trusting it: trust is an explicit user step, so do it explicitly here.
        settings.addTrustedCloudstreamRepo(REPO_URL)
        assertTrue(catalog.install(remote), "install returned false")
        catalog.initialize()

        val api = catalog.getSources().map { it.apiService }.filterIsInstance<CloudstreamApiService>().firstOrNull()
            ?: fail("no CloudStream service after install: ${catalog.getSources().map { it.name }}")
        assertIs<CloudstreamApiService>(api)
        val recent = api.recent(1)
        assertTrue(recent.isNotEmpty(), "recent(1) empty")
        println("E2E recent: ${recent.take(3).map { it.title }}")
        val info = api.itemInfo(recent.first())
        assertTrue(info.chapters.isNotEmpty(), "no episodes for ${info.title}")
        println("E2E info: ${info.title}, ${info.chapters.size} episodes; first=${info.chapters.first().name}")
        val storages = api.chapterInfo(info.chapters.first())
        assertTrue(storages.isNotEmpty(), "no videos for ${info.chapters.first().url}")
        return api to storages.map { api.resolve(it) }
    }

    private fun animeDesktopSettings(): AnimeDesktopSettings {
        DataStoreSettings { File(root, it).absolutePath }
        return AnimeDesktopSettings(AppDirs { appName = "AnimeWorldE2E"; appAuthor = "test" })
    }

    private fun waitFor(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(100)
        }
        return condition()
    }

    private fun awaitTrue(what: String, timeoutMs: Long, condition: () -> Boolean) {
        if (!waitFor(timeoutMs, condition)) fail("timed out after $timeoutMs ms waiting for: $what")
    }

    private companion object {
        // Same repo as CloudstreamFixture in :source_utilities:cloudstream's jvmTest (not visible from this module).
        const val REPO_URL = "https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/builds/repo.json"
    }
}
