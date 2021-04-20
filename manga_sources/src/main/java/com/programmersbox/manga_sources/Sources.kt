package com.programmersbox.manga_sources

import android.annotation.SuppressLint
import android.content.Context
import com.programmersbox.manga_sources.manga.*
import com.programmersbox.manga_sources.utilities.NetworkHelper
import com.programmersbox.models.ApiService

enum class Sources(
    val domain: String,
    val isAdult: Boolean = false,
    val filterOutOfUpdate: Boolean = false,
    val source: ApiService
) : ApiService by source {

    //MANGA_EDEN(domain = "mangaeden", filterOutOfUpdate = true, source = MangaEden),
    //MANGANELO(domain = "manganelo", source = Manganelo),
    MANGA_HERE(domain = "mangahere", source = MangaHere),

    MANGA_4_LIFE(domain = "manga4life", source = MangaFourLife),
    MANGA_PARK(domain = "mangapark", source = MangaPark),
    NINE_ANIME(domain = "nineanime", source = NineAnime),

    //MANGAKAKALOT(domain = "mangakakalot", source = Mangakakalot),
    MANGAMUTINY(domain = "mangamutiny", source = Mangamutiny),

    //MANGA_DOG(domain = "mangadog", source = MangaDog),
    //INKR(domain = "mangarock", source = com.programmersbox.manga_sources.mangasources.manga.INKR),
    //TSUMINO(domain = "tsumino", isAdult = true, source = Tsumino)
    ;

    override val serviceName: String get() = this.name

    companion object {
        fun getSourceByUrl(url: String) = values().find { url.contains(it.domain, true) }

        fun getUpdateSearches() = values().filterNot(Sources::isAdult).filterNot(Sources::filterOutOfUpdate)
    }
}

@SuppressLint("StaticFieldLeak")
object MangaContext {
    @Transient
    lateinit var context: Context

    @Volatile
    @Transient
    private var INSTANCE: NetworkHelper? = null

    fun getInstance(context: Context): NetworkHelper = INSTANCE ?: synchronized(this) { INSTANCE ?: NetworkHelper(context).also { INSTANCE = it } }
}