package com.programmersbox.aniyomi

import android.app.Application
import com.programmersbox.aniyomi.repo.AniyomiRepoClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Real network run: install fixture AnimeOnsen APK -> load -> browse -> episodes -> video links.
 * Skipped unless ANIYOMI_E2E=1, so normal test runs stay offline.
 */
class AniyomiEndToEndNetworkTest {
    private val root = Files.createTempDirectory("aniyomie2e").toFile()

    @AfterTest fun cleanup() {
        root.deleteRecursively()
    }

    @Test fun animeOnsenBrowsesToVideoLinks() = runBlocking {
        assumeTrue("set ANIYOMI_E2E=1 to run", System.getenv("ANIYOMI_E2E") == "1")
        AniyomiRuntime.init(Application("aniyomi-e2e", File(root, "data")))

        val store = AniyomiApkStore(File(root, "apks"), File(root, "jars"))
        store.install(File(root, "dl.apk").also { AniyomiFixture.apk.copyTo(it) })
        val trustId = AniyomiTrust.trustId(
            AniyomiFixture.PACKAGE,
            AniyomiFixture.VERSION_CODE,
            AniyomiFixture.REPO_FINGERPRINT,
        )
        val catalog = AniyomiCatalog(
            application = Application("aniyomi-e2e", File(root, "catalog")),
            repoClient = AniyomiRepoClient(HttpClient(MockEngine { throw IOException("offline") })),
            store = store,
            customUrls = { emptyList() },
            trustedIds = { setOf(trustId) },
            addTrusted = {},
        )
        catalog.initialize()
        val api = assertIs<AniyomiApiService>(catalog.getSources().single().apiService)

        val recent = api.recent(1)
        assertTrue(recent.isNotEmpty(), "recent(1) empty")
        val info = api.itemInfo(recent.first())
        assertTrue(info.chapters.isNotEmpty(), "no episodes for ${info.title}")
        val storages = api.chapterInfo(info.chapters.first())
        assertTrue(storages.isNotEmpty(), "no videos for ${info.chapters.first().url}")
        val resolved = api.resolve(storages.first())
        assertTrue(!resolved.link.isNullOrBlank(), "blank link: $resolved")
        println("E2E OK: ${recent.size} recent, ${info.chapters.size} episodes, ${storages.size} videos, link=${resolved.link}")
    }
}
