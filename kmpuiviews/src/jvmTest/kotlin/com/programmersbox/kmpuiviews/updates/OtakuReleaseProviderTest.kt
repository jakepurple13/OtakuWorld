package com.programmersbox.kmpuiviews.updates

import com.sun.net.httpserver.HttpServer
import dev.nucleusframework.core.runtime.Platform
import java.net.InetSocketAddress
import java.net.http.HttpClient
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OtakuReleaseProviderTest {
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val base = "http://127.0.0.1:${server.address.port}"
    private val requests = CopyOnWriteArrayList<String>()
    private val client = HttpClient.newHttpClient()
    private val releasesPath = "/repos/jakepurple13/OtakuWorld/releases"

    @AfterTest
    fun tearDown() = server.stop(0)

    private fun releaseJson(tag: String, pre: Boolean, vararg assets: String) =
        """{"tag_name":"$tag","prerelease":$pre,"html_url":"$base/tag/$tag","extra":1,
            |"assets":[${assets.joinToString(",") { """{"name":"$it","size":1}""" }}]}""".trimMargin()

    private fun serve(path: String, bodyFor: (query: String?) -> String) {
        server.createContext(path) { ex ->
            requests += ex.requestURI.toString()
            val bytes = bodyFor(ex.requestURI.query).toByteArray()
            ex.sendResponseHeaders(200, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
    }

    private fun serveReleases(vararg json: String) = serve(releasesPath) { "[${json.joinToString(",")}]" }

    private fun serveYml(tag: String, yml: String, version: String) =
        serve("/jakepurple13/OtakuWorld/releases/download/$tag/$yml") { "version: $version\n" }

    @Test
    fun stableResolvesMetadataAndDownloadUrlsToChosenTag() {
        serveReleases(releaseJson("v33.0.0", false, "mangaworld-mac.yml"))
        server.start()
        val p = OtakuReleaseProvider(UpdateTrack.Stable, apiBase = base, webBase = base)
        assertEquals(
            "$base/jakepurple13/OtakuWorld/releases/download/v33.0.0/mangaworld-mac.yml",
            p.resolveMetadataUrl("mangaworld", Platform.MacOS, client)
        )
        assertEquals(
            "$base/jakepurple13/OtakuWorld/releases/download/v33.0.0/MangaWorld-33.0.0.dmg",
            p.getDownloadUrl("MangaWorld-33.0.0.dmg", "33.0.0")
        )
        assertEquals("v33.0.0", p.resolvedRelease?.tagName)
    }

    @Test
    fun nightlyReadsBothYmlsAndPicksHigher() {
        serveReleases(
            releaseJson("nightly_2026_10_30", true, "mangaworld-mac.yml"),
            releaseJson("v32.1.2", false, "mangaworld-mac.yml"),
        )
        serveYml("nightly_2026_10_30", "mangaworld-mac.yml", "33.0.0-alpha.20261030.1415")
        serveYml("v32.1.2", "mangaworld-mac.yml", "32.1.2")
        server.start()
        val p = OtakuReleaseProvider(UpdateTrack.Nightly, apiBase = base, webBase = base)
        assertEquals(
            "$base/jakepurple13/OtakuWorld/releases/download/nightly_2026_10_30/mangaworld-mac.yml",
            p.resolveMetadataUrl("mangaworld", Platform.MacOS, client)
        )
        assertEquals("nightly_2026_10_30", p.resolvedRelease?.tagName)
    }

    @Test
    fun paginatesUntilShortPage() {
        val page1 = (1..100).joinToString(",") { releaseJson("nightly_$it", true, "animeworld-mac.yml") }
        serve(releasesPath) { q ->
            if (q?.contains("page=2") == true) "[${releaseJson("v32.1.2", false, "mangaworld-mac.yml")}]" else "[$page1]"
        }
        server.start()
        val p = OtakuReleaseProvider(UpdateTrack.Stable, apiBase = base, webBase = base)
        p.resolveMetadataUrl("mangaworld", Platform.MacOS, client)
        assertEquals("v32.1.2", p.resolvedRelease?.tagName)
        assertTrue("$releasesPath?per_page=100&page=2" in requests, requests.toString())
    }

    @Test
    fun noMatchingReleaseThrowsNoSuchElement() {
        serveReleases()
        server.start()
        val p = OtakuReleaseProvider(UpdateTrack.Stable, apiBase = base, webBase = base)
        assertFailsWith<NoSuchElementException> {
            p.resolveMetadataUrl("mangaworld", Platform.MacOS, client)
        }
    }
}
