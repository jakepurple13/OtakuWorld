package com.programmersbox.kmpuiviews.repository

import com.programmersbox.extensioninterfaces.ExtensionChapter
import com.programmersbox.extensioninterfaces.ExtensionItem
import com.programmersbox.jsextensionloader.JsExtension
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage

/**
 * Wraps a [JsExtension] so it can be registered into the legacy [SourceRepository]
 * alongside real JAR/APK sources. Each JS extension operation is two-phase
 * (request/parse) internally — that's fully hidden here; this class only maps
 * shapes, no networking of its own.
 */
class JsApiServiceAdapter(private val jsExtension: JsExtension) : KmpApiService {

    override val baseUrl: String = "https://${jsExtension.manifest.id}.jsextension/"
    override val canScroll: Boolean = true
    override val serviceName: String get() = jsExtension.manifest.name

    override suspend fun recent(page: Int): List<KmpItemModel> =
        jsExtension.getLatest(page).map { it.toKmpItemModel() }

    override suspend fun allList(page: Int): List<KmpItemModel> =
        jsExtension.getPopular(page).map { it.toKmpItemModel() }

    override suspend fun itemInfo(model: KmpItemModel): KmpInfoModel {
        val detail = jsExtension.getDetail(model.url)
        return KmpInfoModel(
            title = detail.title,
            description = detail.description.orEmpty(),
            url = detail.url,
            imageUrl = detail.imageUrl.orEmpty(),
            chapters = detail.chapters.map { it.toKmpChapterModel(sourceUrl = detail.url) },
            genres = detail.genres,
            alternativeNames = emptyList(),
            source = this,
        )
    }

    override suspend fun chapterInfo(chapterModel: KmpChapterModel): List<KmpStorage> {
        val content = jsExtension.getContent(chapterModel.url)
        return content.urls.map { url ->
            KmpStorage(
                source = serviceName,
                link = url,
                quality = "Default",
                filename = url.substringAfterLast("/"),
            ).apply { headers.putAll(content.headers) }
        }
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<KmpItemModel>): List<KmpItemModel> =
        jsExtension.search(searchText.toString(), page).map { it.toKmpItemModel() }

    override suspend fun sourceByUrl(url: String): KmpItemModel {
        val detail = jsExtension.getDetail(url)
        return KmpItemModel(
            title = detail.title,
            description = detail.description.orEmpty(),
            url = detail.url,
            imageUrl = detail.imageUrl.orEmpty(),
            source = this,
        )
    }

    private fun ExtensionItem.toKmpItemModel() = KmpItemModel(
        title = title,
        description = "",
        url = url,
        imageUrl = imageUrl.orEmpty(),
        source = this@JsApiServiceAdapter,
    )

    private fun ExtensionChapter.toKmpChapterModel(sourceUrl: String) = KmpChapterModel(
        name = name,
        url = url,
        uploaded = uploaded.orEmpty(),
        sourceUrl = sourceUrl,
        source = this@JsApiServiceAdapter,
    )
}
