package com.programmersbox.kmpuiviews.globalsearch

import com.programmersbox.datastore.GlobalSearchLanguageOverride
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpuiviews.presentation.globalsearch.SearchLanguageGroup
import com.programmersbox.kmpuiviews.presentation.globalsearch.SearchLanguages
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchLanguagesTest {

    private class FakeApiService(
        override val serviceName: String,
        override val language: String?,
    ) : KmpApiService {
        override val baseUrl: String = ""
    }

    private fun source(packageName: String, name: String, language: String?) = KmpSourceInformation(
        apiService = FakeApiService("$packageName:$language", language),
        name = if (language == null) name else "$name ($language)",
        icon = null,
        packageName = packageName,
    )

    private val mangaDex = listOf("en", "ja", "es").map { source("mangadex", "MangaDex", it) }
    private val asura = source("asura", "Asura Scans", "en")
    private val batoAll = source("bato", "Bato.to", "all")
    private val otaku = listOf(source("otaku", "Otaku", null), source("otaku", "Otaku 2", null))
    private val sources = mangaDex + asura + batoAll + otaku

    private fun search(
        defaults: Set<String>,
        overrides: Map<String, GlobalSearchLanguageOverride> = emptyMap(),
    ) = SearchLanguages.sourcesToSearch(sources, defaults, overrides).map { it.apiService.serviceName }

    @Test
    fun defaultLanguagesNarrowMultiLanguageExtensions() {
        assertEquals(
            listOf("mangadex:en", "asura:en", "bato:all", "otaku:null"),
            search(setOf("en")),
        )
    }

    @Test
    fun singleLanguageAndAllSourcesAreAlwaysSearched() {
        assertEquals(
            listOf("mangadex:ja", "asura:en", "bato:all", "otaku:null"),
            search(setOf("ja")),
        )
    }

    @Test
    fun noMatchingLanguageSkipsTheExtension() {
        assertEquals(
            listOf("asura:en", "bato:all", "otaku:null"),
            search(setOf("fr")),
        )
    }

    @Test
    fun searchAllOverrideSearchesEveryLanguage() {
        assertEquals(
            listOf("mangadex:en", "mangadex:ja", "mangadex:es", "asura:en", "bato:all", "otaku:null"),
            search(setOf("fr"), mapOf("mangadex" to GlobalSearchLanguageOverride(searchAll = true))),
        )
    }

    @Test
    fun customOverrideReplacesDefaults() {
        assertEquals(
            listOf("mangadex:ja", "mangadex:es", "asura:en", "bato:all", "otaku:null"),
            search(setOf("en"), mapOf("mangadex" to GlobalSearchLanguageOverride(languages = listOf("ja", "es")))),
        )
    }

    @Test
    fun multiLanguageGroupsListOnlyExtensionsWithSeveralLanguages() {
        assertEquals(
            listOf(SearchLanguageGroup("mangadex", "MangaDex", listOf("en", "es", "ja"))),
            SearchLanguages.multiLanguageGroups(sources),
        )
    }

    @Test
    fun savedDefaultsWin() {
        assertEquals(setOf("ja"), SearchLanguages.resolveDefaults(listOf("ja"), listOf("en", "ja"), "en"))
    }

    @Test
    fun unsetDefaultsUseDeviceLanguageIncludingRegionalVariants() {
        assertEquals(
            setOf("pt", "pt-BR"),
            SearchLanguages.resolveDefaults(emptyList(), listOf("en", "pt", "pt-BR"), "pt"),
        )
    }

    @Test
    fun unsetDefaultsFallBackToDeviceLanguageWhenNothingMatches() {
        assertEquals(setOf("de"), SearchLanguages.resolveDefaults(emptyList(), listOf("en"), "de"))
    }
}
