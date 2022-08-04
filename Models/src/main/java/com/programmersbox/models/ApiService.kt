package com.programmersbox.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.io.Serializable

interface ApiService : Serializable {
    val baseUrl: String
    val websiteUrl: String get() = baseUrl
    val canScroll: Boolean get() = false
    val canScrollAll: Boolean get() = canScroll
    val canPlay: Boolean get() = true
    val canDownload: Boolean get() = true
    fun getRecentFlow(page: Int = 1): Flow<List<ItemModel>> = flow { emit(recent(page)) }.dispatchIo()
    suspend fun recent(page: Int = 1): List<ItemModel> = emptyList()

    fun getListFlow(page: Int = 1): Flow<List<ItemModel>> = flow { emit(allList(page)) }.dispatchIo()
    suspend fun allList(page: Int = 1): List<ItemModel> = emptyList()

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

    fun searchListFlow(searchText: CharSequence, page: Int = 1, list: List<ItemModel>): Flow<List<ItemModel>> =
        flow { emit(search(searchText, page, list)) }

    fun searchSourceList(searchText: CharSequence, page: Int = 1, list: List<ItemModel>): Flow<List<ItemModel>> = flow {
        if (searchText.isBlank()) throw Exception("No search necessary")
        emitAll(searchListFlow(searchText, page, list))
    }
        .dispatchIo()
        .catch {
            it.printStackTrace()
            emitAll(flow { emit(list.filter { s -> s.title.contains(searchText, true) }) })
        }

    fun getChapterInfoFlow(chapterModel: ChapterModel): Flow<List<Storage>> = flow { emit(chapterInfo(chapterModel)) }
        .catch {
            it.printStackTrace()
            emit(emptyList())
        }
        .dispatchIo()

    suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> = emptyList()

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

val sourceFlow = MutableStateFlow<ApiService?>(null)