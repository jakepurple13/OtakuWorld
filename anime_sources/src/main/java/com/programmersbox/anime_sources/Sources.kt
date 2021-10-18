package com.programmersbox.anime_sources

import com.programmersbox.anime_sources.anime.*
import com.programmersbox.models.ApiService
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

enum class Sources(private val api: ApiService) : ApiService by api {
    //GOGOANIME(GogoAnimeApi),

    GOGOANIME_VC(GogoAnimeVC),
    KAWAIIFU(Kawaiifu),
    HDM(Hdm),
    //ANIMESIMPLE_SUBBED(AnimeSimpleSubbed), ANIMESIMPLE_DUBBED(AnimeSimpleDubbed),

    VIDSTREAMING(Vidstreaming),

    PUTLOCKERTV(PutlockerTV), PUTLOCKERANIME(PutlockerAnime), PUTLOCKERCARTOONS(PutlockerCartoons), PUTLOCKERMOVIES(PutlockerMovies),

    ANIMEKISA_SUBBED(AnimeKisaSubbed), ANIMEKISA_DUBBED(AnimeKisaDubbed), ANIMEKISA_MOVIES(AnimeKisaMovies),

    WCO_DUBBED(WcoDubbed), WCO_SUBBED(WcoSubbed), WCO_CARTOON(WcoCartoon), WCO_MOVIES(WcoMovies), WCO_OVA(WcoOva),
    WCOSTREAMCC(WcoStreamCC)
    //ANIMETOON(AnimeToonApi), DUBBED_ANIME(AnimeToonDubbed), ANIMETOON_MOVIES(AnimeToonMovies)
    ;

    override val serviceName: String get() = this.name

    companion object {
        val searchSources
            get() = listOf(
                VIDSTREAMING,
                PUTLOCKERTV,
                WCO_SUBBED,
                GOGOANIME_VC,
                KAWAIIFU,
                ANIMEKISA_SUBBED,
                HDM,
                WCOSTREAMCC
            )
    }
}

internal fun String.toJsoup() = Jsoup.connect(this).get()
internal fun String.asJsoup() = Jsoup.parse(this)

abstract class ShowApi(
    override val baseUrl: String,
    internal val allPath: String,
    internal val recentPath: String
) : ApiService {

    private fun recent(page: Int = 1) = "$baseUrl/$recentPath${recentPage(page)}".toJsoup()
    private fun all(page: Int = 1) = "$baseUrl/$allPath${allPage(page)}".toJsoup()

    internal open fun recentPage(page: Int): String = ""
    internal open fun allPage(page: Int): String = ""

    internal abstract fun getRecent(doc: Document): Single<List<ItemModel>>
    internal abstract fun getList(doc: Document): Single<List<ItemModel>>

    override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> =
        Single.create { it.onSuccess(if (searchText.isEmpty()) list else list.filter { it.title.contains(searchText, true) }) }

    protected fun searchListNonSingle(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> =
        if (searchText.isEmpty()) list else list.filter { it.title.contains(searchText, true) }

    override fun getRecent(page: Int) = Single.create<Document> { it.onSuccess(recent(page)) }
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .flatMap { getRecent(it) }

    override fun getList(page: Int) = Single.create<Document> { it.onSuccess(all(page)) }
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .flatMap { getList(it) }
        .map { it.sortedBy(ItemModel::title) }

    override fun getItemInfo(model: ItemModel): Single<InfoModel> = Single.create<Document> { it.onSuccess(model.url.toJsoup()) }
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .flatMap { getItemInfo(model, it) }

    internal abstract fun getItemInfo(source: ItemModel, doc: Document): Single<InfoModel>
}