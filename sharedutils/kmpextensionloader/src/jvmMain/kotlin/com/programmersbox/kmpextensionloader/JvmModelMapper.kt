package com.programmersbox.kmpextensionloader

import android.app.Application
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpApiServicesCatalog
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpExternalApiServicesCatalog
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.KmpSources
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.models.ApiService
import com.programmersbox.models.ApiServicesCatalog
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.ExternalApiServicesCatalog
import com.programmersbox.models.ExternalCustomApiServicesCatalog
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.RemoteSources
import com.programmersbox.models.SourceInformation
import com.programmersbox.models.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JvmModelMapper(private val mockApplication: Application) {

    fun mapSourceInformation(s: SourceInformation): KmpSourceInformation = KmpSourceInformation(
        apiService = mapApiService(s.apiService),
        name = s.name,
        icon = null,
        packageName = s.packageName,
        catalog = s.catalog?.let { mapCatalog(it) }
    )

    fun mapCatalog(catalog: ApiServicesCatalog): KmpApiServicesCatalog = when (catalog) {
        is ExternalApiServicesCatalog -> object : KmpExternalApiServicesCatalog {
            override val hasRemoteSources = catalog.hasRemoteSources
            override val name = catalog.name
            override suspend fun initialize() = catalog.initialize(mockApplication)
            override fun getSources() = catalog.getSources().map { mapSourceInformation(it) }
            override fun createSources() = catalog.createSources().map { mapApiService(it) }
            override suspend fun getRemoteSources() = catalog.getRemoteSources().map { mapRemoteSources(it) }
            override fun shouldReload(packageName: String) = false
        }
        is ExternalCustomApiServicesCatalog -> object : KmpExternalCustomApiServicesCatalog {
            override val hasRemoteSources = catalog.hasRemoteSources
            override val name = catalog.name
            override suspend fun initialize() = catalog.initialize(mockApplication)
            override fun getSources() = catalog.getSources().map { mapSourceInformation(it) }
            override fun createSources() = catalog.createSources().map { mapApiService(it) }
            override suspend fun getRemoteSources(customUrls: List<String>) =
                catalog.getRemoteSources(customUrls).map { mapRemoteSources(it) }
            override fun shouldReload(packageName: String) = false
        }
        else -> object : KmpApiServicesCatalog {
            override val name = catalog.name
            override fun createSources() = catalog.createSources().map { mapApiService(it) }
        }
    }

    fun mapRemoteSources(r: RemoteSources): KmpRemoteSources = KmpRemoteSources(
        name = r.name, packageName = r.packageName, version = r.version,
        iconUrl = r.iconUrl, downloadLink = r.downloadLink,
        sources = r.sources.map { KmpSources(it.name, it.baseUrl, it.version) }
    )

    fun mapApiService(service: ApiService): KmpApiService = object : KmpApiService {
        override val baseUrl = service.baseUrl
        override val websiteUrl = service.websiteUrl
        override val canScroll = service.canScroll
        override val canScrollAll = service.canScrollAll
        override val canPlay = service.canPlay
        override val canDownload = service.canDownload
        override val notWorking = service.notWorking
        override val serviceName = service.serviceName

        override fun getRecentFlow(page: Int): Flow<List<KmpItemModel>> =
            service.getRecentFlow(page).map { list -> list.map { mapItemModel(it) } }
        override suspend fun recent(page: Int) = service.recent(page).map { mapItemModel(it) }
        override fun getListFlow(page: Int): Flow<List<KmpItemModel>> =
            service.getListFlow(page).map { list -> list.map { mapItemModel(it) } }
        override suspend fun allList(page: Int) = service.allList(page).map { mapItemModel(it) }
        override fun getItemInfoFlow(model: KmpItemModel): Flow<Result<KmpInfoModel>> =
            service.getItemInfoFlow(reverseMapItemModel(model)).map { it.map { mapInfoModel(it) } }
        override suspend fun itemInfo(model: KmpItemModel) =
            mapInfoModel(service.itemInfo(reverseMapItemModel(model)))
        override fun getChapterInfoFlow(chapterModel: KmpChapterModel): Flow<List<KmpStorage>> =
            service.getChapterInfoFlow(reverseMapChapterModel(chapterModel)).map { list -> list.map { mapStorage(it) } }
        override suspend fun chapterInfo(chapterModel: KmpChapterModel) =
            service.chapterInfo(reverseMapChapterModel(chapterModel)).map { mapStorage(it) }
        override fun getSourceByUrlFlow(url: String): Flow<KmpItemModel> =
            service.getSourceByUrlFlow(url).map { mapItemModel(it) }
        override suspend fun sourceByUrl(url: String) = mapItemModel(service.sourceByUrl(url))
        override suspend fun search(searchText: CharSequence, page: Int, list: List<KmpItemModel>) =
            service.search(searchText, page, list.map { reverseMapItemModel(it) }).map { mapItemModel(it) }
        override fun searchListFlow(searchText: CharSequence, page: Int, list: List<KmpItemModel>): Flow<List<KmpItemModel>> =
            service.searchListFlow(searchText, page, list.map { reverseMapItemModel(it) })
                .map { l -> l.map { mapItemModel(it) } }
        override fun searchSourceList(searchText: CharSequence, page: Int, list: List<KmpItemModel>): Flow<List<KmpItemModel>> =
            service.searchSourceList(searchText, page, list.map { reverseMapItemModel(it) })
                .map { l -> l.map { mapItemModel(it) } }
    }

    private fun mapItemModel(m: ItemModel): KmpItemModel = KmpItemModel(
        title = m.title, description = m.description, url = m.url,
        imageUrl = m.imageUrl, source = mapApiService(m.source)
    )

    private fun reverseMapItemModel(m: KmpItemModel): ItemModel = ItemModel(
        title = m.title, description = m.description, url = m.url,
        imageUrl = m.imageUrl, source = reverseMapApiService(m.source)
    )

    private fun mapInfoModel(m: InfoModel): KmpInfoModel = KmpInfoModel(
        title = m.title, description = m.description, url = m.url, imageUrl = m.imageUrl,
        chapters = m.chapters.map { mapChapterModel(it) },
        genres = m.genres, alternativeNames = m.alternativeNames,
        source = mapApiService(m.source)
    )

    private fun mapChapterModel(m: ChapterModel): KmpChapterModel = KmpChapterModel(
        name = m.name, url = m.url, uploaded = m.uploaded,
        sourceUrl = m.sourceUrl, source = mapApiService(m.source)
    )

    private fun reverseMapChapterModel(m: KmpChapterModel): ChapterModel = ChapterModel(
        name = m.name, url = m.url, uploaded = m.uploaded,
        sourceUrl = m.sourceUrl, source = reverseMapApiService(m.source)
    )

    private fun mapStorage(s: Storage): KmpStorage = KmpStorage(
        sub = s.sub, source = s.source, link = s.link, quality = s.quality, filename = s.filename
    )

    private fun reverseMapApiService(service: KmpApiService): ApiService = object : ApiService {
        override val baseUrl = service.baseUrl
        override val websiteUrl = service.websiteUrl
        override val canScroll = service.canScroll
        override val canScrollAll = service.canScrollAll
        override val canPlay = service.canPlay
        override val canDownload = service.canDownload
        override val notWorking = service.notWorking
        override val serviceName = service.serviceName
        override fun getRecentFlow(page: Int): Flow<List<ItemModel>> =
            service.getRecentFlow(page).map { list -> list.map { reverseMapItemModel(it) } }
        override suspend fun recent(page: Int) = service.recent(page).map { reverseMapItemModel(it) }
        override fun getListFlow(page: Int): Flow<List<ItemModel>> =
            service.getListFlow(page).map { list -> list.map { reverseMapItemModel(it) } }
        override suspend fun allList(page: Int) = service.allList(page).map { reverseMapItemModel(it) }
    }
}
