package com.programmersbox.kmpmodels

import android.app.Application
import android.content.pm.PackageInfo
import com.programmersbox.models.ApiService
import com.programmersbox.models.ApiServicesCatalog
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.ExternalApiServicesCatalog
import com.programmersbox.models.ExternalCustomApiServicesCatalog
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.RemoteSources
import com.programmersbox.models.SourceInformation
import com.programmersbox.models.Sources
import com.programmersbox.models.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ModelMapper(private val application: Application) {

    fun mapRemoteSources(remoteSources: RemoteSources): KmpRemoteSources {
        return KmpRemoteSources(
            name = remoteSources.name,
            packageName = remoteSources.packageName,
            version = remoteSources.version,
            iconUrl = remoteSources.iconUrl,
            downloadLink = remoteSources.downloadLink,
            sources = remoteSources.sources.map { mapSources(it) }
        )
    }

    fun mapRemoteSources(remoteSources: KmpRemoteSources): RemoteSources {
        return RemoteSources(
            name = remoteSources.name,
            packageName = remoteSources.packageName,
            version = remoteSources.version,
            iconUrl = remoteSources.iconUrl,
            downloadLink = remoteSources.downloadLink,
            sources = remoteSources.sources.map { mapSources(it) }
        )
    }

    fun mapSourceInformation(sourceInformation: SourceInformation): KmpSourceInformation {
        return KmpSourceInformation(
            name = sourceInformation.name,
            packageName = sourceInformation.packageName,
            icon = null,
            apiService = mapApiService(sourceInformation.apiService),
            catalog = sourceInformation.catalog?.let { mapCatalog(it) }
        )
    }

    fun mapSourceInformation(sourceInformation: KmpSourceInformation): SourceInformation {
        return SourceInformation(
            name = sourceInformation.name,
            packageName = sourceInformation.packageName,
            icon = null,
            apiService = mapApiService(sourceInformation.apiService),
            catalog = sourceInformation.catalog?.let { mapCatalog(it) }
        )
    }

    fun mapCatalog(catalog: KmpApiServicesCatalog): ApiServicesCatalog {
        return when (catalog) {
            is KmpExternalApiServicesCatalog -> object : ExternalApiServicesCatalog {
                override val hasRemoteSources: Boolean = catalog.hasRemoteSources
                override val name: String = catalog.name
                override suspend fun initialize(app: Application) = catalog.initialize()
                override fun getSources(): List<SourceInformation> = catalog.getSources()
                    .map { mapSourceInformation(it) }

                override fun createSources(): List<ApiService> = catalog.createSources().map { mapApiService(it) }

                override suspend fun getRemoteSources(): List<RemoteSources> = catalog.getRemoteSources()
                    .map { mapRemoteSources(it) }

                override fun shouldReload(packageName: String, packageInfo: PackageInfo): Boolean = false
            }

            is KmpExternalCustomApiServicesCatalog -> object : ExternalCustomApiServicesCatalog {
                override val hasRemoteSources: Boolean = catalog.hasRemoteSources
                override val name: String = catalog.name

                override suspend fun initialize(app: Application) = catalog.initialize()
                override fun getSources(): List<SourceInformation> = catalog.getSources()
                    .map { mapSourceInformation(it) }

                override fun createSources(): List<ApiService> = catalog.createSources().map { mapApiService(it) }

                override suspend fun getRemoteSources(customUrls: List<String>): List<RemoteSources> = catalog
                    .getRemoteSources(customUrls)
                    .map { mapRemoteSources(it) }

                override fun shouldReload(packageName: String, packageInfo: PackageInfo): Boolean = false
            }

            else -> object : ApiServicesCatalog {
                override fun createSources(): List<ApiService> {
                    return catalog.createSources().map { mapApiService(it) }
                }

                override val name: String = catalog.name
            }
        }
    }

    fun mapCatalog(catalog: ApiServicesCatalog): KmpApiServicesCatalog {
        return when (catalog) {
            is ExternalApiServicesCatalog -> object : KmpExternalApiServicesCatalog {
                override val hasRemoteSources: Boolean = catalog.hasRemoteSources
                override val name: String = catalog.name
                override suspend fun initialize() = catalog.initialize(application)
                override fun getSources(): List<KmpSourceInformation> = catalog.getSources()
                    .map { mapSourceInformation(it) }

                override fun createSources(): List<KmpApiService> = catalog.createSources().map { mapApiService(it) }
                override suspend fun getRemoteSources(): List<KmpRemoteSources> = catalog.getRemoteSources()
                    .map { mapRemoteSources(it) }

                override fun shouldReload(packageName: String): Boolean = false
            }

            is ExternalCustomApiServicesCatalog -> object : KmpExternalCustomApiServicesCatalog {
                override val hasRemoteSources: Boolean = catalog.hasRemoteSources
                override val name: String = catalog.name

                override suspend fun initialize() = catalog.initialize(application)
                override fun getSources(): List<KmpSourceInformation> = catalog.getSources()
                    .map { mapSourceInformation(it) }

                override fun createSources(): List<KmpApiService> = catalog.createSources().map { mapApiService(it) }

                override suspend fun getRemoteSources(customUrls: List<String>): List<KmpRemoteSources> = catalog
                    .getRemoteSources(customUrls)
                    .map { mapRemoteSources(it) }

                override fun shouldReload(packageName: String): Boolean = false
            }

            else -> object : KmpApiServicesCatalog {
                override fun createSources(): List<KmpApiService> {
                    return catalog.createSources().map { mapApiService(it) }
                }

                override val name: String = catalog.name
            }
        }
    }

    fun mapSources(sources: Sources) = KmpSources(
        name = sources.name,
        baseUrl = sources.baseUrl,
        version = sources.version
    )

    fun mapSources(sources: KmpSources) = Sources(
        name = sources.name,
        baseUrl = sources.baseUrl,
        version = sources.version
    )

    fun mapItemModel(itemModel: ItemModel): KmpItemModel {
        return KmpItemModel(
            title = itemModel.title,
            description = itemModel.description,
            url = itemModel.url,
            imageUrl = itemModel.imageUrl,
            source = mapApiService(itemModel.source)
        )
    }

    fun mapItemModel(itemModel: KmpItemModel): ItemModel {
        return ItemModel(
            title = itemModel.title,
            description = itemModel.description,
            url = itemModel.url,
            imageUrl = itemModel.imageUrl,
            source = mapApiService(itemModel.source)
        )
    }

    fun mapInfoModel(infoModel: InfoModel): KmpInfoModel {
        return KmpInfoModel(
            title = infoModel.title,
            description = infoModel.description,
            url = infoModel.url,
            imageUrl = infoModel.imageUrl,
            chapters = infoModel.chapters.map { mapChapterModel(it) },
            genres = infoModel.genres,
            alternativeNames = infoModel.alternativeNames,
            source = mapApiService(infoModel.source)
        )
    }

    fun mapInfoModel(infoModel: KmpInfoModel): InfoModel {
        return InfoModel(
            title = infoModel.title,
            description = infoModel.description,
            url = infoModel.url,
            imageUrl = infoModel.imageUrl,
            chapters = infoModel.chapters.map { mapChapterModel(it) },
            genres = infoModel.genres,
            alternativeNames = infoModel.alternativeNames,
            source = mapApiService(infoModel.source)
        )
    }

    fun mapChapterModel(chapterModel: ChapterModel): KmpChapterModel {
        return KmpChapterModel(
            name = chapterModel.name,
            url = chapterModel.url,
            uploaded = chapterModel.uploaded,
            sourceUrl = chapterModel.sourceUrl,
            source = mapApiService(chapterModel.source)
        )
    }

    fun mapChapterModel(chapterModel: KmpChapterModel): ChapterModel {
        return ChapterModel(
            name = chapterModel.name,
            url = chapterModel.url,
            uploaded = chapterModel.uploaded,
            sourceUrl = chapterModel.sourceUrl,
            source = mapApiService(chapterModel.source)
        )
    }

    fun mapStorage(storage: KmpStorage): Storage {
        return Storage(
            sub = storage.sub,
            source = storage.source,
            link = storage.link,
            quality = storage.quality,
            filename = storage.filename
        )
    }

    fun mapStorage(storage: Storage): KmpStorage {
        return KmpStorage(
            sub = storage.sub,
            source = storage.source,
            link = storage.link,
            quality = storage.quality,
            filename = storage.filename
        )
    }

    fun mapApiService(apiService: ApiService): KmpApiService {
        return object : KmpApiService {
            override val baseUrl: String = apiService.baseUrl
            override val websiteUrl: String = apiService.websiteUrl
            override val canScroll: Boolean = apiService.canScroll
            override val canScrollAll: Boolean = apiService.canScrollAll
            override val canPlay: Boolean = apiService.canPlay
            override val canDownload: Boolean = apiService.canDownload
            override val notWorking: Boolean = apiService.notWorking
            override val serviceName: String = apiService.serviceName

            override fun getRecentFlow(page: Int): Flow<List<KmpItemModel>> = apiService
                .getRecentFlow(page)
                .map { it.map { mapItemModel(it) } }

            override suspend fun recent(page: Int): List<KmpItemModel> = apiService.recent(page)
                .map { mapItemModel(it) }

            override fun getListFlow(page: Int): Flow<List<KmpItemModel>> = apiService
                .getListFlow(page)
                .map { it.map { mapItemModel(it) } }

            override suspend fun allList(page: Int): List<KmpItemModel> = apiService.allList(page)
                .map { mapItemModel(it) }

            override fun getItemInfoFlow(model: KmpItemModel): Flow<Result<KmpInfoModel>> = apiService
                .getItemInfoFlow(mapItemModel(model))
                .map { it.map { mapInfoModel(it) } }

            override suspend fun itemInfo(model: KmpItemModel): KmpInfoModel = mapInfoModel(
                apiService.itemInfo(mapItemModel(model))
            )

            override fun getChapterInfoFlow(chapterModel: KmpChapterModel): Flow<List<KmpStorage>> = apiService
                .getChapterInfoFlow(mapChapterModel(chapterModel))
                .map { it.map { mapStorage(it) } }

            override suspend fun chapterInfo(chapterModel: KmpChapterModel): List<KmpStorage> = apiService
                .chapterInfo(mapChapterModel(chapterModel))
                .map { mapStorage(it) }

            override fun getSourceByUrlFlow(url: String): Flow<KmpItemModel> = apiService
                .getSourceByUrlFlow(url)
                .map { mapItemModel(it) }

            override suspend fun sourceByUrl(url: String): KmpItemModel = mapItemModel(apiService.sourceByUrl(url))
        }
    }

    fun mapApiService(apiService: KmpApiService): ApiService {
        return object : ApiService {
            override val baseUrl: String = apiService.baseUrl
            override val websiteUrl: String = apiService.websiteUrl
            override val canScroll: Boolean = apiService.canScroll
            override val canScrollAll: Boolean = apiService.canScrollAll
            override val canPlay: Boolean = apiService.canPlay
            override val canDownload: Boolean = apiService.canDownload
            override val notWorking: Boolean = apiService.notWorking
            override val serviceName: String = apiService.serviceName

            override fun getRecentFlow(page: Int): Flow<List<ItemModel>> = apiService
                .getRecentFlow(page)
                .map { it.map { mapItemModel(it) } }

            override suspend fun recent(page: Int): List<ItemModel> = apiService.recent(page)
                .map { mapItemModel(it) }

            override fun getListFlow(page: Int): Flow<List<ItemModel>> = apiService
                .getListFlow(page)
                .map { it.map { mapItemModel(it) } }

            override suspend fun allList(page: Int): List<ItemModel> = apiService.allList(page)
                .map { mapItemModel(it) }

            override fun getItemInfoFlow(model: ItemModel): Flow<Result<InfoModel>> = apiService
                .getItemInfoFlow(mapItemModel(model))
                .map { it.map { mapInfoModel(it) } }

            override suspend fun itemInfo(model: ItemModel): InfoModel = mapInfoModel(
                apiService.itemInfo(mapItemModel(model))
            )

            override fun getChapterInfoFlow(chapterModel: ChapterModel): Flow<List<Storage>> = apiService
                .getChapterInfoFlow(mapChapterModel(chapterModel))
                .map { it.map { mapStorage(it) } }

            override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> = apiService
                .chapterInfo(mapChapterModel(chapterModel))
                .map { mapStorage(it) }

            override fun getSourceByUrlFlow(url: String): Flow<ItemModel> = apiService
                .getSourceByUrlFlow(url)
                .map { mapItemModel(it) }

            override suspend fun sourceByUrl(url: String): ItemModel = mapItemModel(apiService.sourceByUrl(url))
        }
    }
}