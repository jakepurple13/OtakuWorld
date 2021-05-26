package com.programmersbox.anime_sources.anime

import android.content.Context
import android.text.format.DateFormat
import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.anime_sources.toJsoup
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.Storage
import io.reactivex.Single
import org.jsoup.nodes.Document
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.*

object AnimeKisaSubbed : AnimeKisa("anime")
object AnimeKisaDubbed : AnimeKisa("alldubbed")
object AnimeKisaMovies : AnimeKisa("movies")

abstract class AnimeKisa(allPath: String) : ShowApi(
    baseUrl = "https://www.animekisa.tv",
    allPath = allPath,
    recentPath = ""
), KoinComponent {

    private val context: Context by inject()

    private val dateFormat by lazy {
        SimpleDateFormat(
            "${(DateFormat.getTimeFormat(context) as SimpleDateFormat).toLocalizedPattern()} ${(DateFormat.getDateFormat(context) as SimpleDateFormat).toLocalizedPattern()}",
            Locale.getDefault()
        )
    }

    override fun getRecent(doc: Document): Single<List<ItemModel>> = Single.create { emitter ->
        doc
            .select("div.listAnimes")
            .select("div.episode-box-2")
            .map {
                ItemModel(
                    title = it.select("div.title-box").text(),
                    description = "",
                    imageUrl = it.select("img").attr("abs:src"),
                    url = it.select("a.an").next().select("a.an").attr("abs:href"),
                    source = this
                )
            }
            .let(emitter::onSuccess)
    }

    override fun getList(doc: Document): Single<List<ItemModel>> = Single.create { emitter ->
        doc
            .select("a.an")
            .map {
                ItemModel(
                    title = it.text(),
                    description = "",
                    imageUrl = "",
                    url = it.attr("abs:href"),
                    source = this
                )
            }
            .let(emitter::onSuccess)
    }

    override fun getItemInfo(source: ItemModel, doc: Document): Single<InfoModel> = Single.create { emitter ->

        InfoModel(
            source = this,
            url = source.url,
            title = source.title,
            description = "",
            imageUrl = doc.select("div.infopicbox").select("img").attr("abs:src"),
            genres = emptyList(),
            chapters = doc.select("a.infovan").map {
                ChapterModel(
                    name = it.text(),
                    url = it.attr("abs:href"),
                    uploaded = dateFormat.format(Date(it.select("div.timeS").attr("time").toLong() * 1000)),
                    source = this
                )
            },
            alternativeNames = emptyList()
        )
            .let(emitter::onSuccess)
    }

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create {
        val doc = chapterModel.url.toJsoup()
        val downloadUrl = "var VidStreaming = \"(.*?)\";".toRegex()
            .find(doc.toString())?.groups?.get(1)?.value?.replace("load.php", "download")
            ?.toJsoup()
            ?.select("div.dowload")
            ?.select("a")
            ?.eachAttr("abs:href")
            ?.filter { it1 -> it1.contains(".mp4") }
            ?.randomOrNull()

        it.onSuccess(
            listOf(
                Storage(
                    link = downloadUrl,
                    source = chapterModel.url,
                    quality = "Good",
                    sub = "Yes"
                )
            )
        )
    }

}