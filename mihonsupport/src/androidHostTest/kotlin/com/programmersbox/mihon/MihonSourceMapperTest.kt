package com.programmersbox.mihon

import com.programmersbox.kmpmodels.KmpConfigurableApiService
import com.programmersbox.kmpmodels.KmpItemModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MihonSourceMapperTest {

    @Test
    fun serviceIdentity() {
        val service = mihonApiService(FakeCatalogueSource())
        assertEquals("mihon:42", service.serviceName)
        assertEquals("Fake (en)", service.displayName)
        assertFalse(service is KmpConfigurableApiService)
    }

    @Test
    fun recentUsesLatestWhenSupported() = runTest {
        assertEquals("Latest", mihonApiService(FakeCatalogueSource(supportsLatest = true)).recent(1).single().title)
        assertEquals("Popular", mihonApiService(FakeCatalogueSource(supportsLatest = false)).recent(1).single().title)
    }

    @Test
    fun itemsCarryRelativeUrlAndPaging() = runTest {
        val service = mihonApiService(FakeHttpSource())
        val item = service.allList(1).single()
        assertEquals("https://example.org/a", item.url)
        assertEquals("/a", item.extras[MihonSourceMapper.URL])
        assertEquals(false, item.extras[MihonSourceMapper.HAS_NEXT])
    }

    @Test
    fun searchPassesQuery() = runTest {
        val service = mihonApiService(FakeCatalogueSource())
        assertEquals("Search naruto", service.search("naruto", 1, emptyList()).single().title)
    }

    @Test
    fun itemInfoRoundTripsUrlAndMemo() = runTest {
        val source = FakeCatalogueSource()
        val service = mihonApiService(source)
        val item = service.allList(1).single().apply {
            extras[MihonSourceMapper.MEMO] = memoOf("k", "v").toString()
        }

        val info = service.itemInfo(item)

        assertEquals("/p/1", source.lastDetailsUrl)
        assertEquals("""{"k":"v"}""", source.lastMemo)
        assertEquals(listOf("Action", "Comedy"), info.genres)
        assertEquals("Desc", info.description)
        assertEquals("Author", info.extras["author"])
        assertEquals("Ongoing", info.extras["status"])
        assertEquals(listOf("Ch 2", "Ch 1"), info.chapters.map { it.name })
        assertEquals("/c/2", info.chapters.first().extras[MihonSourceMapper.URL])
        assertEquals(info.url, info.chapters.first().sourceUrl)
    }

    // Review Focus 1: item reloaded from DB has no extras
    @Test
    fun itemInfoWithoutExtrasStripsBaseUrl() = runTest {
        val source = FakeCatalogueSource()
        val service = mihonApiService(source)
        val fromDb = KmpItemModel("Popular", "", "/p/1", "", service)
        service.itemInfo(fromDb)
        assertEquals("/p/1", source.lastDetailsUrl)
    }

    // Review Focus 5a: date_upload == 0
    @Test
    fun chapterUploadTime() = runTest {
        val service = mihonApiService(FakeCatalogueSource())
        val chapters = service.itemInfo(service.allList(1).single()).chapters
        assertEquals(1_700_000_000_000, chapters[0].uploadedTime)
        assertTrue(chapters[0].uploaded.isNotEmpty())
        assertNull(chapters[1].uploadedTime)
        assertEquals("", chapters[1].uploaded)
    }

    // Review Focus 5b: imageUrl already set -> no getImageUrl call
    @Test
    fun pagesResolveImageUrlOnlyWhenMissingAndCarryHeaders() = runTest {
        val source = FakeHttpSource()
        val service = mihonApiService(source)
        val info = KmpItemModel("A", "", "https://example.org/a", "", service)
        val chapter = com.programmersbox.kmpmodels.KmpChapterModel("c", "https://example.org/c/1", "", info.url, service)

        val storages = service.chapterInfo(chapter)

        assertEquals(
            listOf("https://img.example.org/0.jpg", "https://img.example.org/resolved-1.jpg"),
            storages.map { it.link },
        )
        assertEquals(1, source.imageUrlCalls)
        assertEquals("https://example.org/", storages.first().headers["Referer"])
    }

    @Test
    fun configurableSourceGetsMarker() {
        val configurable = object : eu.kanade.tachiyomi.source.CatalogueSource by FakeCatalogueSource(),
            eu.kanade.tachiyomi.source.ConfigurableSource {
            override fun setupPreferenceScreen(screen: androidx.preference.PreferenceScreen) = Unit
        }
        assertIs<KmpConfigurableApiService>(mihonApiService(configurable))
    }
}
