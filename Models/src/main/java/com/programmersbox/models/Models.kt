package com.programmersbox.models

import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import java.io.Serializable

data class ItemModel(
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val source: ApiService
) : Serializable {
    val extras = mutableMapOf<String, Any>()
    fun toInfoModel() = source.getItemInfo(this)
}

data class InfoModel(
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val chapters: List<ChapterModel>,
    val genres: List<String>,
    val alternativeNames: List<String>,
    val source: ApiService
)

data class ChapterModel(
    val name: String,
    val url: String,
    val uploaded: String,
    val sourceUrl: String,
    val source: ApiService
) : Serializable {
    var uploadedTime: Long? = null
    fun getChapterInfo() = source.getChapterInfo(this)
    val extras = mutableMapOf<String, Any>()
}

class NormalLink(var normal: Normal? = null)
class Normal(var storage: Array<Storage>? = emptyArray())
data class Storage(
    var sub: String? = null,
    var source: String? = null,
    var link: String? = null,
    var quality: String? = null,
    var filename: String? = null
) {
    val headers = mutableMapOf<String, String>()
}

data class SwatchInfo(val rgb: Int?, val titleColor: Int?, val bodyColor: Int?)

interface ApiService: Serializable {
    val baseUrl: String
    val websiteUrl: String get() = baseUrl
    val canScroll: Boolean get() = false
    val canScrollAll: Boolean get() = canScroll
    val canPlay: Boolean get() = true
    val canDownload: Boolean get() = true
    fun getRecent(page: Int = 1): Single<List<ItemModel>>
    fun getList(page: Int = 1): Single<List<ItemModel>>
    fun getItemInfo(model: ItemModel): Single<InfoModel>
    fun searchList(searchText: CharSequence, page: Int = 1, list: List<ItemModel>): Single<List<ItemModel>> =
        Single.create { e -> e.onSuccess(list.filter { it.title.contains(searchText, true) }) }

    fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>>

    fun getSourceByUrl(url: String): Single<ItemModel> = Single.create {
        it.onSuccess(ItemModel("", "", url, "", this))
    }

    val serviceName: String get() = this::class.java.name
}

val sourcePublish = BehaviorSubject.create<ApiService>()
