package com.programmersbox.cloudstream

import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageData
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.utils.ExtractorLink

/** A plugin API whose responses the test supplies. */
class FakeMainApi(
    override var name: String = "Fake",
    override var lang: String = "en",
    override val hasMainPage: Boolean = true,
    override val mainPage: List<MainPageData> = listOf(MainPageData("Latest", "https://fake.test/latest")),
    private val loadResponse: suspend FakeMainApi.(String) -> LoadResponse? = { null },
    private val links: suspend (String, (SubtitleFile) -> Unit, (ExtractorLink) -> Unit) -> Unit = { _, _, _ -> },
) : MainAPI() {
    override var mainUrl = "https://fake.test"

    val mainPageRequests = mutableListOf<MainPageRequest>()
    val searchQueries = mutableListOf<String>()

    fun item(title: String): SearchResponse = newAnimeSearchResponse(title, "$mainUrl/$title") {
        posterUrl = "$mainUrl/$title.png"
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        mainPageRequests += request
        return newHomePageResponse(request, listOf(item("${request.name}-$page")))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        searchQueries += query
        return listOf(item(query))
    }

    override suspend fun load(url: String): LoadResponse? = loadResponse(url)

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        links(data, subtitleCallback, callback)
        return true
    }
}
