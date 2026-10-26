// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
// OTAKU: `actual` modifiers removed; the commonMain `expect` is not vendored.
package com.lagradost.cloudstream3.utils

import java.util.Locale

// TODO: add androidMain using LocaleListCompat.getAdjustedDefault() to respect
// per-app language preferences set via AppCompatDelegate.setApplicationLocales()

fun getCurrentLocale(): String =
    Locale.getDefault().toLanguageTag()

fun localizedLanguageName(ietfTag: String, localizedTo: String): String? {
    val localeOfLangCode = Locale.forLanguageTag(ietfTag)
    val localeOfLocalizeTo = Locale.forLanguageTag(localizedTo)
    val displayName = localeOfLangCode.getDisplayName(localeOfLocalizeTo)

    // Locale.getDisplayName() falls back to the raw tag or "language (country)" form
    // when it doesn't know how to render the name.
    val langCodeWithCountry = "${localeOfLangCode.language} ("
    val failed =
        displayName.equals(ietfTag, ignoreCase = true) ||
        displayName.contains(langCodeWithCountry, ignoreCase = true)

    return if (failed) null else displayName
}