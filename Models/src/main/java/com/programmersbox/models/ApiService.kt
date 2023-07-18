package com.programmersbox.models

import android.app.Application
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.Serializable

interface ApiService : Serializable {
    val baseUrl: String
    val websiteUrl: String get() = baseUrl
    val canScroll: Boolean get() = false
    val canScrollAll: Boolean get() = canScroll
    val canPlay: Boolean get() = true
    val canDownload: Boolean get() = true
    val notWorking: Boolean get() = false
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

interface ApiServicesCatalog {
    fun createSources(): List<ApiService>
    val name: String
}

interface ExternalApiServicesCatalog : ApiServicesCatalog {
    suspend fun initialize(app: Application)

    fun getSources(): List<SourceInformation>
    override fun createSources(): List<ApiService> = getSources().map { it.apiService }

    val hasRemoteSources: Boolean
    suspend fun getRemoteSources(): List<RemoteSources> = emptyList()
}

data class RemoteSources(
    val name: String,
    val iconUrl: String,
    val downloadLink: String,
    val sources: List<Sources>
)

data class Sources(
    val name: String,
    val baseUrl: String,
)

data class SourceInformation(
    val apiService: ApiService,
    val name: String,
    val icon: Drawable?,
    val packageName: String,
    val catalog: ApiServicesCatalog? = null
)

val sourceFlow = MutableStateFlow<ApiService?>(null)