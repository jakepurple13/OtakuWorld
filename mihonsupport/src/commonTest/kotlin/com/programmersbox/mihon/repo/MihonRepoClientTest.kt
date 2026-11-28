package com.programmersbox.mihon.repo

import com.programmersbox.kmpmodels.RepoCheck
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val REPO = """{"meta":{"name":"Keiyoushi","shortName":"Keiyoushi","website":"https://keiyoushi.github.io","signingKeyFingerprint":"abc123"}}"""

private const val INDEX = """[
 {"name":"Tachiyomi: MangaDex","pkg":"eu.kanade.tachiyomi.extension.all.mangadex","apk":"tachiyomi-all.mangadex-v1.4.190.apk","lang":"all","code":190,"version":"1.4.190","nsfw":1,
  "sources":[{"name":"MangaDex","lang":"en","id":"2499283573021220255","baseUrl":"https://mangadex.org"}]},
 {"name":"Tachiyomi: Old","pkg":"eu.kanade.tachiyomi.extension.en.old","apk":"old-v1.3.5.apk","lang":"en","code":5,"version":"1.3.5","nsfw":0,
  "sources":[{"name":"Old","lang":"en","id":1,"baseUrl":"https://old.example"}]},
 {"name":"Tachiyomi: Mid","pkg":"eu.kanade.tachiyomi.extension.en.mid","apk":"mid-v1.5.1.apk","lang":"en","code":1,"version":"1.5.1","nsfw":0,
  "sources":[{"name":"Mid","lang":"en","id":2,"baseUrl":"https://mid.example"}]}
]"""

@OptIn(ExperimentalSerializationApi::class)
class MihonRepoClientTest {
    private fun client(failHost: String? = null) = HttpClient(MockEngine { request ->
        val url = request.url.toString()
        when {
            failHost != null && url.contains(failHost) -> respondError(HttpStatusCode.NotFound)
            url.endsWith("/repo.json") -> respond(REPO)
            url.endsWith("/index.min.json") -> respond(INDEX)
            else -> respondError(HttpStatusCode.NotFound)
        }
    })

    /** Per-host repo: fingerprint = [keysByHost][host]; hosts in [failing] return 500. */
    private fun perHostClient(keysByHost: Map<String, String>, failing: Set<String>) = HttpClient(MockEngine { request ->
        val host = request.url.host
        when {
            host in failing -> respondError(HttpStatusCode.InternalServerError)
            host !in keysByHost -> respondError(HttpStatusCode.NotFound)
            request.url.encodedPath.endsWith("/repo.json") ->
                respond("""{"meta":{"name":"$host","signingKeyFingerprint":"${keysByHost.getValue(host)}"}}""")
            request.url.encodedPath.endsWith("/index.min.json") -> respond(INDEX)
            else -> respondError(HttpStatusCode.NotFound)
        }
    })

    // Review Focus 2
    @Test
    fun normalizesUserUrls() {
        val base = "https://raw.githubusercontent.com/keiyoushi/extensions/repo"
        listOf(
            base, "$base/", "  $base  ", "$base/index.min.json", "$base/repo.json",
        ).forEach { assertEquals(base, MihonRepoClient.normalize(it), it) }
    }

    @Test
    fun mapsSupportedEntries() = runTest {
        val repo = MihonRepoClient(client())
        val result = repo.fetch(listOf("https://repo.example/index.min.json"))

        val dex = result.single()
        assertEquals("MangaDex", dex.name)
        assertEquals("all", dex.lang)
        assertEquals(true, dex.nsfw)
        assertEquals("eu.kanade.tachiyomi.extension.all.mangadex", dex.packageName)
        assertEquals("1.4.190", dex.version)
        assertEquals("https://repo.example/icon/eu.kanade.tachiyomi.extension.all.mangadex.png", dex.iconUrl)
        assertEquals("https://repo.example/apk/tachiyomi-all.mangadex-v1.4.190.apk", dex.downloadLink)
        assertEquals("https://mangadex.org", dex.sources.single().baseUrl)
        assertEquals(setOf("abc123"), repo.signingKeys)
    }

    @Test
    fun failingRepoDoesNotDropOthers() = runTest {
        val repo = MihonRepoClient(client(failHost = "bad.example"))
        val result = repo.fetch(listOf("https://bad.example", "https://good.example"))
        assertEquals(listOf("eu.kanade.tachiyomi.extension.all.mangadex"), result.map { it.packageName })
    }

    @Test
    fun signingKeysOnlyCoverReposInLatestFetch() = runTest {
        val repo = MihonRepoClient(perHostClient(mapOf("a.example" to "keyA", "b.example" to "keyB"), failing = emptySet()))
        repo.fetch(listOf("https://a.example", "https://b.example"))
        assertEquals(setOf("keyA", "keyB"), repo.signingKeys)

        repo.fetch(listOf("https://b.example"))
        assertEquals(setOf("keyB"), repo.signingKeys)
    }

    @Test
    fun failedRepoKeepsCachedKey() = runTest {
        val failing = mutableSetOf<String>()
        val repo = MihonRepoClient(perHostClient(mapOf("a.example" to "keyA"), failing))
        repo.fetch(listOf("https://a.example"))
        assertEquals(setOf("keyA"), repo.signingKeys)

        failing += "a.example"
        repo.fetch(listOf("https://a.example"))
        assertEquals(setOf("keyA"), repo.signingKeys)
    }

