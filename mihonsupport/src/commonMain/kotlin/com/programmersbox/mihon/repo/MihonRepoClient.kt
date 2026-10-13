package com.programmersbox.mihon.repo

import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSources
import com.programmersbox.mihon.MihonLib
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile

class MihonRepoClient(private val httpClient: HttpClient) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val mutex = Mutex()

    /** Repo base URL -> signing key fingerprint. Guarded by [mutex]. */
    private var keysByRepo: Map<String, String> = emptyMap()

    /** Immutable copy of the fingerprints in [keysByRepo], readable without suspending. */
    @Volatile
    private var keySnapshot: Set<String> = emptySet()

    /**
     * Signing key fingerprints for the repos in the most recent [fetch] call. A repo that failed
     * in that call keeps its previously cached key; repos not in that call are dropped.
     */
    val signingKeys: Set<String> get() = keySnapshot

    suspend fun fetch(urls: List<String>): List<KmpRemoteSources> {
        val bases = urls.map(::normalize).filter { it.isNotEmpty() }.distinct().filter {
            // Plain http would let a network attacker inject a signing key that marks extensions trusted.
            it.startsWith("https://", ignoreCase = true).also { ok -> if (!ok) println("Ignoring non-https Mihon repo: $it") }
        }
        val results = coroutineScope {
            bases.map { base -> async { fetchRepo(base) } }.awaitAll()
        }
        mutex.withLock {
            val updated = mutableMapOf<String, String>()
            bases.forEachIndexed { index, base ->
                val key = results[index]?.fingerprint ?: keysByRepo[base]
                if (key != null) updated[base] = key
            }
            keysByRepo = updated
            keySnapshot = updated.values.toSet()
        }
        return results.filterNotNull().flatMap { it.sources }
    }

    private data class RepoFetch(val fingerprint: String, val sources: List<KmpRemoteSources>)

    /** Returns null when the repo could not be fetched or parsed. Cancellation is rethrown. */
    private suspend fun fetchRepo(base: String): RepoFetch? = try {
        val repo = json.decodeFromString<MihonRepo>(getText("$base/repo.json"))
        val index = json.decodeFromString<List<MihonIndexEntry>>(getText("$base/index.min.json"))
        RepoFetch(
            fingerprint = repo.meta.signingKeyFingerprint,
            sources = index
                .filter { MihonLib.isSupported(MihonLib.libVersion(it.version)) }
                .map { it.toRemote(base) },
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.printStackTrace()
        null
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
