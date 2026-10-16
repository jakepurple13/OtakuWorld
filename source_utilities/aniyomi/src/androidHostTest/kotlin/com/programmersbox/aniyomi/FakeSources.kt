package com.programmersbox.aniyomi

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import rx.Observable

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
class FakeAnimeCatalogueSource(
    override val supportsLatest: Boolean = true,
) : AnimeCatalogueSource {
    override val id = 42L
    override val name = "Fake"
    override val lang = "en"

    override suspend fun getPopularAnime(page: Int) = AnimesPage(emptyList(), hasNextPage = false)
    override suspend fun getLatestUpdates(page: Int) = AnimesPage(emptyList(), hasNextPage = false)
    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList) =
        AnimesPage(emptyList(), hasNextPage = false)

    override fun fetchPopularAnime(page: Int): Observable<AnimesPage> = throw UnsupportedOperationException()
    override fun fetchSearchAnime(page: Int, query: String, filters: AnimeFilterList): Observable<AnimesPage> =
        throw UnsupportedOperationException()
    override fun fetchLatestUpdates(page: Int): Observable<AnimesPage> = throw UnsupportedOperationException()
}
