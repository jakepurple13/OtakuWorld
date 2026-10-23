package com.programmersbox.aniyomi.loader

import com.programmersbox.aniyomi.FakeAnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimeRelation
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeEpisodeUpdate
import eu.kanade.tachiyomi.animesource.model.SAnimeSeasonUpdate
import eu.kanade.tachiyomi.animesource.model.SEpisode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TwoSourceFactory : AnimeSourceFactory {
    override fun createSources(): List<AnimeSource> =
        listOf(FakeAnimeCatalogueSource(), FakeAnimeCatalogueSource(false))
}

/** An AnimeSource that is not an AnimeCatalogueSource. */
class PlainSource : AnimeSource {
    override val id = 1L
    override val name = "Plain"
    override val supportsLatest = false
    override suspend fun getPopularAnime(page: Int): AnimesPage = throw UnsupportedOperationException()
    override suspend fun getLatestUpdates(page: Int): AnimesPage = throw UnsupportedOperationException()
    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage =
        throw UnsupportedOperationException()
    override suspend fun getAnimeEpisodeUpdate(
        anime: SAnime,
        episodes: List<SEpisode>,
        fetchDetails: Boolean,
        fetchEpisodes: Boolean,
    ): SAnimeEpisodeUpdate = throw UnsupportedOperationException()
    override suspend fun getAnimeSeasonUpdate(
        anime: SAnime,
        seasons: List<SAnime>,
        fetchDetails: Boolean,
        fetchSeasons: Boolean,
    ): SAnimeSeasonUpdate = throw UnsupportedOperationException()
    override suspend fun getRelatedAnimeList(anime: SAnime): List<AnimeRelation> = throw UnsupportedOperationException()
}

class ThrowingSource : AnimeCatalogueSource by FakeAnimeCatalogueSource() {
    init { throw NoClassDefFoundError("eu/kanade/tachiyomi/Missing") }
}

class SourceInstantiationTest {
    private val loader = javaClass.classLoader!!
    private val pkg = "com.programmersbox.aniyomi.loader"

    @Test
    fun relativeClassNamesResolved() {
        val sources = AniyomiSourceInstantiator.instantiate(
            " .TwoSourceFactory ; ${FakeAnimeCatalogueSource::class.java.name}", pkg, loader,
        )
        assertEquals(3, sources.size)
    }

    @Test
    fun factoryExpanded() {
        val sources = AniyomiSourceInstantiator.instantiate(".TwoSourceFactory", pkg, loader)
        assertEquals(listOf(true, false), sources.map { it.supportsLatest })
    }

    @Test
    fun nonCatalogueDropped() {
        val sources = AniyomiSourceInstantiator.instantiate(".PlainSource;.TwoSourceFactory", pkg, loader)
        assertEquals(2, sources.size)
    }

    @Test
    fun anyFailureFailsPackage() {
        assertFailsWith<ClassNotFoundException> {
            AniyomiSourceInstantiator.instantiate(".TwoSourceFactory;does.not.Exist", pkg, loader)
        }
        assertFailsWith<Throwable> {
            AniyomiSourceInstantiator.instantiate(".TwoSourceFactory;.ThrowingSource", pkg, loader)
        }
    }
}
