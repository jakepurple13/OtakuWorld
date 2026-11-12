package com.programmersbox.mihon

import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpConfigurableApiService
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.ResolvableSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private const val IMAGE_URL_PARALLELISM = 4

open class MihonApiService(val source: CatalogueSource) : KmpApiService {
    override val baseUrl: String = (source as? HttpSource)?.baseUrl.orEmpty()
    override val serviceName: String = "mihon:${source.id}"
    override val canScroll: Boolean = true
    override val displayName: String = "${source.name} (${source.lang})"
    override val language: String = source.lang

    override suspend fun recent(page: Int): List<KmpItemModel> = withContext(Dispatchers.IO) {
        val result = if (source.supportsLatest) source.getLatestUpdates(page) else source.getPopularManga(page)
        MihonSourceMapper.toItems(result, this@MihonApiService)
    }

    override suspend fun allList(page: Int): List<KmpItemModel> = withContext(Dispatchers.IO) {
        MihonSourceMapper.toItems(source.getPopularManga(page), this@MihonApiService)
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<KmpItemModel>): List<KmpItemModel> =
        withContext(Dispatchers.IO) {
            MihonSourceMapper.toItems(
                source.getSearchManga(page, searchText.toString(), source.getFilterList()),
                this@MihonApiService,
            )
        }

    override suspend fun itemInfo(model: KmpItemModel): KmpInfoModel = withContext(Dispatchers.IO) {
        val manga = MihonSourceMapper.toSManga(model, this@MihonApiService)
        val update = source.getMangaUpdate(manga, emptyList(), fetchDetails = true, fetchChapters = true)
        MihonSourceMapper.toInfo(update, manga, model, this@MihonApiService)
    }

    override suspend fun chapterInfo(chapterModel: KmpChapterModel): List<KmpStorage> = withContext(Dispatchers.IO) {
        val pages = source.getPageList(MihonSourceMapper.toSChapter(chapterModel, this@MihonApiService))
        // Pages without an image url need one request each; resolve a few at a time.
        val limit = Semaphore(IMAGE_URL_PARALLELISM)
        val resolved = coroutineScope {
            pages.map { page ->
                async {
                    if (page.imageUrl != null) return@async Result.success(page.imageUrl)
                    val http = source as? HttpSource ?: return@async Result.success(null)
                    limit.withPermit {
                        try {
                            Result.success(http.getImageUrl(page).also { page.imageUrl = it })
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                }
            }.awaitAll()
        }
        // One failed page falls back to its page url; the chapter only fails when every lookup failed.
        if (resolved.isNotEmpty() && resolved.all { it.isFailure }) throw resolved.first().exceptionOrNull()!!
        pages.mapIndexed { index, page ->
            val imageUrl = resolved[index].getOrNull() ?: page.url
            MihonSourceMapper.toStorage(page, imageUrl, source)
        }
    }

    override suspend fun sourceByUrl(url: String): KmpItemModel = withContext(Dispatchers.IO) {
        val resolvable = source as? ResolvableSource ?: error("${source.name} cannot open URLs")
        val manga = resolvable.getManga(url) ?: error("${source.name} could not resolve $url")
        MihonSourceMapper.toItem(manga, this@MihonApiService) ?: error("${source.name} returned a manga without a url for $url")
    }
}

class ConfigurableMihonApiService(source: CatalogueSource) : MihonApiService(source), KmpConfigurableApiService
