package com.programmersbox.mihon.repo

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val REPO = """{"meta":{"name":"Keiyoushi","shortName":"Keiyoushi","website":"https://keiyoushi.github.io","signingKeyFingerprint":"abc123"}}"""

private const val INDEX = """[
 {"name":"Tachiyomi: MangaDex","pkg":"eu.kanade.tachiyomi.extension.all.mangadex","apk":"tachiyomi-all.mangadex-v1.4.190.apk","lang":"all","code":190,"version":"1.4.190","nsfw":1,
  "sources":[{"name":"MangaDex","lang":"en","id":"2499283573021220255","baseUrl":"https://mangadex.org"}]},
 {"name":"Tachiyomi: Old","pkg":"eu.kanade.tachiyomi.extension.en.old","apk":"old-v1.3.5.apk","lang":"en","code":5,"version":"1.3.5","nsfw":0,
  "sources":[{"name":"Old","lang":"en","id":1,"baseUrl":"https://old.example"}]},
 {"name":"Tachiyomi: Mid","pkg":"eu.kanade.tachiyomi.extension.en.mid","apk":"mid-v1.5.1.apk","lang":"en","code":1,"version":"1.5.1","nsfw":0,
  "sources":[{"name":"Mid","lang":"en","id":2,"baseUrl":"https://mid.example"}]}
]"""

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
        assertEquals("MangaDex (18+)", dex.name)
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
}
