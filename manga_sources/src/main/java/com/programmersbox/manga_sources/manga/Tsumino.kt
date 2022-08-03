package com.programmersbox.manga_sources.manga

import androidx.annotation.WorkerThread
import androidx.compose.ui.util.fastMap
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.getJsonApi
import com.programmersbox.models.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object Tsumino : ApiService {

    override val baseUrl: String get() = "https://www.tsumino.com"
    override val canScroll: Boolean get() = true
    override val serviceName: String get() = "TSUMINO"

    override suspend fun search(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> {
        val body = FormBody.Builder()
            .add("PageNumber", page.toString())
            .add("Text", searchText.toString())
            .add("Sort", "Newest")
            .add("List", "0")
            .add("Length", "0")
            .build()
        return getJsonApiPost<Base>("$baseUrl/Search/Operate/", body)
            ?.data
            ?.fastMap {
                ItemModel(
                    title = it.entry?.title.toString(),
                    description = "${it.entry?.duration}",
                    url = it.entry?.id.toString(),
                    imageUrl = it.entry?.thumbnailUrl ?: it.entry?.thumbnailTemplateUrl ?: "",
                    source = Tsumino
                )
            }.orEmpty()
    }

    override suspend fun recent(page: Int): List<ItemModel> {
        return getJsonApi<Base>("$baseUrl/Search/Operate/?PageNumber=$page&Sort=Newest")
            ?.data
            ?.fastMap {
                ItemModel(
                    title = it.entry?.title.toString(),
                    description = "${it.entry?.duration}",
                    url = it.entry?.id.toString(),
                    imageUrl = it.entry?.thumbnailUrl ?: it.entry?.thumbnailTemplateUrl ?: "",
                    source = Tsumino
                )
            }.orEmpty()
    }

    override suspend fun allList(page: Int): List<ItemModel> {
        return getJsonApi<Base>("$baseUrl/Search/Operate/?PageNumber=$page&Sort=Popularity")
            ?.data
            ?.fastMap {
                ItemModel(
                    title = it.entry?.title.toString(),
                    description = "${it.entry?.duration}",
                    url = it.entry?.id.toString(),
                    imageUrl = it.entry?.thumbnailUrl ?: it.entry?.thumbnailTemplateUrl ?: "",
                    source = Tsumino
                )
            }.orEmpty()
    }

    override suspend fun itemInfo(model: ItemModel): InfoModel {
        val doc = Jsoup.connect("$baseUrl/entry/${model.url}").get()
        return InfoModel(
            title = model.title,
            description = getDesc(doc),
            url = "$baseUrl/entry/${model.url}",
            imageUrl = model.imageUrl,
            chapters = listOf(
                ChapterModel(
                    url = model.url,
                    name = doc.select("#Pages").text(),
                    uploaded = "",
                    sourceUrl = model.url,
                    source = Tsumino
                )
            ),
            genres = doc.select("#Tag a").eachText(),
            alternativeNames = emptyList(),
            source = Tsumino
        )
    }

    override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
        return chapterModel.name.toIntOrNull()?.let { 1..it }
            ?.map { "https://content.tsumino.com/thumbs/${chapterModel.url}/$it" }
            .orEmpty()
            .fastMap { Storage(link = it, source = chapterModel.url, quality = "Good", sub = "Yes") }
    }

    private fun getDesc(document: Document): String {
        val stringBuilder = StringBuilder()
        val parodies = document.select("#Parody a")
        val characters = document.select("#Character a")
        if (parodies.size > 0) {
            stringBuilder.append("Parodies: ")
            parodies.forEach {
                stringBuilder.append(it.text())
                if (it != parodies.last())
                    stringBuilder.append(", ")
            }
        }
        if (characters.size > 0) {
            stringBuilder.append("\n\n")
            stringBuilder.append("Characters: ")
            characters.forEach {
                stringBuilder.append(it.text())
                if (it != characters.last())
                    stringBuilder.append(", ")
            }
        }
        return stringBuilder.toString()
    }

    private data class Base(val pageNumber: Number?, val pageCount: Number?, val data: List<Data>?)

    private data class Data(val entry: Entry?, val impression: String?, val historyPage: Number?)

    private data class Entry(
        val id: Number?,
        val title: String?,
        val rating: Number?,
        val duration: Number?,
        val collectionPosition: Number?,
        val entryType: String?,
        val thumbnailUrl: String?,
        val thumbnailTemplateUrl: String?,
        val filledOpinion: String?
    )

    @WorkerThread
    fun getApiPost(url: String, requestBody: RequestBody, builder: okhttp3.Request.Builder.() -> Unit = {}): String? {
        val request = okhttp3.Request.Builder()
            .url(url)
            .apply(builder)
            .post(requestBody)
            .build()
        val response = OkHttpClient().newCall(request).execute()
        return if (response.code == 200) response.body!!.string() else null
    }

    @WorkerThread
    inline fun <reified T> getJsonApiPost(url: String, requestBody: RequestBody, noinline builder: okhttp3.Request.Builder.() -> Unit = {}) =
        getApiPost(url, requestBody, builder).fromJson<T>()


}