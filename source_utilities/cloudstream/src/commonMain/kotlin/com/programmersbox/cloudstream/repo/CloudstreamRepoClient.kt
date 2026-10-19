package com.programmersbox.cloudstream.repo

import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSources
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.concurrent.Volatile

class CloudstreamRepoClient(private val httpClient: HttpClient) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** downloadLink -> (repo key, plugin). Replaced wholesale by each [fetch]. */
    @Volatile
    private var pluginsByLink: Map<String, Pair<String, CsPlugin>> = emptyMap()

    /**
     * Fetches every https repo in [urls] and returns its anime plugins. A URL whose JSON has no
     * `pluginLists` (an Aniyomi repo) is skipped; a repo or plugin list that fails is skipped
     * without affecting the others. Cancellation is rethrown.
     */
    suspend fun fetch(urls: List<String>): List<KmpRemoteSources> {
        val repoUrls = urls.map(::normalize).filter { it.isNotEmpty() }.distinct().filter {
            it.startsWith("https://", ignoreCase = true).also { ok -> if (!ok) println("Ignoring non-https CloudStream repo: $it") }
        }
        val results = coroutineScope {
            repoUrls.map { repoUrl -> async { repoUrl to fetchRepo(repoUrl) } }.awaitAll()
        }
        val links = mutableMapOf<String, Pair<String, CsPlugin>>()
        val remotes = mutableListOf<KmpRemoteSources>()
        results.forEach { (repoUrl, plugins) ->
            plugins.forEach { plugin ->
                if (plugin.url !in links) {
                    links[plugin.url] = repoUrl to plugin
                    remotes += plugin.toRemote()
                }
            }
        }
        pluginsByLink = links
        return remotes
    }

    /**
     * Looks up a plugin from the last [fetch] by its `downloadLink`. The first element is the repo
     * key: the normalized `repo.json` URL (see [normalize]) the plugin came from, the same string
     * that was fetched, so it can key trust decisions.
     */
    fun pluginFor(downloadLink: String): Pair<String, CsPlugin>? = pluginsByLink[downloadLink]

    private suspend fun fetchRepo(repoUrl: String): List<CsPlugin> = try {
        val repoText = getText(repoUrl)
        // Not a CloudStream repo (an Aniyomi repo object, or an index array): skip silently.
        if ((json.parseToJsonElement(repoText) as? JsonObject)?.containsKey("pluginLists") != true) {
            emptyList()
        } else {
            val repo = json.decodeFromString<CsRepo>(repoText)
            coroutineScope {
                repo.pluginLists.map { listUrl -> async { fetchPluginList(listUrl) } }.awaitAll()
            }.flatten()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        println("CloudstreamRepoClient: $repoUrl: ${e.message}")
        emptyList()
    }

    private suspend fun fetchPluginList(listUrl: String): List<CsPlugin> = try {
        val url = listUrl.trim()
        // Plugins are downloaded from these entries and executed, so plain http is refused.
        require(url.startsWith("https://", ignoreCase = true)) { "Ignoring non-https CloudStream plugin list: $url" }
        // A cleartext .cs3 download could be swapped in transit (fileHash is optional), so drop those entries.
        json.decodeFromString<List<CsPlugin>>(getText(url))
            .filter { it.status != 0 && it.isAnime() && it.url.startsWith("https://", ignoreCase = true) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        println("CloudstreamRepoClient: $listUrl: ${e.message}")
        emptyList()
    }

    private suspend fun getText(url: String): String {
        val response = httpClient.get(url)
        check(response.status.isSuccess()) { "GET $url -> ${response.status}" }
        return response.bodyAsText()
    }

    private fun CsPlugin.isAnime() = tvTypes.orEmpty().any { it in ANIME_TYPES }

    private fun CsPlugin.toRemote() = KmpRemoteSources(
        name = name,
        packageName = internalName,
        version = version.toString(),
        iconUrl = iconUrl?.replace("%size%", "128").orEmpty(),
        downloadLink = url,
        sources = listOf(KmpSources(name, repositoryUrl.orEmpty(), version.toString())),
        lang = language.orEmpty(),
    )

    companion object {
        private val ANIME_TYPES = setOf("Anime", "AnimeMovie", "OVA")

        /**
         * Canonical repo key: the trimmed `repo.json` URL. A URL already ending in `.json` is used
         * as entered; otherwise trailing slashes are dropped and `/repo.json` appended.
         */
        fun normalize(url: String): String {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return ""
            return if (trimmed.endsWith(".json", ignoreCase = true)) trimmed else "${trimmed.trimEnd('/')}/repo.json"
        }
    }
}
