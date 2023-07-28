package com.programmersbox.manga_sources

import com.programmersbox.manga_sources.manga.AsuraScans
import com.programmersbox.manga_sources.manga.MangaHere
import com.programmersbox.manga_sources.manga.MangaTown
import com.programmersbox.manga_sources.manga.NineAnime
import com.programmersbox.manga_sources.manga.Tsumino
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
    ASURA_SCANS(domain = "asurascans", source = AsuraScans),

    NINE_ANIME(domain = "nineanime", source = NineAnime),

    //MANGAKAKALOT(domain = "mangakakalot", source = Mangakakalot),
    //MANGA_DOG(domain = "mangadog", source = MangaDog),
    //INKR(domain = "mangarock", source = com.programmersbox.manga_sources.mangasources.manga.INKR),
    TSUMINO(domain = "tsumino", isAdult = true, source = Tsumino),
    MANGATOWN(domain = "mangatown", source = MangaTown);

    override val serviceName: String get() = this.name

    companion object {
        fun getSourceByUrl(url: String) = values().find { url.contains(it.domain, true) }

        fun getUpdateSearches() = values().filterNot(Sources::isAdult).filterNot(Sources::filterOutOfUpdate)
    }
}
