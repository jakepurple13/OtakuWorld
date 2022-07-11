package com.programmersbox.models

import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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
    fun toInfoModelFlow() = source.getItemInfoFlow(this)
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
) {
    val extras = mutableMapOf<String, Any>()
}

data class ChapterModel(
    val name: String,
    val url: String,
    val uploaded: String,
    val sourceUrl: String,
    val source: ApiService
) : Serializable {
    var uploadedTime: Long? = null
    fun getChapterInfo() = source.getChapterInfo(this)
    fun getChapterInfoFlow() = source.getChapterInfoFlow(this)
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

interface ApiService : Serializable {
    val baseUrl: String
    val websiteUrl: String get() = baseUrl
    val canScroll: Boolean get() = false
    val canScrollAll: Boolean get() = canScroll
    val canPlay: Boolean get() = true
    val canDownload: Boolean get() = true
    fun getRecent(page: Int = 1): Single<List<ItemModel>>
    fun getRecentFlow(page: Int = 1): Flow<List<ItemModel>> = flow { emit(recent(page)) }.dispatchIo()
    suspend fun recent(page: Int): List<ItemModel> = emptyList()

    fun getList(page: Int = 1): Single<List<ItemModel>>
    fun getListFlow(page: Int = 1): Flow<List<ItemModel>> = flow { emit(allList(page)) }.dispatchIo()
    suspend fun allList(page: Int): List<ItemModel> = emptyList()

    fun getItemInfo(model: ItemModel): Single<InfoModel>
    fun getItemInfoFlow(model: ItemModel): Flow<Result<InfoModel>> = flow {
        emit(
            try {
                Result.success(itemInfo(model))
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        )
    }
        .dispatchIo()

    suspend fun itemInfo(model: ItemModel): InfoModel = error("Need to create an itemInfo")

    suspend fun search(searchText: CharSequence, page: Int = 1, list: List<ItemModel>): List<ItemModel> =
        list.filter { it.title.contains(searchText, true) }

    fun searchList(searchText: CharSequence, page: Int = 1, list: List<ItemModel>): Single<List<ItemModel>> =
        Single.create { e -> e.onSuccess(list.filter { it.title.contains(searchText, true) }) }

    fun searchListFlow(searchText: CharSequence, page: Int = 1, list: List<ItemModel>): Flow<List<ItemModel>> =
        flow { emit(search(searchText, page, list)) }

    fun searchSourceList(searchText: CharSequence, page: Int = 1, list: List<ItemModel>): Flow<List<ItemModel>> = flow {
        if (searchText.isBlank()) throw Exception("No search necessary")
        emitAll(searchListFlow(searchText, page, list))
    }
        .dispatchIo()
        .catch {
            it.printStackTrace()
            emitAll(flow { emit(list.filter { it.title.contains(searchText, true) }) })
        }

    fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>>
    fun getChapterInfoFlow(chapterModel: ChapterModel): Flow<List<Storage>> = flow { emit(chapterInfo(chapterModel)) }.dispatchIo()
    suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> = emptyList()

    fun getSourceByUrl(url: String): Single<ItemModel> = Single.create {
        it.onSuccess(ItemModel("", "", url, "", this))
    }

    fun getSourceByUrlFlow(url: String): Flow<ItemModel> = flow { emit(sourceByUrl(url)) }
        .dispatchIo()
        .catch {
            it.printStackTrace()
            emit(ItemModel("", "", url, "", this@ApiService))
        }

    suspend fun sourceByUrl(url: String): ItemModel = error("Not setup")

    val serviceName: String get() = this::class.java.name

    fun <T> Flow<List<T>>.dispatchIoAndCatchList() = this
        .dispatchIo()
        .catch {
            it.printStackTrace()
            emit(emptyList())
        }

    fun <T> Flow<T>.dispatchIo() = this.flowOn(Dispatchers.IO)
}

val sourcePublish = BehaviorSubject.create<ApiService>()
