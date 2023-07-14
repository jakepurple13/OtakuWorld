package com.programmersbox.extensionloader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.programmersbox.models.ApiService


private const val METADATA_NAME = "programmersbox.otaku.name"
private const val METADATA_CLASS = "programmersbox.otaku.class"
private const val EXTENSION_FEATURE = "programmersbox.otaku.extension"

class SourceLoader(
    private val context: Context,
    sourceType: String,
    private val sourceRepository: SourceRepository
) {
    private val extensionLoader = ExtensionLoader<ApiService, SourceInformation>(
        context,
        "$EXTENSION_FEATURE.$sourceType",
        METADATA_CLASS,
    ) { t, a, p ->
        SourceInformation(
            apiService = t,
            name = a.metaData.getString(METADATA_NAME) ?: "Nothing",
            icon = context.packageManager.getApplicationIcon(p.packageName),
            packageName = p.packageName
        )
    }

    /*private val extensionLoader2 = ExtensionLoader<CatalogueSource, SourceInformation>(
        context,
        "tachiyomi.extension",
        "tachiyomi.extension.class",
    ) { t, a, p ->
        SourceInformation(
            apiService = object : ApiService {
                override val baseUrl: String get() = t.name

                override suspend fun recent(page: Int): List<ItemModel> {
                    return t.fetchLatestUpdates(page).toBlocking().first().mangas.map {
                        ItemModel(
                            title = it.title,
                            description = it.description.orEmpty(),
                            url = it.url,
                            imageUrl = it.thumbnail_url.orEmpty(),
                            source = this
                        ).also { item -> item.extras["smanga"] = it }
                    }
                }

                override suspend fun allList(page: Int): List<ItemModel> {
                    return t.fetchPopularManga(page).toBlocking().first().mangas.map {
                        ItemModel(
                            title = it.title,
                            description = it.description.orEmpty(),
                            url = it.url,
                            imageUrl = it.thumbnail_url.orEmpty(),
                            source = this
                        ).also { item -> item.extras["smanga"] = it }
                    }
                }

                override suspend fun itemInfo(model: ItemModel): InfoModel {
                    val s = t.fetchMangaDetails(model.extras["smanga"] as SManga).toBlocking().first()
                    return InfoModel(
                        title = model.title,
                        description = model.description,
                        url = model.url,
                        imageUrl = model.imageUrl,
                        chapters = t.fetchChapterList(s).toBlocking().first().map {
                            ChapterModel(
                                name = it.name,
                                url = it.url,
                                uploaded = it.date_upload.toString(),
                                sourceUrl = model.url,
                                source = this
                            ).also { item -> item.extras["schapter"] = it }
                        },
                        genres = s.genre?.split(",").orEmpty(),
                        alternativeNames = emptyList(),
                        source = this
                    )
                }

                override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
                    return t.fetchPageList(chapterModel.extras["schapter"] as SChapter).toBlocking().first().map {
                        Storage(
                            link = it.imageUrl,
                            source = chapterModel.url,
                            quality = "Good",
                            sub = "Yes"
                        )
                    }
                }
            },
            name = a.metaData.getString(METADATA_NAME) ?: "Nothing",
            icon = context.packageManager.getApplicationIcon(p.packageName),
            packageName = p.packageName
        )
    }*/

    init {
        val uninstallApplication: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val packageName = intent.data?.encodedSchemeSpecificPart
                when (intent.action) {
                    Intent.ACTION_PACKAGE_REPLACED -> load()
                    Intent.ACTION_PACKAGE_ADDED -> load()
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        sourceRepository.list.find { it.packageName == packageName }?.let { sourceRepository.removeSource(it) }
                    }
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        intentFilter.addDataScheme("package")
        context.registerReceiver(uninstallApplication, intentFilter)
    }

    fun load() {
        sourceRepository.setSources(extensionLoader.loadExtensions().sortedBy { it.name })
    }

    suspend fun blockingLoad() {
        sourceRepository.setSources(extensionLoader.loadExtensionsBlocking().sortedBy { it.name })
    }
}
