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
    internal val extras = mutableMapOf<String, Any>()
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
    val source: ApiService
) : ViewModel() {
    var uploadedTime: Long? = null
    //fun getPageInfo() = sources.getPageInfo(this)
}

data class SwatchInfo(val rgb: Int?, val titleColor: Int?, val bodyColor: Int?) : ViewModel()

interface ApiService {
    val baseUrl: String
    val canScroll: Boolean get() = false
    fun getRecent(page: Int = 1): Single<List<ItemModel>>
    fun getList(page: Int = 1): Single<List<ItemModel>>
    fun getItemInfo(model: ItemModel): Single<InfoModel>
    fun searchList(searchText: CharSequence, page: Int = 1, list: List<ItemModel>): Single<List<ItemModel>> =
        Single.create { it.onSuccess(list.filter { it.title.contains(searchText, true) }) }
    //fun getVideoLink(info: ChapterModel): Single<List<String>>
}

val sourcePublish = BehaviorSubject.create<ApiService>()
