package com.programmersbox.aniyomi.repo

import com.programmersbox.kmpmodels.RepoCheck
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val CHECK_REPO = """{"meta":{"name":"Kohi-den","signingKeyFingerprint":"abc123"}}"""

private fun entries(vararg versions: String) = versions.withIndex().joinToString(",", "[", "]") { (i, v) ->
    """{"name":"Aniyomi: E$i","pkg":"a.b$i","apk":"e$i.apk","lang":"en","code":1,"version":"$v","nsfw":0,"sources":[]}"""
}

class AniyomiRepoClientCheckTest {
    private fun client(
        repo: String = CHECK_REPO,
        index: String = entries("14.12", "16.3"),
        failAll: Boolean = false,
    ) = HttpClient(MockEngine { request ->
        when {
            failAll -> respondError(HttpStatusCode.NotFound)
            request.url.encodedPath.endsWith("/repo.json") -> respond(repo)
            request.url.encodedPath.endsWith("/index.min.json") -> respond(index)
            else -> respondError(HttpStatusCode.NotFound)
        }
    })

    @Test
    fun checkFoundReturnsRepoName() = runTest {
        val found = assertIs<RepoCheck.Found>(AniyomiRepoClient(client()).check("https://repo.example"))
        assertEquals("Kohi-den", found.name)
        assertEquals(2, found.remotes.size)
    }

    @Test
    fun checkMihonRepoIsNotThisFormat() = runTest {
        val http = client(index = entries("1.4.190", "1.4.12"))
        assertEquals(RepoCheck.NotThisFormat, AniyomiRepoClient(http).check("https://repo.example"))
    }

    @Test
    fun checkCloudstreamRepoIsNotThisFormat() = runTest {
        val http = client(repo = """{"name":"X","manifestVersion":1,"pluginLists":[]}""")
        assertEquals(RepoCheck.NotThisFormat, AniyomiRepoClient(http).check("https://repo.example"))
    }

    @Test
    fun checkNonObjectRepoJsonIsNotThisFormat() = runTest {
        val http = client(repo = "[]")
        assertEquals(RepoCheck.NotThisFormat, AniyomiRepoClient(http).check("https://repo.example"))
    }

    @Test
    fun check404IsFailed() = runTest {
        val failed = assertIs<RepoCheck.Failed>(AniyomiRepoClient(client(failAll = true)).check("https://repo.example"))
        assertTrue("404" in failed.message, failed.message)
    }

    @Test
    fun checkHttpIsFailed() = runTest {
        assertEquals(
            RepoCheck.Failed("Only https links are supported"),
            AniyomiRepoClient(client()).check("http://repo.example"),
        )
    }

    @Test
    fun checkDoesNotChangeSigningKeys() = runTest {
        val repo = AniyomiRepoClient(client())
        repo.check("https://repo.example")
        assertEquals(emptySet(), repo.signingKeys)
        assertEquals(emptyMap(), repo.signingKeysByRepo)
    }
}
