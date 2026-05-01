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

    // Fix 1: Named inner class wrapping ApiService → KmpApiService
    private inner class WrappedApiService(val delegate: ApiService) : KmpApiService {
        override val baseUrl = delegate.baseUrl
        override val websiteUrl = delegate.websiteUrl
        override val canScroll = delegate.canScroll
        override val canScrollAll = delegate.canScrollAll
        override val canPlay = delegate.canPlay
        override val canDownload = delegate.canDownload
        override val notWorking = delegate.notWorking
        override val serviceName = delegate.serviceName

        override fun getRecentFlow(page: Int): Flow<List<KmpItemModel>> =
            delegate.getRecentFlow(page).map { list -> list.map { mapItemModel(it) } }
        override suspend fun recent(page: Int) = delegate.recent(page).map { mapItemModel(it) }
        override fun getListFlow(page: Int): Flow<List<KmpItemModel>> =
            delegate.getListFlow(page).map { list -> list.map { mapItemModel(it) } }
        override suspend fun allList(page: Int) = delegate.allList(page).map { mapItemModel(it) }
        override fun getItemInfoFlow(model: KmpItemModel): Flow<Result<KmpInfoModel>> =
            delegate.getItemInfoFlow(reverseMapItemModel(model)).map { it.map { mapInfoModel(it) } }
        override suspend fun itemInfo(model: KmpItemModel) =
            mapInfoModel(delegate.itemInfo(reverseMapItemModel(model)))
        override fun getChapterInfoFlow(chapterModel: KmpChapterModel): Flow<List<KmpStorage>> =
            delegate.getChapterInfoFlow(reverseMapChapterModel(chapterModel)).map { list -> list.map { mapStorage(it) } }
        override suspend fun chapterInfo(chapterModel: KmpChapterModel) =
            delegate.chapterInfo(reverseMapChapterModel(chapterModel)).map { mapStorage(it) }
        override fun getSourceByUrlFlow(url: String): Flow<KmpItemModel> =
            delegate.getSourceByUrlFlow(url).map { mapItemModel(it) }
        override suspend fun sourceByUrl(url: String) = mapItemModel(delegate.sourceByUrl(url))
        override suspend fun search(searchText: CharSequence, page: Int, list: List<KmpItemModel>) =
            delegate.search(searchText, page, list.map { reverseMapItemModel(it) }).map { mapItemModel(it) }
        override fun searchListFlow(searchText: CharSequence, page: Int, list: List<KmpItemModel>): Flow<List<KmpItemModel>> =
            delegate.searchListFlow(searchText, page, list.map { reverseMapItemModel(it) })
                .map { l -> l.map { mapItemModel(it) } }
        override fun searchSourceList(searchText: CharSequence, page: Int, list: List<KmpItemModel>): Flow<List<KmpItemModel>> =
            delegate.searchSourceList(searchText, page, list.map { reverseMapItemModel(it) })
                .map { l -> l.map { mapItemModel(it) } }
    }

    // Fix 1: Named inner class wrapping KmpApiService → ApiService
    private inner class WrappedKmpApiService(val delegate: KmpApiService) : ApiService {
        override val baseUrl = delegate.baseUrl
        override val websiteUrl = delegate.websiteUrl
        override val canScroll = delegate.canScroll
        override val canScrollAll = delegate.canScrollAll
        override val canPlay = delegate.canPlay
        override val canDownload = delegate.canDownload
        override val notWorking = delegate.notWorking
        override val serviceName = delegate.serviceName
        override fun getRecentFlow(page: Int): Flow<List<ItemModel>> =
            delegate.getRecentFlow(page).map { list -> list.map { reverseMapItemModel(it) } }
        override suspend fun recent(page: Int) = delegate.recent(page).map { reverseMapItemModel(it) }
        override fun getListFlow(page: Int): Flow<List<ItemModel>> =
            delegate.getListFlow(page).map { list -> list.map { reverseMapItemModel(it) } }
        override suspend fun allList(page: Int) = delegate.allList(page).map { reverseMapItemModel(it) }
        override fun getItemInfoFlow(model: ItemModel): Flow<Result<InfoModel>> =
            delegate.getItemInfoFlow(mapItemModel(model)).map { it.map { reverseMapInfoModel(it) } }
        override suspend fun itemInfo(model: ItemModel) =
            reverseMapInfoModel(delegate.itemInfo(mapItemModel(model)))
        override fun getChapterInfoFlow(chapterModel: ChapterModel): Flow<List<Storage>> =
            delegate.getChapterInfoFlow(mapChapterModel(chapterModel)).map { list -> list.map { reverseMapStorage(it) } }
        override suspend fun chapterInfo(chapterModel: ChapterModel) =
            delegate.chapterInfo(mapChapterModel(chapterModel)).map { reverseMapStorage(it) }
        override fun getSourceByUrlFlow(url: String): Flow<ItemModel> =
            delegate.getSourceByUrlFlow(url).map { reverseMapItemModel(it) }
        override suspend fun sourceByUrl(url: String) = reverseMapItemModel(delegate.sourceByUrl(url))
        override suspend fun search(searchText: CharSequence, page: Int, list: List<ItemModel>) =
            delegate.search(searchText, page, list.map { mapItemModel(it) }).map { reverseMapItemModel(it) }
        override fun searchListFlow(searchText: CharSequence, page: Int, list: List<ItemModel>): Flow<List<ItemModel>> =
            delegate.searchListFlow(searchText, page, list.map { mapItemModel(it) })
                .map { l -> l.map { reverseMapItemModel(it) } }
        override fun searchSourceList(searchText: CharSequence, page: Int, list: List<ItemModel>): Flow<List<ItemModel>> =
            delegate.searchSourceList(searchText, page, list.map { mapItemModel(it) })
                .map { l -> l.map { reverseMapItemModel(it) } }
    }

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

    // Fix 1: Identity-check prevents re-wrapping an already-wrapped service
    fun mapApiService(service: ApiService): KmpApiService =
        (service as? WrappedKmpApiService)?.delegate ?: WrappedApiService(service)

    private fun reverseMapApiService(service: KmpApiService): ApiService =
        (service as? WrappedApiService)?.delegate ?: WrappedKmpApiService(service)

    // Fix 2: Copy extras/otherExtras through all map functions
    private fun mapItemModel(m: ItemModel): KmpItemModel = KmpItemModel(
        title = m.title, description = m.description, url = m.url,
        imageUrl = m.imageUrl, source = mapApiService(m.source)
    ).also {
        it.extras.putAll(m.extras)
        it.otherExtras.putAll(m.otherExtras)
    }

    private fun reverseMapItemModel(m: KmpItemModel): ItemModel = ItemModel(
        title = m.title, description = m.description, url = m.url,
        imageUrl = m.imageUrl, source = reverseMapApiService(m.source)
    ).also {
        it.extras.putAll(m.extras)
        it.otherExtras.putAll(m.otherExtras)
    }

    private fun mapInfoModel(m: InfoModel): KmpInfoModel = KmpInfoModel(
        title = m.title, description = m.description, url = m.url, imageUrl = m.imageUrl,
        chapters = m.chapters.map { mapChapterModel(it) },
        genres = m.genres, alternativeNames = m.alternativeNames,
        source = mapApiService(m.source)
    ).also { it.extras.putAll(m.extras) }

    // Fix 2: Added reverseMapInfoModel (was missing)
    private fun reverseMapInfoModel(m: KmpInfoModel): InfoModel = InfoModel(
        title = m.title, description = m.description, url = m.url, imageUrl = m.imageUrl,
        chapters = m.chapters.map { reverseMapChapterModel(it) },
        genres = m.genres, alternativeNames = m.alternativeNames,
        source = reverseMapApiService(m.source)
    ).also { it.extras.putAll(m.extras) }

    private fun mapChapterModel(m: ChapterModel): KmpChapterModel = KmpChapterModel(
        name = m.name, url = m.url, uploaded = m.uploaded,
        sourceUrl = m.sourceUrl, source = mapApiService(m.source)
    ).also {
        it.uploadedTime = m.uploadedTime
        it.extras.putAll(m.extras)
        it.otherExtras.putAll(m.otherExtras)
    }

    private fun reverseMapChapterModel(m: KmpChapterModel): ChapterModel = ChapterModel(
        name = m.name, url = m.url, uploaded = m.uploaded,
        sourceUrl = m.sourceUrl, source = reverseMapApiService(m.source)
    ).also {
        it.uploadedTime = m.uploadedTime
        it.extras.putAll(m.extras)
        it.otherExtras.putAll(m.otherExtras)
    }

    private fun mapStorage(s: Storage): KmpStorage = KmpStorage(
        sub = s.sub, source = s.source, link = s.link, quality = s.quality, filename = s.filename
    ).also { it.headers.putAll(s.headers) }

    private fun reverseMapStorage(s: KmpStorage): Storage = Storage(
        sub = s.sub, source = s.source, link = s.link, quality = s.quality, filename = s.filename
    ).also { it.headers.putAll(s.headers) }
}
