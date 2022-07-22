package com.programmersbox.anime_sources.anime

import androidx.compose.ui.util.fastMap
import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.anime_sources.Sources
import com.programmersbox.anime_sources.utilities.M3u8Helper
import com.programmersbox.anime_sources.utilities.extractors
import com.programmersbox.anime_sources.utilities.getQualityFromName
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.getApi
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.Storage
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import java.net.URI
import java.net.URLDecoder

object AllAnime : ShowApi(
    baseUrl = "https://allanime.site",
    allPath = "",
    recentPath = ""
) {
    override val canDownload: Boolean get() = false

    override fun getRecent(doc: Document): Single<List<ItemModel>> = Single.just(emptyList())

    override fun getRecent(page: Int): Single<List<ItemModel>> = Single.create {
        val url =
            """$baseUrl/graphql?variables={"search":{"allowAdult":true,"allowUnknown":false},"limit":30,"page":1,"translationType":"dub","countryOrigin":"ALL"}&extensions={"persistedQuery":{"version":1,"sha256Hash":"d2670e3e27ee109630991152c8484fce5ff5e280c523378001f9a23dc1839068"}}"""
        getApi(url)?.let { it.fromJson<AllAnimeQuery>() }
            ?.data?.shows?.edges
            ?.map {
                ItemModel(
                    title = it.name,
                    description = it.description.orEmpty(),
                    imageUrl = it.thumbnail.orEmpty(),
                    url = "$baseUrl/anime/${it._id}",
                    source = Sources.ALLANIME
                )
            }
            .orEmpty()
            .let(it::onSuccess)
    }
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())

    override suspend fun recent(page: Int): List<ItemModel> {
        val url =
            """$baseUrl/graphql?variables={"search":{"allowAdult":true,"allowUnknown":false},"limit":30,"page":1,"translationType":"dub","countryOrigin":"ALL"}&extensions={"persistedQuery":{"version":1,"sha256Hash":"d2670e3e27ee109630991152c8484fce5ff5e280c523378001f9a23dc1839068"}}"""
        return getApi(url)?.let { it.fromJson<AllAnimeQuery>() }
            ?.data?.shows?.edges
            ?.map {
                ItemModel(
                    title = it.name,
                    description = it.description.orEmpty(),
                    imageUrl = it.thumbnail.orEmpty(),
                    url = "$baseUrl/anime/${it._id}",
                    source = Sources.ALLANIME
                )
            }
            .orEmpty()
    }

    override fun getList(doc: Document): Single<List<ItemModel>> = Single.just(emptyList())

    override suspend fun allList(page: Int): List<ItemModel> {
        val random =
            """$baseUrl/graphql?variables={"format":"anime"}&extensions={"persistedQuery":{"version":1,"sha256Hash":"21ac672633498a3698e8f6a93ce6c2b3722b29a216dcca93363bf012c360cd54"}}"""

        return getApi(random)?.let { it.fromJson<RandomMain>() }
            ?.data?.queryRandomRecommendation
            ?.map {
                ItemModel(
                    title = it.name.orEmpty(),
                    description = "",
                    imageUrl = it.thumbnail.orEmpty(),
                    url = "$baseUrl/anime/${it._id}",
                    source = Sources.ALLANIME
                )
            }
            .orEmpty()
    }

    override fun getItemInfo(source: ItemModel, doc: Document): Single<InfoModel> = Single.never()

    override suspend fun itemInfo(model: ItemModel): InfoModel {
        val rhino = Context.enter()
        rhino.initStandardObjects()
        rhino.optimizationLevel = -1
        val scope: Scriptable = rhino.initStandardObjects()

        val response = getApi(model.url).orEmpty()
        val doc = Jsoup.parse(response)
        val script = doc.select("script").firstOrNull { it.html().contains("window.__NUXT__") }
        val js = """
            const window = {}
            ${script?.html()}
            const returnValue = JSON.stringify(window.__NUXT__.fetch[0].show)
        """.trimIndent()
        rhino.evaluateString(scope, js, "JavaScript", 1, null)
        val jsEval = scope.get("returnValue", scope)
        return jsEval.toString().fromJson<Edges>()?.let { edges ->
            InfoModel(
                source = Sources.ALLANIME,
                url = model.url,
                title = edges.name,
                description = edges.description?.replace(Regex("""<(.*?)>"""), "").orEmpty(),
                imageUrl = edges.thumbnail.orEmpty(),
                genres = emptyList(),
                chapters = edges.availableEpisodes?.let {
                    listOfNotNull(
                        if (it.sub != 0) ((1..it.sub).map { epNum ->
                            ChapterModel(
                                name = "Sub $epNum",
                                url = "$baseUrl/anime/${edges._id}/episodes/sub/$epNum",
                                uploaded = "",
                                sourceUrl = model.url,
                                source = Sources.ALLANIME
                            )
                        }.reversed()) else null,
                        if (it.dub != 0) ((1..it.dub).map { epNum ->
                            ChapterModel(
                                name = "Dub $epNum",
                                url = "$baseUrl/anime/${edges._id}/episodes/dub/$epNum",
                                uploaded = "",
                                sourceUrl = model.url,
                                source = Sources.ALLANIME
                            )
                        }.reversed()) else null
                    ).flatten()
                }.orEmpty(),
                alternativeNames = emptyList()
            )
        } ?: super.itemInfo(model)
    }

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.just(emptyList())

    private val embedBlackList = listOf(
        "https://mp4upload.com/",
        "https://streamsb.net/",
        "https://dood.to/",
        "https://videobin.co/",
        "https://ok.ru",
        "https://streamlare.com",
    )

    private fun embedIsBlacklisted(url: String): Boolean {
        embedBlackList.forEach {
            if (it.javaClass.name == "kotlin.text.Regex") {
                if ((it as Regex).matches(url)) {
                    return true
                }
            } else {
                if (url.contains(it)) {
                    return true
                }
            }
        }
        return false
    }

    override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
        var apiEndPoint = getApi("$baseUrl/getVersion")?.fromJson<ApiEndPoint>()?.episodeIframeHead
        if (apiEndPoint != null) {
            if (apiEndPoint.endsWith("/")) apiEndPoint = apiEndPoint.slice(0 until apiEndPoint.length - 1)
        }

        val html = getApi(chapterModel.url).orEmpty()
        val sources = Regex("""sourceUrl[:=]"(.+?)"""").findAll(html).toList()
            .map { URLDecoder.decode(it.destructured.component1().sanitize(), "UTF-8") }

        val m3u8 = M3u8Helper()

        return sources.map {
            var link = it.replace(" ", "%20")
            if (URI(link).isAbsolute || link.startsWith("//")) {
                if (link.startsWith("//")) link = "https:$it"

                if (Regex("""streaming\.php\?""").matches(link)) {
                    // for now ignore
                    emptyList()
                } else if (!embedIsBlacklisted(link)) {
                    if (URI(link).path.contains(".m3u")) {
                        m3u8.m3u8Generation(M3u8Helper.M3u8Stream(link, null), false).fastMap {
                            Storage(
                                link = it.streamUrl,
                                source = chapterModel.url,
                                filename = "${chapterModel.name}.mp4",
                                quality = "${chapterModel.name}: ${getQualityFromName(it.quality.toString()).name}",
                                sub = getQualityFromName(it.quality.toString()).name
                            )
                        }
                    } else {
                        extractors
                            .flatMap { e ->
                                if (link.startsWith(e.mainUrl)) {
                                    e.getUrl(link)
                                } else emptyList()
                            }
                    }
                } else emptyList()
            } else {
                link = apiEndPoint + URI(link).path + ".json?" + URI(link).query
                getApi(link)
                    ?.fromJson<AllAnimeVideoApiResponse>()?.links
                    ?.map { server ->
                        if (server.hls != null && server.hls) {
                            /*m3u8.m3u8Generation(M3u8Helper.M3u8Stream(link, null), false).fastMap {
                                Storage(
                                    link = it.streamUrl,
                                    source = chapterModel.url,
                                    filename = "${chapterModel.name}.mp4",
                                    quality = server.resolutionStr,
                                    sub = getQualityFromName(it.quality.toString()).name
                                )
                            }*/
                            listOf(
                                Storage(
                                    link = server.link,
                                    source = chapterModel.url,
                                    filename = "${chapterModel.name}.mp4",
                                    quality = server.resolutionStr,
                                    sub = ""
                                )
                            )
                        } else {
                            extractors
                                .flatMap { e ->
                                    if (link.startsWith(e.mainUrl)) {
                                        e.getUrl(link)
                                    } else emptyList()
                                }
                        }
                    }
                    ?.flatten()
                    .orEmpty()
            }
        }.flatten()
    }

    private fun String.sanitize(): String {
        var out = this
        listOf(Pair("\\u002F", "/")).forEach {
            out = out.replace(it.first, it.second)
        }
        return out
    }

    override suspend fun search(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> {
        val url =
            """$baseUrl/graphql?variables={"search":{"allowAdult":true,"allowUnknown":false,"query":"$searchText"},"limit":26,"page":1,"translationType":"dub","countryOrigin":"ALL"}&extensions={"persistedQuery":{"version":1,"sha256Hash":"d2670e3e27ee109630991152c8484fce5ff5e280c523378001f9a23dc1839068"}}"""

        return getApi(url)?.let { it.fromJson<AllAnimeQuery>() }
            ?.data?.shows?.edges
            ?.map {
                ItemModel(
                    title = it.name,
                    description = it.description.orEmpty(),
                    imageUrl = it.thumbnail.orEmpty(),
                    url = "$baseUrl/anime/${it._id}",
                    source = Sources.ALLANIME
                )
            }
            .orEmpty()
    }

    override suspend fun sourceByUrl(url: String): ItemModel {
        return super.sourceByUrl(url)
    }

    @Serializable
    private data class Data(
        val shows: Shows
    )

    @Serializable
    private data class Shows(
        val pageInfo: PageInfo,
        val edges: List<Edges>,
        val __typename: String
    )

    @Serializable
    private data class Edges(
        val _id: String?,
        val name: String,
        val englishName: String?,
        val nativeName: String?,
        val thumbnail: String?,
        val type: String?,
        val season: Season?,
        val score: Double?,
        val airedStart: AiredStart?,
        val availableEpisodes: AvailableEpisodes?,
        val availableEpisodesDetail: AvailableEpisodesDetail?,
        val studios: List<String>?,
        val description: String?,
        val status: String?,
    )

    @Serializable
    private data class AvailableEpisodes(
        val sub: Int,
        val dub: Int,
        val raw: Int
    )

    @Serializable
    private data class AvailableEpisodesDetail(
        val sub: List<String>,
        val dub: List<String>,
        val raw: List<String>
    )

    @Serializable
    private data class AiredStart(
        val year: Int,
        val month: Int,
        val date: Int
    )

    @Serializable
    private data class Season(
        val quarter: String,
        val year: Int
    )

    @Serializable
    private data class PageInfo(
        val total: Int,
        val __typename: String
    )

    @Serializable
    private data class AllAnimeQuery(
        val data: Data
    )

    @Serializable
    data class RandomMain(
        var data: DataRan? = DataRan()
    )

    @Serializable
    data class DataRan(
        var queryRandomRecommendation: ArrayList<QueryRandomRecommendation> = arrayListOf()
    )

    @Serializable
    data class QueryRandomRecommendation(
        val _id: String? = null,
        val name: String? = null,
        val englishName: String? = null,
        val nativeName: String? = null,
        val thumbnail: String? = null,
        val airedStart: String? = null,
        val availableChapters: String? = null,
        val availableEpisodes: String? = null,
        val __typename: String? = null
    )

    @Serializable
    private data class Links(
        val link: String,
        val hls: Boolean?,
        val resolutionStr: String,
        val src: String?
    )

    @Serializable
    private data class AllAnimeVideoApiResponse(
        val links: List<Links>
    )

    @Serializable
    private data class ApiEndPoint(
        val episodeIframeHead: String
    )

}