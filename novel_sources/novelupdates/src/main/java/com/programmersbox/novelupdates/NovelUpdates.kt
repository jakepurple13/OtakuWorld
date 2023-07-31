package com.programmersbox.novelupdates

import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.Storage
import com.programmersbox.models.createHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.Parameters
import org.jsoup.nodes.Document

object NovelUpdates : ApiService {
    override val canDownload: Boolean get() = false
    override val baseUrl: String get() = "https://www.novelupdates.com"
    override val canScroll: Boolean get() = true
    override val serviceName: String get() = "NOVEL_UPDATES"
    private val client = createHttpClient()

    override suspend fun recent(page: Int): List<ItemModel> {
        val f = client.get("$baseUrl/series-ranking/?rank=week&pg=$page")
        val doc = f.body<Document>()
        return doc
            .select("div.search_main_box_nu")
            .map {
                ItemModel(
                    title = it.select(".search_title > a").text(),
                    description = "",
                    url = it.select(".search_title > a").attr("abs:href"),
                    imageUrl = it.select("img").attr("abs:src"),
                    source = this
                )
            }
    }

    override suspend fun allList(page: Int): List<ItemModel> {
        val f = client.get("$baseUrl/series-ranking/?rank=popular&pg=$page")
        val doc = f.body<Document>()
        return doc
            .select("div.search_main_box_nu")
            .map {
                ItemModel(
                    title = it.select(".search_title > a").text(),
                    description = "",
                    url = it.select(".search_title > a").attr("abs:href"),
                    imageUrl = it.select("img").attr("abs:src"),
                    source = this
                )
            }
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> {
        val f = client.get("$baseUrl/page/$page/?s=$searchText&post_type=seriesplans")
        val doc = f.body<Document>()
        return doc
            .select("div.search_main_box_nu")
            .map {
                ItemModel(
                    title = it.select(".search_title > a").text(),
                    description = "",
                    url = it.select(".search_title > a").attr("abs:href"),
                    imageUrl = it.select("img").attr("abs:src"),
                    source = this
                )
            }
    }

    override suspend fun sourceByUrl(url: String): ItemModel {
        val f = client.get(url)
        val doc = f.body<Document>()
        return ItemModel(
            title = doc.select(".seriestitlenu").text(),
            description = doc.select("#editdescription p").text(),
            url = url,
            imageUrl = doc.select(".seriesimg > img").attr("abs:src"),
            source = this
        )
    }

    override suspend fun itemInfo(model: ItemModel): InfoModel {
        val f = client.get(model.url)
        val doc = f.body<Document>()
        val bookId = doc.select("#mypostid").attr("value")
        val chapters = client.submitForm(
            url = "$baseUrl/wp-admin/admin-ajax.php",
            formParameters = Parameters.build {
                append("action", "nd_getchapters")
                append("mygrr", "0")
                append("mypostid", bookId)
            }
        )
            .body<Document>()
            .select("li.sp_li_chp")
            .map {
                ChapterModel(
                    name = it.select("li").text(),
                    url = it.select("a:nth-child(2)").attr("abs:href"),
                    source = this,
                    sourceUrl = model.url,
                    uploaded = ""
                )
            }
        return InfoModel(
            title = doc.select(".seriestitlenu").text(),
            description = doc.select("#editdescription p").text(),
            url = model.url,
            imageUrl = doc.select(".seriesimg > img").attr("abs:src"),
            chapters = chapters,
            genres = doc.select("#seriesgenre a").eachText(),
            alternativeNames = emptyList(),
            source = this
        )
    }

    override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
        val f = client.get(chapterModel.url)
        val doc = f.body<Document>()
        val d = doc.select("p").html()
        return listOf(Storage(link = d.replace("</span>", "</span><br><br>")))
    }
}