package com.programmersbox.mihon.repo

import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSources
import com.programmersbox.mihon.MihonLib
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.concurrent.Volatile

@OptIn(ExperimentalSerializationApi::class)
class MihonRepoClient(private val httpClient: HttpClient) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** Lenient decoder for proto3-JSON index_v2 payloads (quoted int64, enum names, unknown enum values). */
    private val v2Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
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
        val indexV2 = repo.indexV2
        if (indexV2 != null) fetchIndexV2(repo, indexV2) else fetchLegacy(base, repo)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private suspend fun fetchLegacy(base: String, repo: MihonRepo): RepoFetch {
        val index = json.decodeFromString<List<MihonIndexEntry>>(getText("$base/index.min.json"))
        return RepoFetch(
            fingerprint = requireNotNull(repo.meta.signingKeyFingerprint) { "$base/repo.json has no signingKeyFingerprint" },
            sources = index
                .filter { MihonLib.isSupported(MihonLib.libVersion(it.version)) }
                .map { it.toRemote(base) },
        )
    }

    /** index_v2: protobuf or proto3-JSON, optionally gzipped; the extension list is inline or behind a URL. */
    private suspend fun fetchIndexV2(repo: MihonRepo, indexV2: String): RepoFetch {
        val indexBytes = getBytes(requireHttps(indexV2))
        val index = if (isJson(indexBytes)) {
            v2Json.decodeFromString<V2IndexJson>(indexBytes.decodeToString()).toIndex()
        } else {
            ProtoBuf.decodeFromByteArray<V2Index>(indexBytes)
        }
        val extensions = when (val ext = index.extensions) {
            is V2InlineList -> ext.extensionList
            is V2ListUrl -> {
                val listBytes = getBytes(requireHttps(ext.extensionListUrl))
                if (isJson(listBytes)) {
                    v2Json.decodeFromString<V2ExtensionList>(listBytes.decodeToString())
                } else {
                    ProtoBuf.decodeFromByteArray<V2ExtensionList>(listBytes)
                }
            }
            null -> V2ExtensionList()
        }
        val fingerprint = repo.meta.signingKeyFingerprint?.takeIf { it.isNotBlank() }
            ?: index.signingKey.takeIf { it.isNotBlank() }
            ?: error("$indexV2 has no signing key")
        return RepoFetch(
            fingerprint = fingerprint,
            sources = extensions.extensions
                .filter { MihonLib.isSupported(it.extensionLib.toDoubleOrNull()) }
                .map { it.toRemote() },
        )
    }

    private fun requireHttps(url: String): String = url.trim().also {
        // Same rule as repo URLs: a plain-http index could inject a signing key or APK links.
        require(it.startsWith("https://", ignoreCase = true)) { "Ignoring non-https Mihon index: $it" }
    }

    /** A proto3-JSON payload starts with `{`; a protobuf `Index`/`ExtensionList` never does (0x7b is not a valid tag here). */
    private fun isJson(bytes: ByteArray): Boolean =
        bytes.firstOrNull { it.toInt().toChar() !in " \t\r\n" } == '{'.code.toByte()

    private suspend fun getText(url: String): String {
        val response = httpClient.get(url)
        check(response.status.isSuccess()) { "GET $url -> ${response.status}" }
        return response.bodyAsText()
    }

    /** Fetches [url], gunzipping the body when it carries the gzip magic. */
    private suspend fun getBytes(url: String): ByteArray {
        val response = httpClient.get(url)
        check(response.status.isSuccess()) { "GET $url -> ${response.status}" }
        val bytes = response.readRawBytes()
        return if (isGzip(bytes)) gunzip(bytes) else bytes
    }

    private fun MihonIndexEntry.toRemote(base: String) = KmpRemoteSources(
        name = name.removePrefix("Tachiyomi: ") + if (nsfw == 1) " (18+)" else "",
        packageName = pkg,
        version = version,
        iconUrl = "$base/icon/$pkg.png",
        downloadLink = "$base/apk/$apk",
        sources = sources.map { KmpSources(name = it.name, baseUrl = it.baseUrl, version = version) },
    )

    private fun V2Extension.toRemote() = KmpRemoteSources(
        name = name + if (contentWarning == V2ContentWarning.CONTENT_WARNING_NSFW) " (18+)" else "",
        packageName = packageName,
        version = versionName,
        iconUrl = resources?.iconUrl.orEmpty(),
        downloadLink = resources?.apkUrl.orEmpty(),
        sources = sources.map { KmpSources(name = it.name, baseUrl = it.homeUrl, version = versionName) },
    )

    companion object {
        fun normalize(url: String): String = url.trim()
            .removeSuffix("/index.min.json")
            .removeSuffix("/repo.json")
            .trimEnd('/')
    }
}
