package com.programmersbox.aniyomi.repo

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val REPO = """{"meta":{"name":"Yuzono","website":"https://example","signingKeyFingerprint":"abc123"}}"""

// Trimmed from yuzono/anime-repo index.min.json, plus a lib 15 entry that must be dropped.
private const val INDEX = """[
 {"name":"Aniyomi: AnimeOnsen","pkg":"eu.kanade.tachiyomi.animeextension.all.animeonsen","apk":"aniyomi-all.animeonsen-v14.10.apk","lang":"all","code":10,"version":"14.10","nsfw":0,
  "sources":[{"name":"AnimeOnsen","lang":"all","id":"8542735178285060053","baseUrl":"https://www.animeonsen.xyz"}]},
 {"name":"Aniyomi: Lewd","pkg":"eu.kanade.tachiyomi.animeextension.en.lewd","apk":"aniyomi-en.lewd-v16.55.apk","lang":"en","code":55,"version":"16.55","nsfw":1,
  "sources":[{"name":"Lewd","lang":"en","id":"3","baseUrl":"https://lewd.example"}]},
 {"name":"Aniyomi: Old","pkg":"eu.kanade.tachiyomi.animeextension.en.old","apk":"old-v15.3.apk","lang":"en","code":3,"version":"15.3","nsfw":0,
  "sources":[{"name":"Old","lang":"en","id":"2","baseUrl":"https://old.example"}]}
]"""

private const val INDEX_V2 = """{"name":"Sec","signingKey":"v2key","extensionList":{"extensions":[
 {"name":"Aniyomi: Jelly","packageName":"eu.kanade.tachiyomi.animeextension.all.jelly","resources":{"apkUrl":"https://r.example/jelly.apk","iconUrl":"https://r.example/jelly.png"},
  "extensionLib":"16","versionCode":"7","versionName":"16.7","contentWarning":"CONTENT_WARNING_NSFW",
  "sources":[{"id":"1","name":"Jelly","language":"en"},{"id":"2","name":"Jelly","language":"ja"}]},
 {"name":"Aniyomi: Old","packageName":"eu.kanade.tachiyomi.animeextension.all.old","extensionLib":"15","versionName":"15.1"}
]}}"""

class AniyomiRepoClientTest {
    private fun client(
        repo: String = REPO,
        failHost: String? = null,
        seen: MutableList<String>? = null,
    ) = HttpClient(MockEngine { request ->
        val url = request.url.toString()
        seen?.add(url)
        when {
            failHost != null && url.contains(failHost) -> respondError(HttpStatusCode.NotFound)
            url.endsWith("/repo.json") -> respond(repo)
            url.endsWith("/index.min.json") -> respond(INDEX)
            url.endsWith("/index.json") -> respond(INDEX_V2)
            else -> respondError(HttpStatusCode.NotFound)
        }
    })

    @Test
    fun parsesLegacyIndex() = runTest {
        val repo = AniyomiRepoClient(client())
        val result = repo.fetch(listOf("https://repo.example"))

        assertEquals(listOf("AnimeOnsen", "Lewd"), result.map { it.name })
        val onsen = result.first()
        assertEquals("all", onsen.lang)
        assertEquals(false, onsen.nsfw)
        assertEquals("14.10", onsen.version)
        assertEquals("https://repo.example/icon/eu.kanade.tachiyomi.animeextension.all.animeonsen.png", onsen.iconUrl)
        assertEquals("https://repo.example/apk/aniyomi-all.animeonsen-v14.10.apk", onsen.downloadLink)
        assertEquals("https://www.animeonsen.xyz", onsen.sources.single().baseUrl)
        assertEquals(true, result.last().nsfw)
        assertEquals(setOf("abc123"), repo.signingKeys)
    }

    @Test
    fun parsesIndexV2Json() = runTest {
        val v2Repo = """{"index_v2":"https://repo.example/index.json","meta":{"name":"Sec","signingKeyFingerprint":"fp"}}"""
        val repo = AniyomiRepoClient(client(repo = v2Repo))
        val jelly = repo.fetch(listOf("https://repo.example")).single()

        assertEquals("Jelly", jelly.name)
        assertEquals("eu.kanade.tachiyomi.animeextension.all.jelly", jelly.packageName)
        assertEquals("16.7", jelly.version)
        assertEquals("all", jelly.lang)
        assertEquals(true, jelly.nsfw)
        assertEquals("https://r.example/jelly.apk", jelly.downloadLink)
        assertEquals("https://r.example/jelly.png", jelly.iconUrl)
        assertEquals(setOf("fp"), repo.signingKeys)
    }

    @Test
    fun normalizesIndexUrl() = runTest {
        val base = "https://repo.example/x"
        listOf("$base/index.min.json", "$base/repo.json", "$base/", base).forEach { url ->
            val seen = mutableListOf<String>()
            AniyomiRepoClient(client(seen = seen)).fetch(listOf(url))
            assertEquals(listOf("$base/index.min.json", "$base/repo.json"), seen.sorted(), url)
        }
    }

    @Test
    fun skipsCloudstreamRepo() = runTest {
        val cs = """{"name":"x","manifestVersion":1,"pluginLists":["https://a.example/plugins.json"]}"""
        val repo = AniyomiRepoClient(client(repo = cs))
        assertEquals(emptyList(), repo.fetch(listOf("https://repo.example")))
        assertEquals(emptySet(), repo.signingKeys)
    }

    @Test
    fun failingRepoSkipped() = runTest {
        val repo = AniyomiRepoClient(client(failHost = "bad.example"))
        val result = repo.fetch(listOf("https://bad.example", "https://good.example"))
        assertEquals(listOf("AnimeOnsen", "Lewd"), result.map { it.name })
    }

    @Test
    fun httpRepoIsIgnored() = runTest {
        val repo = AniyomiRepoClient(client())
        assertEquals(emptyList(), repo.fetch(listOf("http://repo.example")))
    }
}
