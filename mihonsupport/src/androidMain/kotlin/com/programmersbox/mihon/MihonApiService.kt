package com.programmersbox.mihon

import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpConfigurableApiService
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.ResolvableSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun mihonApiService(source: CatalogueSource): MihonApiService =
    if (source is ConfigurableSource) ConfigurableMihonApiService(source) else MihonApiService(source)

open class MihonApiService(val source: CatalogueSource) : KmpApiService {
    override val baseUrl: String = (source as? HttpSource)?.baseUrl.orEmpty()
    override val serviceName: String = "mihon:${source.id}"
    override val canScroll: Boolean = true
    val displayName: String = "${source.name} (${source.lang})"

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
        pages.map { page ->
            val imageUrl = page.imageUrl
                ?: (source as? HttpSource)?.getImageUrl(page)?.also { page.imageUrl = it }
                ?: page.url
            MihonSourceMapper.toStorage(page, imageUrl, source)
        }
    }

    override suspend fun sourceByUrl(url: String): KmpItemModel = withContext(Dispatchers.IO) {
        val resolvable = source as? ResolvableSource ?: error("${source.name} cannot open URLs")
        val manga = resolvable.getManga(url) ?: error("${source.name} could not resolve $url")
        MihonSourceMapper.toItem(manga, this@MihonApiService)
    }
}

class ConfigurableMihonApiService(source: CatalogueSource) : MihonApiService(source), KmpConfigurableApiService
