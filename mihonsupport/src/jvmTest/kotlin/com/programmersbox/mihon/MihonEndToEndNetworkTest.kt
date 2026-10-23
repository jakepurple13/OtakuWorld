package com.programmersbox.mihon

import android.app.Application
import com.programmersbox.mihon.repo.MihonRepoClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Real network run: Keiyoushi repo -> download APK -> install -> load -> browse -> chapter -> pages.
 * Skipped unless MIHON_E2E=1, so normal test runs stay offline.
 */
class MihonEndToEndNetworkTest {
    private val root = Files.createTempDirectory("mihone2e").toFile()

    @AfterTest fun cleanup() {
        root.deleteRecursively()
    }

    @Test fun keiyoushiExtensionBrowsesToPages() = runBlocking {
        assumeTrue("set MIHON_E2E=1 to run", System.getenv("MIHON_E2E") == "1")
        MihonRuntime.init(Application("mihon-e2e", File(root, "data")))

        val http = HttpClient(OkHttp)
        val repoClient = MihonRepoClient(http)
        val remote = repoClient.fetch(listOf(REPO_URL))
            .firstOrNull { it.packageName == PACKAGE }
        checkNotNull(remote) { "$PACKAGE not in $REPO_URL" }
        assertTrue(remote.downloadLink.startsWith("https://"), remote.downloadLink)

        val response = http.get(remote.downloadLink)
        check(response.status.isSuccess()) { "GET ${remote.downloadLink} -> ${response.status}" }
        val download = File(root, "download.apk").apply { writeBytes(response.readRawBytes()) }

        val store = MihonApkStore(File(root, "apks"), File(root, "jars"))
        store.install(download)
        val loader = JvmMihonExtensionLoader(store)
        try {
            val catalog = testCatalog(store)
            val api = assertIs<MihonApiService>(
                loader.load(catalog, repoClient.signingKeys, emptySet()) {}.first().apiService
            )

            val recent = api.recent(1)
            assertTrue(recent.isNotEmpty(), "recent(1) empty")
            val info = api.itemInfo(recent.first())
            assertTrue(info.chapters.isNotEmpty(), "no chapters for ${info.title}")
            val pages = api.chapterInfo(info.chapters.first())
            assertTrue(pages.isNotEmpty(), "no pages for ${info.chapters.first().url}")
            assertTrue(
                pages.all { it.link?.startsWith("http://") == true || it.link?.startsWith("https://") == true },
                pages.map { it.link }.toString(),
            )
            println("E2E OK: ${remote.name} ${remote.version}: ${recent.size} recent, ${info.chapters.size} chapters, ${pages.size} pages")
        } finally {
            loader.close()
            http.close()
        }
    }

    private companion object {
        const val REPO_URL = "https://raw.githubusercontent.com/keiyoushi/extensions/repo"
        const val PACKAGE = MihonFixture.PACKAGE
    }
}
