package com.programmersbox.anime_sources.anime

import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.Storage
import io.reactivex.Single
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object WcoDubbed : WcoStream("dubbed-anime-list")

abstract class WcoStream(allPath: String) : ShowApi(
    baseUrl = "https://www.wcostream.com",
    allPath = allPath,
    recentPath = ""
) {

    /*override fun recentPage(page: Int): String = page.toString()*/

    override fun getRecent(doc: Document): Single<List<ItemModel>> = Single.create { emitter ->
        doc
            .select("ul.items")
            .select("li")
            .map {
                ItemModel(
                    title = it.select("div.img").select("img").attr("alt"),
                    description = "",
                    imageUrl = it.select("div.img").select("img").attr("abs:src"),
                    url = it.select("div.release").select("a").attr("abs:href"),
                    source = this
                )
            }
            .let(emitter::onSuccess)
    }

    override fun getList(doc: Document): Single<List<ItemModel>> = Single.create { emitter ->
        doc
            .select("div.ddmcc")
            .select("li")
            .map {
                ItemModel(
                    title = it.select("a").text(),
                    description = "",
                    imageUrl = "",
                    url = it.select("a").attr("abs:href"),
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
            description = doc.select("div.iltext").text(),
            imageUrl = doc.select("div#cat-img-desc").select("img").attr("abs:src"),
            genres = doc.select("div#cat-genre").select("div.wcobtn").eachText(),
            chapters = doc.select("div#catlist-listview").select("ul").select("li")
                .map {
                    ChapterModel(
                        name = it.select("a").text(),
                        url = it.select("a").attr("abs:href"),
                        uploaded = "",
                        source = this
                    )
                },
            alternativeNames = emptyList()
        )
            .let(emitter::onSuccess)
    }

    private fun <T> T.alsoPrint() = also { println(it) }

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create { emitter ->

        val f = Jsoup.connect(chapterModel.url)
            .header("X-Requested-With", "XMLHttpRequest")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win 64; x64; rv:69.0) Gecko/20100101 Firefox/69.0")
            .followRedirects(true)
            .get()
            //.also { println(it) }
            .let {
                val f1 = it.select("div.kaynak_star_rating").let {
                    Triple(it.attr("data-id"), it.attr("data-token"), it.attr("data-time"))
                }

                //https://www.wcostream.com/playlist-cat/zaion-i-wish-you-were-here

                ///inc/embed/getvidlink.php?v=Zaion/Zaion%20I%20Wish%20You%20Were%20Here%20-%2004%20-%20HerePresence%20%5BDr-Love55%5D.mp4&embed=anime

                /*
                return Base64.encodeToString(clientInfo.toByteArray(), Base64.DEFAULT).removeSurrounding("\n").replace("\n", "")
        //return java.util.Base64.getEncoder().encodeToString(clientInfo.toByteArray())
                 */

                val scriptUrl = it
                    .select("meta[itemprop=embedURL]")
                    .attr("content")
                    .alsoPrint()

                val list = it
                    .select("meta[itemprop=embedURL]")
                    .alsoPrint()
                    .next()
                    .alsoPrint()
                    .let { it.toString() }
                    .let { "var (.*?) = \\[(.*?)\\];".toRegex().find(it)?.groups?.get(2)?.value }
                    .alsoPrint()

                val ending = " - ([0-9]+)".toRegex().find(scriptUrl)?.groups?.get(1)?.value
                    .alsoPrint()

                val letters = list
                    ?.split(",")
                    ?.map { it.removeSurrounding("\"") }
                    ?.lastOrNull()
                    .alsoPrint()
                //?.let { Base64.getDecoder().decode(it) }
                //.alsoPrint()

                /*.attr("content")
                .also { println(it) }
                .let { "$baseUrl$it&pid=${f1.first}&h=${f1.second}&t=${f1.third}" }
                .also { println(it) }
                .let {
                    Jsoup
                        .connect(it)
                        .header("X-Requested-With", "XMLHttpRequest")
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win 64; x64; rv:69.0) Gecko/20100101 Firefox/69.0")
                        .get()
                }*/
            }
            //.also { println(it) }
            /*.select("script")
            .also { println(it) }
            .eachAttr("abs:src")
            .filter { !it.contains("firebase") }
            .map { getApi(it) }*//*
            //.filter { it.toString().contains("getvidlink") }
            //.also { println(it) }
            *//* .select("meta[itemprop=embedURL]")
             .also { println(it) }
             .attr("content")
             .also { println(it) }
             .let { getApi("$baseUrl$it") }*/
            .also { println("-".repeat(50)) }
            .also { println(it) }
        //.let { getApi("$baseUrl$it") }
        //.also { println(it) }

        /*.map {
            Storage(
                link = "",
                source = chapterModel.url,
                quality = "Good",
                sub = "Yes"
            )
        }*/

        /*
        Storage(
                link = link,
                source = chapterModel.url,
                quality = "Good",
                sub = "Yes"
            )
         */

        emitter.onSuccess(emptyList())
    }

}