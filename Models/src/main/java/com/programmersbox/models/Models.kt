package com.programmersbox.models

import androidx.lifecycle.ViewModel
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import java.io.Serializable

data class ItemModel(
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val source: ApiService
) : ViewModel(), Serializable {
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
) : ViewModel()

data class ChapterModel(
    val name: String,
    val url: String,
    val uploaded: String,
    val sourceUrl: String,
    val source: ApiService
) : ViewModel(), Serializable {
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

data class SwatchInfo(val rgb: Int?, val titleColor: Int?, val bodyColor: Int?) : ViewModel()

interface ApiService: Serializable {
    val baseUrl: String
    val websiteUrl: String get() = baseUrl
    val canScroll: Boolean get() = false
    fun getRecent(page: Int = 1): Single<List<ItemModel>>
    fun getList(page: Int = 1): Single<List<ItemModel>>
    fun getItemInfo(model: ItemModel): Single<InfoModel>
    fun searchList(searchText: CharSequence, page: Int = 1, list: List<ItemModel>): Single<List<ItemModel>> =
        Single.create { it.onSuccess(list.filter { it.title.contains(searchText, true) }) }

    fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>>

    fun getSourceByUrl(url: String): Single<ItemModel> = Single.create {
        it.onSuccess(ItemModel("", "", "", "", this))
    }

    val serviceName: String get() = this::class.java.name
}

val sourcePublish = BehaviorSubject.create<ApiService>()
