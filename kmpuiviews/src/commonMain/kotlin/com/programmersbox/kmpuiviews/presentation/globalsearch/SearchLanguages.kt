package com.programmersbox.kmpuiviews.presentation.globalsearch

import androidx.compose.ui.text.intl.Locale
import com.programmersbox.datastore.GlobalSearchLanguageOverride
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** An installed extension that offers more than one language. */
data class SearchLanguageGroup(
    val packageName: String,
    val name: String,
    val languages: List<String>,
)

data class SearchLanguagesState(
    val sources: List<KmpSourceInformation>,
    val groups: List<SearchLanguageGroup>,
    val defaults: Set<String>,
    val overrides: Map<String, GlobalSearchLanguageOverride>,
) {
    /** Every language a multi-language extension offers, for the default chips. */
    val available: List<String> = groups.flatMap { it.languages }.distinct().sorted()

    fun sourcesToSearch() = SearchLanguages.sourcesToSearch(sources, defaults, overrides)
}

fun searchLanguagesState(
    sourceRepository: SourceRepository,
    settings: NewSettingsHandling,
    deviceLanguage: String = Locale.current.language,
): Flow<SearchLanguagesState> = combine(
    sourceRepository.sources,
    settings.globalSearchLanguages,
    settings.globalSearchLanguageOverrides,
) { sources, saved, overrides ->
    val groups = SearchLanguages.multiLanguageGroups(sources)
    SearchLanguagesState(
        sources = sources,
        groups = groups,
        defaults = SearchLanguages.resolveDefaults(saved, groups.flatMap { it.languages }, deviceLanguage),
        overrides = overrides,
    )
}

object SearchLanguages {
    /** Language code of a source that covers every language by itself. */
    const val ALL = "all"

    private fun KmpSourceInformation.lang() = apiService.language

    private fun List<KmpSourceInformation>.distinctLanguages() =
        mapNotNull { it.lang() }.filter { it != ALL }.distinct()

    fun multiLanguageGroups(sources: List<KmpSourceInformation>): List<SearchLanguageGroup> = sources
        .groupBy { it.packageName }
        .mapNotNull { (packageName, group) ->
            val languages = group.distinctLanguages()
            if (languages.size <= 1) return@mapNotNull null
            val first = group.first()
            SearchLanguageGroup(
                packageName = packageName,
                name = first.name.removeSuffix(" (${first.lang()})"),
                languages = languages.sorted(),
            )
        }

    /** Saved defaults, or the device language (and its regional variants) when nothing is saved. */
    fun resolveDefaults(saved: List<String>, available: Collection<String>, deviceLanguage: String): Set<String> =
        saved.toSet().ifEmpty {
            available
                .filter { it.substringBefore('-') == deviceLanguage }
                .toSet()
                .ifEmpty { setOf(deviceLanguage) }
        }

    fun effectiveLanguages(
        languages: List<String>,
        defaults: Set<String>,
        override: GlobalSearchLanguageOverride?,
    ): List<String> = when {
        override?.searchAll == true -> languages
        override != null -> languages.filter { it in override.languages }
        else -> languages.filter { it in defaults }
    }

    /**
     * Picks the sources global search queries. Only extensions with several languages are narrowed;
     * other extensions keep one source per package, as before.
     */
    fun sourcesToSearch(
        sources: List<KmpSourceInformation>,
        defaults: Set<String>,
        overrides: Map<String, GlobalSearchLanguageOverride>,
    ): List<KmpSourceInformation> = sources
        .groupBy { it.packageName }
        .flatMap { (packageName, group) ->
            val languages = group.distinctLanguages()
            when {
                languages.size > 1 -> {
                    val chosen = effectiveLanguages(languages, defaults, overrides[packageName])
                    group.filter { it.lang() == ALL || it.lang() in chosen }
                }

                group.any { it.lang() != null } -> group
                else -> listOf(group.first())
            }
        }
}
