package com.programmersbox.kmpmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable

interface KmpApiService {
    val baseUrl: String
    val websiteUrl: String get() = baseUrl
    val canScroll: Boolean get() = false
    val canScrollAll: Boolean get() = canScroll
    val canPlay: Boolean get() = true
    val canDownload: Boolean get() = true
    val notWorking: Boolean get() = false
    fun getRecentFlow(page: Int = 1): Flow<List<KmpItemModel>> = flow { emit(recent(page)) }.dispatchIo()
    suspend fun recent(page: Int = 1): List<KmpItemModel> = emptyList()

    fun getListFlow(page: Int = 1): Flow<List<KmpItemModel>> = flow { emit(allList(page)) }.dispatchIo()
    suspend fun allList(page: Int = 1): List<KmpItemModel> = emptyList()

    fun getItemInfoFlow(model: KmpItemModel): Flow<Result<KmpInfoModel>> = flow {
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

    suspend fun itemInfo(model: KmpItemModel): KmpInfoModel = error("Need to create an itemInfo")

    suspend fun search(searchText: CharSequence, page: Int = 1, list: List<KmpItemModel>): List<KmpItemModel> =
        list.filter { it.title.contains(searchText, true) }

    fun searchListFlow(searchText: CharSequence, page: Int = 1, list: List<KmpItemModel>): Flow<List<KmpItemModel>> =
        flow { emit(search(searchText, page, list)) }

    fun searchSourceList(searchText: CharSequence, page: Int = 1, list: List<KmpItemModel>): Flow<List<KmpItemModel>> = flow {
        if (searchText.isBlank()) throw Exception("No search necessary")
        emitAll(searchListFlow(searchText, page, list))
    }
        .dispatchIo()
        .catch {
            it.printStackTrace()
            emitAll(flow { emit(list.filter { s -> s.title.contains(searchText, true) }) })
        }

    fun getChapterInfoFlow(chapterModel: KmpChapterModel): Flow<List<KmpStorage>> = flow { emit(chapterInfo(chapterModel)) }
        .catch {
            it.printStackTrace()
            emit(emptyList())
        }
        .dispatchIo()

    suspend fun chapterInfo(chapterModel: KmpChapterModel): List<KmpStorage> = emptyList()

    fun getSourceByUrlFlow(url: String): Flow<KmpItemModel> = flow { emit(sourceByUrl(url)) }
        .dispatchIo()
        .catch {
            it.printStackTrace()
            emit(KmpItemModel("", "", url, "", this@KmpApiService))
        }

    suspend fun sourceByUrl(url: String): KmpItemModel = error("Not setup")

    val serviceName: String get() = this::class.simpleName!!

    fun <T> Flow<List<T>>.dispatchIoAndCatchList() = this
        .dispatchIo()
        .catch {
            it.printStackTrace()
            emit(emptyList())
        }

    fun <T> Flow<T>.dispatchIo() = this.flowOn(Dispatchers.IO)
}

interface KmpApiServicesCatalog {
    fun createSources(): List<KmpApiService>
    val name: String
}

interface KmpExternalApiServicesCatalog : KmpApiServicesCatalog {
    suspend fun initialize()

    fun getSources(): List<KmpSourceInformation>
    override fun createSources(): List<KmpApiService> = getSources().map { it.apiService }

    val hasRemoteSources: Boolean
    suspend fun getRemoteSources(): List<KmpRemoteSources> = emptyList()

    fun shouldReload(packageName: String): Boolean = false
}

interface KmpExternalCustomApiServicesCatalog : KmpApiServicesCatalog {

    suspend fun initialize()

    fun getSources(): List<KmpSourceInformation>
    override fun createSources(): List<KmpApiService> = getSources().map { it.apiService }

    val hasRemoteSources: Boolean

    fun shouldReload(packageName: String): Boolean = false

    suspend fun getRemoteSources(customUrls: List<String>): List<KmpRemoteSources> = emptyList()
}
@Serializable
data class KmpRemoteSources(
    val name: String,
    val packageName: String,
    val version: String,
    val iconUrl: String,
    val downloadLink: String,
    val sources: List<KmpSources>,
)

@Serializable
data class KmpSources(
    val name: String,
    val baseUrl: String,
    val version: String,
)

@Serializable
data class KmpSourceInformation(
    val apiService: KmpApiService,
    val name: String,
    val icon: Int?,
    val packageName: String,
    val catalog: KmpApiServicesCatalog? = null,
)
