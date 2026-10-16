package com.programmersbox.cloudstream.repo

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private const val REPO_URL = "https://repo.example/repo.json"
private const val LIST_URL = "https://repo.example/plugins.json"
private const val BUILDS = "https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/builds"

// Trimmed from phisher98/cloudstream-extensions-phisher builds/repo.json.
private const val REPO = """{"name":"Phisher Repo","iconUrl":"https://example/RepoIcon.png","description":"Phisher Repository",
 "manifestVersion":1,"pluginLists":["$LIST_URL"]}"""

// Trimmed from the matching plugins.json: Anime, non-anime, "All", status 0 (synthesized), %size% icon (synthesized), no fileHash.
private const val PLUGINS = """[
 {"url":"$BUILDS/Anichi.cs3","status":1,"version":29,"name":"Anichi","internalName":"Anichi","authors":["Hexated,Phisher98"],
  "description":"AllManga/Mkissa Source","fileSize":209794,"repositoryUrl":"https://github.com/phisher98/cloudstream-extensions-phisher",
  "language":"en","tvTypes":["AnimeMovie","Anime","OVA"],"iconUrl":"https://example/Allanime.png","apiVersion":1,
  "fileHash":"sha256-2f05a1c836915a0afb5b467d65e067ade30d970f4410dc183df8a72d3ea8d2aa"},
 {"url":"$BUILDS/AllMovieLandProvider.cs3","status":1,"version":25,"name":"AllMovieLandProvider","internalName":"AllMovieLandProvider",
  "authors":["Phisher98"],"language":"hi","tvTypes":["Movie","TvSeries","Cartoon"],"apiVersion":1,"fileHash":"sha256-12"},
 {"url":"$BUILDS/AllWish.cs3","status":1,"version":18,"name":"AllWish","internalName":"AllWish","language":"en","tvTypes":["All"],"apiVersion":1},
 {"url":"$BUILDS/Dead.cs3","status":0,"version":3,"name":"Dead","internalName":"Dead","language":"en","tvTypes":["Anime"],"apiVersion":1},
 {"url":"$BUILDS/Sized.cs3","status":1,"version":4,"name":"Sized","internalName":"Sized","language":"ja","tvTypes":["OVA"],
  "iconUrl":"https://example/icon-%size%.png","apiVersion":1}
]"""

private const val ANIYOMI_REPO = """{"meta":{"name":"Yuzono","signingKeyFingerprint":"abc123"}}"""

class CloudstreamRepoClientTest {
    private fun client(
        repo: String = REPO,
        failHost: String? = null,
        failList: Boolean = false,
    ) = HttpClient(MockEngine { request ->
        val url = request.url.toString()
        when {
            failHost != null && url.contains(failHost) -> respondError(HttpStatusCode.NotFound)
            url.endsWith("/repo.json") -> respond(repo)
            url == LIST_URL && failList -> respondError(HttpStatusCode.InternalServerError)
            url == LIST_URL -> respond(PLUGINS)
            else -> respondError(HttpStatusCode.NotFound)
        }
    })

    @Test
    fun filtersToAnimeTypes() = runTest {
        val result = CloudstreamRepoClient(client()).fetch(listOf("https://repo.example"))
        assertEquals(listOf("Anichi", "Sized"), result.map { it.name })
        val anichi = result.first()
        assertEquals("Anichi", anichi.packageName)
        assertEquals("29", anichi.version)
        assertEquals("en", anichi.lang)
        assertEquals("$BUILDS/Anichi.cs3", anichi.downloadLink)
        assertEquals("https://example/Allanime.png", anichi.iconUrl)
        assertEquals("https://github.com/phisher98/cloudstream-extensions-phisher", anichi.sources.single().baseUrl)
    }

    @Test
    fun dropsStatusZero() = runTest {
        val result = CloudstreamRepoClient(client()).fetch(listOf("https://repo.example"))
        assertEquals(false, result.any { it.name == "Dead" })
    }

    @Test
    fun iconSizeReplaced() = runTest {
        val sized = CloudstreamRepoClient(client()).fetch(listOf("https://repo.example")).first { it.name == "Sized" }
        assertEquals("https://example/icon-128.png", sized.iconUrl)
        assertEquals("", sized.sources.single().baseUrl)
    }

    @Test
    fun skipsAniyomiRepo() = runTest {
        val repo = CloudstreamRepoClient(client(repo = ANIYOMI_REPO))
        assertEquals(emptyList(), repo.fetch(listOf("https://repo.example")))
        assertNull(repo.pluginFor("$BUILDS/Anichi.cs3"))
    }

    @Test
    fun failingPluginListSkipped() = runTest {
        assertEquals(emptyList(), CloudstreamRepoClient(client(failList = true)).fetch(listOf("https://repo.example")))
        val result = CloudstreamRepoClient(client(failHost = "bad.example"))
            .fetch(listOf("https://bad.example", "https://repo.example"))
        assertEquals(listOf("Anichi", "Sized"), result.map { it.name })
    }

    @Test
    fun httpRepoIsIgnored() = runTest {
        assertEquals(emptyList(), CloudstreamRepoClient(client()).fetch(listOf("http://repo.example")))
    }

    @Test
    fun pluginForFindsHash() = runTest {
        val repo = CloudstreamRepoClient(client())
        repo.fetch(listOf("https://repo.example/"))
        val (repoUrl, plugin) = assertNotNull(repo.pluginFor("$BUILDS/Anichi.cs3"))
        assertEquals(REPO_URL, repoUrl)
        assertEquals("sha256-2f05a1c836915a0afb5b467d65e067ade30d970f4410dc183df8a72d3ea8d2aa", plugin.fileHash)
        assertNull(repo.pluginFor("$BUILDS/Dead.cs3"))
    }

    @Test
    fun normalizesRepoUrl() {
        listOf("https://repo.example", "https://repo.example/", " https://repo.example/repo.json ").forEach {
            assertEquals(REPO_URL, CloudstreamRepoClient.normalize(it), it)
        }
    }
}