    @Test
    fun restoredKeySurvivesOfflineStart() = runTest {
        val repo = MihonRepoClient(perHostClient(emptyMap(), failing = setOf("a.example")))
        repo.restoreSigningKeys(mapOf("https://a.example" to "keyA", "https://gone.example" to "keyGone"))
        repo.fetch(listOf("https://a.example"))
        assertEquals(setOf("keyA"), repo.signingKeys)
        assertEquals(mapOf("https://a.example" to "keyA"), repo.signingKeysByRepo)
    }

    @Test
    fun restoreIsIgnoredOnceKeysAreKnown() = runTest {
        val repo = MihonRepoClient(perHostClient(mapOf("a.example" to "keyA"), failing = emptySet()))
        repo.fetch(listOf("https://a.example"))
        repo.restoreSigningKeys(mapOf("https://a.example" to "stale"))
        assertEquals(setOf("keyA"), repo.signingKeys)
    }

    @Test
    fun httpRepoIsIgnored() = runTest {
        val repo = MihonRepoClient(client())
        val result = repo.fetch(listOf("http://repo.example"))
        assertEquals(emptyList(), result)
        assertEquals(emptySet(), repo.signingKeys)
    }

    @Test
    fun httpIgnoredHttpsKept() = runTest {
        val repo = MihonRepoClient(client())
        val result = repo.fetch(listOf("http://bad.example", "https://repo.example"))
        assertEquals(1, result.size)
        assertEquals(setOf("abc123"), repo.signingKeys)
    }

    @Test
    fun checkFoundReturnsRepoNameAndSupported() = runTest {
        val result = MihonRepoClient(client()).check("https://repo.example")
        val found = assertIs<RepoCheck.Found>(result)
        assertEquals("Keiyoushi", found.name)
        assertEquals(listOf("eu.kanade.tachiyomi.extension.all.mangadex"), found.remotes.map { it.packageName })
    }

    @Test
    fun checkAniyomiRepoIsNotThisFormat() = runTest {
        val aniyomi = """[
         {"name":"Aniyomi: A","pkg":"a.b","apk":"a.apk","lang":"en","code":1,"version":"14.12","nsfw":0,"sources":[]},
         {"name":"Aniyomi: B","pkg":"a.c","apk":"b.apk","lang":"en","code":1,"version":"15.3","nsfw":0,"sources":[]}
        ]"""
        val http = HttpClient(MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/repo.json") -> respond(REPO)
                request.url.encodedPath.endsWith("/index.min.json") -> respond(aniyomi)
                else -> respondError(HttpStatusCode.NotFound)
            }
        })
        assertEquals(RepoCheck.NotThisFormat, MihonRepoClient(http).check("https://repo.example"))
    }

    @Test
    fun checkCloudstreamRepoIsNotThisFormat() = runTest {
        val http = HttpClient(MockEngine {
            respond("""{"name":"X","manifestVersion":1,"pluginLists":[]}""")
        })
        assertEquals(RepoCheck.NotThisFormat, MihonRepoClient(http).check("https://repo.example"))
    }

    @Test
    fun check404IsFailed() = runTest {
        val result = MihonRepoClient(client(failHost = "repo.example")).check("https://repo.example")
        val failed = assertIs<RepoCheck.Failed>(result)
        assertTrue("404" in failed.message, failed.message)
    }

    @Test
    fun checkHttpIsFailed() = runTest {
        assertEquals(
            RepoCheck.Failed("Only https links are supported"),
            MihonRepoClient(client()).check("http://repo.example"),
        )
    }

    @Test
    fun checkDoesNotChangeSigningKeys() = runTest {
        val repo = MihonRepoClient(client())
        repo.check("https://repo.example")
        assertEquals(emptySet(), repo.signingKeys)
        assertEquals(emptyMap(), repo.signingKeysByRepo)
    }

    @Test
    fun checkUnsignedRepoIsFailed() = runTest {
        val http = HttpClient(MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/repo.json") -> respond("""{"meta":{"name":"Unsigned"}}""")
                request.url.encodedPath.endsWith("/index.min.json") -> respond(INDEX)
                else -> respondError(HttpStatusCode.NotFound)
            }
        })
        assertEquals(RepoCheck.Failed("Repo has no signing key"), MihonRepoClient(http).check("https://repo.example"))
    }

    @Test
    fun checkNonObjectRepoJsonIsNotThisFormat() = runTest {
        val http = HttpClient(MockEngine { respond("[]") })
        assertEquals(RepoCheck.NotThisFormat, MihonRepoClient(http).check("https://repo.example"))
    }

    @Test
    fun checkIndexV2RepoIsFound() = runTest {
        val indexUrl = "https://cdn.example/index.pb"
        val index = ProtoBuf.encodeToByteArray(V2Index(name = "Store", signingKey = "indexKey", extensions = V2InlineList(V2_EXTENSIONS)))
        // No key in repo.json: the fingerprint comes from the index.
        val http = v2Client(mapOf("https://store.example/repo.json" to { repoJson(indexUrl, key = null) }, indexUrl to { index }))
        val found = assertIs<RepoCheck.Found>(MihonRepoClient(http).check("https://store.example"))
        assertEquals("Store", found.name)
        assertEquals(listOf("pkg.dex", "pkg.six"), found.remotes.map { it.packageName })
    }

    @Test
    fun checkUndecodableV2IndexIsNotThisFormat() = runTest {
        val indexUrl = "https://cdn.example/index.json"
        val http = v2Client(mapOf("https://store.example/repo.json" to { repoJson(indexUrl) }, indexUrl to { """{"name":""" }))
        assertEquals(RepoCheck.NotThisFormat, MihonRepoClient(http).check("https://store.example"))
    }
}
