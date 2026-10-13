package com.programmersbox.mihon.repo

import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSources
import com.programmersbox.mihon.MihonLib
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

class MihonRepoClient(private val httpClient: HttpClient) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val keysByRepo = mutableMapOf<String, String>()

    /** Signing key fingerprints of every repo fetched successfully at least once this session. */
    val signingKeys: Set<String> get() = keysByRepo.values.toSet()

    suspend fun fetch(urls: List<String>): List<KmpRemoteSources> = coroutineScope {
        urls.map(::normalize)
            .filter { it.isNotEmpty() }
            .distinct()
            .map { base -> async { runCatching { fetchRepo(base) }.onFailure { it.printStackTrace() }.getOrDefault(emptyList()) } }
            .awaitAll()
            .flatten()
    }

    private suspend fun fetchRepo(base: String): List<KmpRemoteSources> {
        val repo = json.decodeFromString<MihonRepo>(getText("$base/repo.json"))
        val index = json.decodeFromString<List<MihonIndexEntry>>(getText("$base/index.min.json"))
        keysByRepo[base] = repo.meta.signingKeyFingerprint
        return index
            .filter { MihonLib.isSupported(MihonLib.libVersion(it.version)) }
            .map { it.toRemote(base) }
    }

    private suspend fun getText(url: String): String {
        val response = httpClient.get(url)
        check(response.status.isSuccess()) { "GET $url -> ${response.status}" }
        return response.bodyAsText()
    }

    private fun MihonIndexEntry.toRemote(base: String) = KmpRemoteSources(
        name = name.removePrefix("Tachiyomi: ") + if (nsfw == 1) " (18+)" else "",
        packageName = pkg,
        version = version,
        iconUrl = "$base/icon/$pkg.png",
        downloadLink = "$base/apk/$apk",
        sources = sources.map { KmpSources(name = it.name, baseUrl = it.baseUrl, version = version) },
    )

    companion object {
        fun normalize(url: String): String = url.trim()
            .removeSuffix("/index.min.json")
            .removeSuffix("/repo.json")
            .trimEnd('/')
    }
}
