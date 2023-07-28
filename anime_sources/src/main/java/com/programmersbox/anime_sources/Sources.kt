package com.programmersbox.anime_sources

import com.programmersbox.anime_sources.anime.AllAnime
import com.programmersbox.anime_sources.anime.AnimeFlick
import com.programmersbox.anime_sources.anime.AnimeKisaDubbed
import com.programmersbox.anime_sources.anime.AnimeKisaMovies
import com.programmersbox.anime_sources.anime.AnimeKisaSubbed
import com.programmersbox.anime_sources.anime.CrunchyRoll
import com.programmersbox.anime_sources.anime.Dopebox
import com.programmersbox.anime_sources.anime.GogoAnimeVC
import com.programmersbox.anime_sources.anime.Hdm
import com.programmersbox.anime_sources.anime.PutlockerAnime
import com.programmersbox.anime_sources.anime.PutlockerCartoons
import com.programmersbox.anime_sources.anime.PutlockerMovies
import com.programmersbox.anime_sources.anime.PutlockerTV
import com.programmersbox.anime_sources.anime.SflixS
import com.programmersbox.anime_sources.anime.VidEmbed
import com.programmersbox.anime_sources.anime.Vidstreaming
import com.programmersbox.anime_sources.anime.WcoStream
import com.programmersbox.anime_sources.anime.WcoStreamCC
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import org.jsoup.Jsoup

enum class Sources(private val api: ApiService, override val notWorking: Boolean = false) : ApiService by api {
    //GOGOANIME(GogoAnimeApi),

    ALLANIME(AllAnime),

    GOGOANIME_VC(GogoAnimeVC),
    HDM(Hdm, true),
    //ANIMESIMPLE_SUBBED(AnimeSimpleSubbed), ANIMESIMPLE_DUBBED(AnimeSimpleDubbed),

    VIDSTREAMING(Vidstreaming), VIDEMBED(VidEmbed),
    DOPEBOX(Dopebox),
    SFLIX(SflixS),

    CRUNCHYROLL(CrunchyRoll),
    ANIMEFLICK(AnimeFlick),

    PUTLOCKERTV(PutlockerTV, true),
    PUTLOCKERANIME(PutlockerAnime, true),
    PUTLOCKERCARTOONS(PutlockerCartoons, true),
    PUTLOCKERMOVIES(PutlockerMovies, true),

    ANIMEKISA_SUBBED(AnimeKisaSubbed, true), ANIMEKISA_DUBBED(AnimeKisaDubbed, true), ANIMEKISA_MOVIES(AnimeKisaMovies, true),

    WCOSTREAM(WcoStream),//WCO_DUBBED(WcoDubbed), WCO_SUBBED(WcoSubbed), WCO_CARTOON(WcoCartoon), WCO_MOVIES(WcoMovies), WCO_OVA(WcoOva),
    WCOSTREAMCC(WcoStreamCC)
    //ANIMETOON(AnimeToonApi), DUBBED_ANIME(AnimeToonDubbed), ANIMETOON_MOVIES(AnimeToonMovies)
    ;

    override val serviceName: String get() = this.name

    companion object {
        val searchSources
            get() = listOf(
                ALLANIME,
                VIDSTREAMING,
                VIDEMBED,
                //PUTLOCKERTV,
                //WCO_SUBBED,
                DOPEBOX,
                SFLIX,
                WCOSTREAM,
                GOGOANIME_VC,
                ANIMEFLICK,
                ANIMEKISA_SUBBED,
                //HDM,
                WCOSTREAMCC,
                CRUNCHYROLL
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

    internal fun recentPath(page: Int = 1) = "$baseUrl/$recentPath${recentPage(page)}".toJsoup()
    internal fun all(page: Int = 1) = "$baseUrl/$allPath${allPage(page)}".toJsoup()

    internal open fun recentPage(page: Int): String = ""
    internal open fun allPage(page: Int): String = ""

    protected fun searchListNonSingle(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> =
        if (searchText.isEmpty()) list else list.filter { it.title.contains(searchText, true) }
}