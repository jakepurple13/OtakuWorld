package com.programmersbox.models

import android.app.Application
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.Serializable

private const val TAG = "ApiService"

suspend fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelayMs: Long = 1_000L,
    maxDelayMs: Long = 20_000L,
    factor: Double = 2.0,
    block: suspend () -> T,
): T {
    var delayMs = initialDelayMs
    repeat(times - 1) {
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Request failed (attempt ${it + 1}/$times), retrying in ${delayMs}ms", e)
            delay(delayMs)
            delayMs = (delayMs * factor).toLong().coerceAtMost(maxDelayMs)
        }
    }
    return block()
}

interface ApiService : Serializable {
    val baseUrl: String
    val websiteUrl: String get() = baseUrl
    val canScroll: Boolean get() = false
    val canScrollAll: Boolean get() = canScroll
    val canPlay: Boolean get() = true
    val canDownload: Boolean get() = true
    val notWorking: Boolean get() = false

    fun getRecentFlow(page: Int = 1): Flow<List<ItemModel>> = flow {
        emit(retryWithBackoff { recent(page) })
    }.dispatchIo()

    suspend fun recent(page: Int = 1): List<ItemModel> = emptyList()

    fun getListFlow(page: Int = 1): Flow<List<ItemModel>> = flow {
        emit(retryWithBackoff { allList(page) })
    }.dispatchIo()

    suspend fun allList(page: Int = 1): List<ItemModel> = emptyList()

    fun getItemInfoFlow(model: ItemModel): Flow<Result<InfoModel>> = flow {
        emit(
            try {
                Result.success(retryWithBackoff { itemInfo(model) })
            } catch (e: Exception) {
                Log.e(TAG, "itemInfo failed for ${model.title}", e)
                Result.failure(e)
            }
        )
    }.dispatchIo()

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
            Log.w(TAG, "searchSourceList error, falling back to local filter", it)
            emitAll(flow { emit(list.filter { s -> s.title.contains(searchText, true) }) })
        }

    fun getChapterInfoFlow(chapterModel: ChapterModel): Flow<List<Storage>> = flow {
        emit(retryWithBackoff { chapterInfo(chapterModel) })
    }
        .catch {
            Log.e(TAG, "chapterInfo failed", it)
            emit(emptyList())
        }
        .dispatchIo()

    suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> = emptyList()

    fun getSourceByUrlFlow(url: String): Flow<ItemModel> = flow { emit(sourceByUrl(url)) }
        .dispatchIo()
        .catch {
            Log.e(TAG, "sourceByUrl failed for $url", it)
            emit(ItemModel("", "", url, "", this@ApiService))
        }

    suspend fun sourceByUrl(url: String): ItemModel = error("Not setup")

    val serviceName: String get() = this::class.java.name

    fun <T> Flow<List<T>>.dispatchIoAndCatchList() = this
        .dispatchIo()
        .catch {
            Log.e(TAG, "list flow error", it)
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

    fun shouldReload(packageName: String, packageInfo: PackageInfo): Boolean = false
}

interface ExternalCustomApiServicesCatalog : ApiServicesCatalog {

    suspend fun initialize(app: Application)

    fun getSources(): List<SourceInformation>
    override fun createSources(): List<ApiService> = getSources().map { it.apiService }

    val hasRemoteSources: Boolean

    fun shouldReload(packageName: String, packageInfo: PackageInfo): Boolean = false

    suspend fun getRemoteSources(customUrls: List<String>): List<RemoteSources> = emptyList()
}

data class RemoteSources(
    val name: String,
    val packageName: String,
    val version: String,
    val iconUrl: String,
    val downloadLink: String,
    val sources: List<Sources>,
)

data class Sources(
    val name: String,
    val baseUrl: String,
    val version: String,
)

data class SourceInformation(
    val apiService: ApiService,
    val name: String,
    val icon: Drawable?,
    val packageName: String,
    val catalog: ApiServicesCatalog? = null,
)
