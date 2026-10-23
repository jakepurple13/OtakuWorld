package com.programmersbox.mihon

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response

fun manga(title: String, url: String) = SManga.create().apply {
    this.title = title
    this.url = url
    thumbnail_url = "https://img.example.org$url.jpg"
}

class FakeCatalogueSource(
    override val supportsLatest: Boolean = true,
) : CatalogueSource {
    override val id = 42L
    override val name = "Fake"
    override val lang = "en"

    var lastDetailsUrl: String? = null
    var lastMemo: String? = null

    override suspend fun getPopularManga(page: Int) = MangasPage(listOf(manga("Popular", "/p/1")), hasNextPage = true)
    override suspend fun getLatestUpdates(page: Int) = MangasPage(listOf(manga("Latest", "/l/1")), hasNextPage = false)
    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList) =
        MangasPage(listOf(manga("Search $query", "/s/1")), hasNextPage = false)

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        lastDetailsUrl = manga.url
        lastMemo = manga.memo.toString()
        val details = manga.copy().apply {
            description = "Desc"
            genre = "Action, Comedy"
            author = "Author"
            status = SManga.ONGOING
        }
        val chs = listOf(
            SChapter.create().apply { name = "Ch 2"; url = "/c/2"; date_upload = 1_700_000_000_000; chapter_number = 2f },
            SChapter.create().apply { name = "Ch 1"; url = "/c/1"; date_upload = 0; chapter_number = 1f },
        )
        return SMangaUpdate(details, chs)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> = listOf(
        Page(0, url = "/page/0", imageUrl = "https://img.example.org/0.jpg"),
        Page(1, url = "/page/1", imageUrl = null),
    )
}

/** Mimics extension details parsers: returns a NEW SManga/SChapters with url/title never set. */
class PartialUpdateCatalogueSource(
    private val chapters: () -> List<SChapter> = { emptyList() },
) : CatalogueSource by FakeCatalogueSource() {
    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate = SMangaUpdate(
        SManga.create().apply { description = "D"; genre = "A, B" },
        this.chapters(),
    )
}

/** HttpSource that never touches Injekt/network: overrides headersBuilder and getImageUrl. */
@Suppress("OVERRIDE_DEPRECATION")
open class FakeHttpSource : HttpSource() {
    override val name = "FakeHttp"
    override val lang = "en"
    override val supportsLatest = false
    override val baseUrl = "https://example.org"
    var imageUrlCalls = 0

    override fun headersBuilder(): Headers.Builder = Headers.Builder().add("Referer", "https://example.org/")

    override suspend fun getPopularManga(page: Int) = MangasPage(listOf(manga("A", "/a")), hasNextPage = false)
    override suspend fun getPageList(chapter: SChapter): List<Page> = listOf(
        Page(0, url = "/page/0", imageUrl = "https://img.example.org/0.jpg"),
        Page(1, url = "/page/1", imageUrl = null),
    )
    override suspend fun getImageUrl(page: Page): String {
        imageUrlCalls++
        return "https://img.example.org/resolved-${page.index}.jpg"
    }

    override fun popularMangaRequest(page: Int): Request = throw UnsupportedOperationException()
    override fun popularMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw UnsupportedOperationException()
    override fun searchMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()
    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()
    override fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException()
    override fun mangaDetailsParse(response: Response): SManga = throw UnsupportedOperationException()
    override fun chapterListParse(response: Response): List<SChapter> = throw UnsupportedOperationException()
    override fun pageListParse(response: Response): List<Page> = throw UnsupportedOperationException()
    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()
}

/** MangaDex shape: the image host lives in page.url and imageUrl is a path that imageRequest completes. */
class HostInPageUrlSource : FakeHttpSource() {
    override suspend fun getPageList(chapter: SChapter): List<Page> = listOf(
        Page(0, url = "https://cdn.example.org,https://api.example.org/at-home/1", imageUrl = "/data/hash/1.jpg"),
    )

    override fun imageRequest(page: Page): Request =
        okhttp3.Request.Builder().url(page.url.substringBefore(',') + page.imageUrl).headers(headers).build()
}

fun memoOf(key: String, value: String) = buildJsonObject { put(key, JsonPrimitive(value)) }
