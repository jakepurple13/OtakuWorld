package com.programmersbox.kmpuiviews.languageorder

import com.programmersbox.datastore.SourceLanguageOrder
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpuiviews.presentation.globalsearch.SearchLanguageGroup
import com.programmersbox.kmpuiviews.presentation.settings.languageorder.LanguageOrderState
import com.programmersbox.kmpuiviews.presentation.settings.languageorder.SourceLanguageOrdering
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceLanguageOrderingTest {

    private class FakeApiService(
        override val serviceName: String,
        override val language: String?,
    ) : KmpApiService {
        override val baseUrl: String = ""
    }

    private fun source(packageName: String, language: String?) = KmpSourceInformation(
        apiService = FakeApiService("$packageName:$language", language),
        name = "$packageName ($language)",
        icon = null,
        packageName = packageName,
    )

    // MangaFire's languages, in the order the extension loads them.
    private val mangaFire = listOf("pt-BR", "fr", "ja", "es-419", "en", "pt", "es").map { source("mangafire", it) }

    private fun List<KmpSourceInformation>.languages() = map { it.apiService.language }

    @Test
    fun rankedMeansRankedDirectlyOrThroughTheBaseLanguage() {
        val ranked = listOf("en", "pt")
        assertEquals(
            listOf(true, true, false, false, false),
            listOf("en", "pt-BR", "ja", "all", null).map { SourceLanguageOrdering.isRanked(it, ranked) },
        )
    }

    @Test
    fun rankedLanguagesComeFirstThenTheRestAToZ() {
        assertEquals(
            listOf("en", "ja", "es", "es-419", "fr", "pt", "pt-BR"),
            SourceLanguageOrdering.sort(mangaFire, listOf("en", "ja")).languages(),
        )
    }

    @Test
    fun regionalVariantFollowsItsRankedBaseLanguage() {
        assertEquals(
            listOf("pt", "pt-BR", "en", "es", "es-419", "fr", "ja"),
            SourceLanguageOrdering.sort(mangaFire, listOf("pt", "en")).languages(),
        )
    }

    @Test
    fun rankedVariantBeatsItsBaseLanguage() {
        assertEquals(
            listOf("pt-BR", "pt", "en"),
            SourceLanguageOrdering.order(listOf("en", "pt", "pt-BR"), listOf("pt-BR", "pt")),
        )
    }

    @Test
    fun allAndUnknownLanguagesGoLast() {
        val sources = listOf(source("x", null), source("x", "all"), source("x", "ja"), source("x", "en"))
        assertEquals(
            listOf("en", "ja", "all", null),
            SourceLanguageOrdering.sort(sources, listOf("en")).languages(),
        )
    }

    @Test
    fun emptySavedOrderFallsBackToDeviceLanguage() {
        assertEquals(listOf("en"), SourceLanguageOrdering.resolveDefaults(emptyList(), "en"))
        assertEquals(listOf("ja", "en"), SourceLanguageOrdering.resolveDefaults(listOf("ja", "en"), "en"))
    }

    @Test
    fun extensionOrderOverridesTheDefault() {
        val state = LanguageOrderState(
            groups = listOf(SearchLanguageGroup("mangafire", "MangaFire", mangaFire.mapNotNull { it.apiService.language }.sorted())),
            defaults = listOf("en"),
            overrides = mapOf("mangafire" to SourceLanguageOrder(languages = listOf("ja", "fr"))),
        )
        assertEquals(
            listOf("ja", "fr", "en", "es", "es-419", "pt", "pt-BR"),
            state.sort(mangaFire).languages(),
        )
        assertEquals(listOf("es", "es-419", "fr", "ja", "pt", "pt-BR"), state.unranked)
    }
}
