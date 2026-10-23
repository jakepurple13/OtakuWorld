package com.programmersbox.mihon

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VendoredRuntimeTest {
    private val server = MockWebServer()

    @BeforeTest fun start() = server.start()
    @AfterTest fun stop() = server.close()

    private inner class RxOnlySource : HttpSource() {
        override val name = "RxOnly"
        override val lang = "en"
        override val supportsLatest = false
        override val baseUrl: String get() = server.url("/").toString().removeSuffix("/")
        override val client: OkHttpClient = OkHttpClient()
        override fun headersBuilder(): Headers.Builder = Headers.Builder().add("Referer", "$baseUrl/")

        override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/popular?page=$page", headers)
        override fun popularMangaParse(response: Response): MangasPage {
            val titles = response.body.string().lines().filter { it.isNotBlank() }
            return MangasPage(titles.map { t -> SManga.create().apply { title = t; url = "/m/$t" } }, hasNextPage = true)
        }

        override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw UnsupportedOperationException()
        override fun searchMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()
        override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()
        override fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException()
        override fun mangaDetailsParse(response: Response): SManga = throw UnsupportedOperationException()
        override fun chapterListParse(response: Response): List<SChapter> = throw UnsupportedOperationException()
        override fun pageListParse(response: Response): List<Page> = throw UnsupportedOperationException()
        override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()
    }

    @Test
    fun suspendApiBridgesToRxRequestPath() = runTest {
        server.enqueue(MockResponse.Builder().body("One\nTwo\n").build())

        val page = RxOnlySource().getPopularManga(1)

        assertEquals(listOf("One", "Two"), page.mangas.map { it.title })
        assertTrue(page.hasNextPage)
        val recorded = server.takeRequest()
        assertEquals("/popular?page=1", recorded.target)
        assertEquals("${server.url("/").toString().removeSuffix("/")}/", recorded.headers["Referer"])
    }
}
