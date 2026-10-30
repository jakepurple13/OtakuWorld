package com.programmersbox.kmpuiviews.presentation.settings.languageorder

import androidx.compose.ui.text.intl.Locale
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SourceLanguageOrder
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.globalsearch.SearchLanguageGroup
import com.programmersbox.kmpuiviews.presentation.globalsearch.SearchLanguages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Full name of a language code in the user's locale, e.g. "en" -> "English". Falls back to the code. */
expect fun languageDisplayName(code: String): String

data class LanguageOrderState(
    val groups: List<SearchLanguageGroup>,
    val defaults: List<String>,
    val overrides: Map<String, SourceLanguageOrder>,
) {
    /** Every language a multi-language extension offers that isn't in the default order yet. */
    val unranked: List<String> = groups.flatMap { it.languages }.distinct().filter { it !in defaults }.sorted()

    fun orderFor(packageName: String): List<String> = overrides[packageName]?.languages ?: defaults

    fun sort(sources: List<KmpSourceInformation>): List<KmpSourceInformation> =
        SourceLanguageOrdering.sort(sources, orderFor(sources.firstOrNull()?.packageName.orEmpty()))
}

fun languageOrderState(
    sourceRepository: SourceRepository,
    settings: NewSettingsHandling,
    deviceLanguage: String = Locale.current.language,
): Flow<LanguageOrderState> = combine(
    sourceRepository.sources,
    settings.sourceLanguageOrder,
    settings.sourceLanguageOrderOverrides,
) { sources, saved, overrides ->
    LanguageOrderState(
        groups = SearchLanguages.multiLanguageGroups(sources),
        defaults = SourceLanguageOrdering.resolveDefaults(saved, deviceLanguage),
        overrides = overrides,
    )
}

object SourceLanguageOrdering {
    /** Saved order, or the device language when nothing is saved. */
    fun resolveDefaults(saved: List<String>, deviceLanguage: String): List<String> =
        saved.ifEmpty { listOf(deviceLanguage) }

    /**
     * Ranked languages come first in their given order. A regional variant without its own rank
     * follows its base language (pt-BR after pt). Everything else is A–Z, then "all", then sources
     * with no language in their original order.
     */
    fun sort(sources: List<KmpSourceInformation>, ranked: List<String>): List<KmpSourceInformation> =
        sources.sortedWith(compareBy<KmpSourceInformation> { bucket(it.apiService.language, ranked) }
            .thenBy { rank(it.apiService.language, ranked) }
            .thenBy { it.apiService.language.orEmpty() })

    /** Whether [language] has a rank of its own or through its base language. */
    fun isRanked(language: String?, ranked: List<String>) = bucket(language, ranked) == 0

    /** [languages] in the order [sort] would put them. */
    fun order(languages: List<String>, ranked: List<String>): List<String> =
        languages.sortedWith(compareBy<String> { bucket(it, ranked) }.thenBy { rank(it, ranked) }.thenBy { it })

    private fun bucket(language: String?, ranked: List<String>) = when {
        language == null -> 3
        language == SearchLanguages.ALL -> 2
        rank(language, ranked) != Int.MAX_VALUE -> 0
        else -> 1
    }

    // Doubled so a variant (base rank + 1) sorts after its base language and before the next rank.
    private fun rank(language: String?, ranked: List<String>): Int {
        if (language == null) return Int.MAX_VALUE
        val exact = ranked.indexOf(language)
        if (exact != -1) return exact * 2
        val base = ranked.indexOf(language.substringBefore('-'))
        return if (base != -1) base * 2 + 1 else Int.MAX_VALUE
    }
}
