package com.programmersbox.cloudstream

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageData
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * Service names for the APIs of all loaded plugins: `cloudstream:<api name>`, or
 * `cloudstream:<internalName>:<api name>` when two APIs share a name.
 */
fun serviceNamesFor(apis: List<Pair<String, MainAPI>>): List<String> {
    val counts = apis.groupingBy { it.second.name }.eachCount()
    return apis.map { (internalName, api) ->
        if (counts.getValue(api.name) > 1) "cloudstream:$internalName:${api.name}" else "cloudstream:${api.name}"
    }
}

open class CloudstreamApiService(
    val api: MainAPI,
    val internalName: String,
    override val serviceName: String,
) : KmpApiService {
    override val baseUrl: String get() = api.mainUrl
    override val canScroll: Boolean = true
    override val canDownload: Boolean = true
    override val displayName: String get() = "${api.name} (${api.lang})"
    override val language: String get() = api.lang

    override suspend fun recent(page: Int): List<KmpItemModel> = withContext(Dispatchers.IO) {
        if (!hasHomePage()) return@withContext emptyList()
        mainPage(api.mainPage.first(), page)
    }

    /** Every main page section; a failing section is skipped unless all of them fail. */
    override suspend fun allList(page: Int): List<KmpItemModel> = withContext(Dispatchers.IO) {
        if (!hasHomePage()) return@withContext emptyList()
        var failure: Exception? = null
        val items = api.mainPage.flatMap { section ->
            try {
                mainPage(section, page)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Cloudstream ${api.name}: main page '${section.name}' failed: $e")
                failure = failure ?: e
                emptyList()
            }
        }
        failure?.takeIf { items.isEmpty() }?.let { throw it }
        items.distinctBy { it.url }
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<KmpItemModel>): List<KmpItemModel> =
        withContext(Dispatchers.IO) {
            // The default search(query, page) falls back to search(query).
            val result = plugin { api.search(searchText.toString(), page) } ?: return@withContext emptyList()
            CloudstreamMapper.toItems(result.items, result.hasNext, this@CloudstreamApiService)
        }

    override suspend fun itemInfo(model: KmpItemModel): KmpInfoModel = withContext(Dispatchers.IO) {
        val response = plugin { api.load(model.url) } ?: error("${api.name} could not load ${model.url}")
        CloudstreamMapper.toInfo(response, model, this@CloudstreamApiService)
    }

    override suspend fun chapterInfo(chapterModel: KmpChapterModel): List<KmpStorage> = withContext(Dispatchers.IO) {
        val data = chapterModel.extras[CloudstreamMapper.CS_DATA] as? String ?: chapterModel.url
        // Plugins call back from several coroutines at once.
        val subtitles = Collections.synchronizedList(mutableListOf<SubtitleFile>())
        val links = Collections.synchronizedList(mutableListOf<ExtractorLink>())
        try {
            plugin { api.loadLinks(data, false, { subtitles += it }, { links += it }) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep whatever was found before the failure.
            println("Cloudstream ${api.name}: loadLinks failed after ${links.size} links: $e")
        }
        val allSubtitles = synchronized(subtitles) { subtitles.toList() }
        synchronized(links) { links.toList() }.mapNotNull { CloudstreamMapper.toStorage(it, allSubtitles) }
    }

    override suspend fun sourceByUrl(url: String): KmpItemModel = withContext(Dispatchers.IO) {
        val response = plugin { api.load(url) } ?: error("${api.name} could not load $url")
        CloudstreamMapper.toItem(response, this@CloudstreamApiService)
    }

    /** Search-only plugins keep upstream's placeholder `mainPage` and a throwing `getMainPage`. */
    private fun hasHomePage() = api.hasMainPage && api.mainPage.isNotEmpty()

    private suspend fun mainPage(section: MainPageData, page: Int): List<KmpItemModel> {
        val request = MainPageRequest(section.name, section.data, horizontalImages = false)
        val response = plugin { api.getMainPage(page, request) } ?: return emptyList()
        return CloudstreamMapper.toItems(response.items.flatMap { it.list }, response.hasNext, this)
    }

    /**
     * Plugins throw [Error]s too (NotImplementedError, NoClassDefFoundError for a missing shim).
     * Callers only catch [Exception], so wrap those.
     */
    private inline fun <T> plugin(block: () -> T): T =
        try {
            block()
        } catch (e: VirtualMachineError) {
            throw e
        } catch (e: Error) {
            throw IllegalStateException("${api.name}: $e", e)
        }
}
