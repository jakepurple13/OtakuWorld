package com.programmersbox.anime.shared.videoplayer

import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpmodels.KmpSubtitle

val DEFAULT_PLAYER_HEADERS: Map<String, String> = mapOf(
    "accept" to "*/*",
    "sec-ch-ua" to "\"Chromium\";v=\"91\", \" Not;A Brand\";v=\"99\"",
    "sec-ch-ua-mobile" to "?0",
    "sec-fetch-user" to "?1",
    "sec-fetch-mode" to "navigate",
    "sec-fetch-dest" to "video",
)

/** Defaults, then [referer] if non-blank, then [sourceHeaders]. Keys compare case-insensitively; later wins, its casing kept. */
fun playerHeaders(sourceHeaders: Map<String, String>, referer: String?): Map<String, String> {
    val result = LinkedHashMap<String, String>()
    fun put(key: String, value: String) {
        result.keys.firstOrNull { it.equals(key, ignoreCase = true) }?.let(result::remove)
        result[key] = value
    }
    DEFAULT_PLAYER_HEADERS.forEach(::put)
    if (!referer.isNullOrBlank()) put("referer", referer)
    sourceHeaders.forEach(::put)
    return result
}

fun subtitleMime(url: String): String =
    when (url.substringBefore('#').substringBefore('?').substringAfterLast('/').substringAfterLast('.', "").lowercase()) {
        "vtt" -> "text/vtt"
        "srt" -> "application/x-subrip"
        "ass", "ssa" -> "text/x-ssa"
        else -> "text/vtt"
    }

/** Name for an in-stream text track; a blank or "Unknown" label is not shown as-is. */
fun embeddedTrackLabel(label: String?, language: String?): String {
    if (!label.isNullOrBlank() && !label.equals("Unknown", ignoreCase = true)) return label
    return if (language.isNullOrBlank()) "Embedded" else "Embedded ($language)"
}

fun streamMime(storage: KmpStorage): String? = storage.mimeType

/** Label fragments marking a track that only covers part of the dialogue. */
private val PARTIAL_SUBTITLE_MARKERS = listOf("sign", "song", "forced", "dubtitle", "cc only")

/** English names for common language codes, so a preferred code also matches a "Japanese"-style label. */
private val LANGUAGE_NAMES = mapOf(
    "ja" to "Japanese", "en" to "English", "es" to "Spanish", "fr" to "French", "de" to "German",
    "pt" to "Portuguese", "it" to "Italian", "ru" to "Russian", "ko" to "Korean", "zh" to "Chinese",
    "ar" to "Arabic", "id" to "Indonesian", "tr" to "Turkish", "vi" to "Vietnamese", "th" to "Thai",
    "pl" to "Polish", "hi" to "Hindi",
)

/** Lowercase runs of letters; everything else separates tokens ("0_en" -> ["en"]). */
private fun letterTokens(text: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    for (c in text.lowercase()) {
        if (c.isLetter()) {
            current.append(c)
        } else if (current.isNotEmpty()) {
            tokens += current.toString()
            current.clear()
        }
    }
    if (current.isNotEmpty()) tokens += current.toString()
    return tokens
}

/**
 * Index of the subtitle to show by default, or null when [subtitles] is empty.
 *
 * A lang matches when [deviceLanguage] (a code like "en"; any region suffix is dropped) or
 * [deviceLanguageName] (like "English") appears in it as whole words, case-insensitively.
 * Tries each of [preferredLanguages] in order, then the device language, then English, then any track. Within each step, full-dialogue
 * tracks win over partial ones (signs/songs, forced, dubtitles, "CC only"); a partial track is
 * picked only when it is the only match
 * (or never demoted when [skipSignsAndSongs] is false).
 */
fun defaultSubtitleIndex(
    subtitles: List<KmpSubtitle>,
    deviceLanguage: String,
    deviceLanguageName: String = "",
    preferredLanguages: List<String> = emptyList(),
    skipSignsAndSongs: Boolean = true,
): Int? {
    if (subtitles.isEmpty()) return null
    val labels = subtitles.map { letterTokens(it.lang) }
    val partial = subtitles.map { s -> skipSignsAndSongs && PARTIAL_SUBTITLE_MARKERS.any { s.lang.contains(it, ignoreCase = true) } }

    fun pick(candidates: List<Int>): Int? = candidates.firstOrNull { !partial[it] } ?: candidates.firstOrNull()

    fun matching(vararg queries: String): Int? {
        val phrases = queries.map(::letterTokens).filter { it.isNotEmpty() }
        return pick(
            labels.indices.filter { i -> phrases.any { phrase -> labels[i].windowed(phrase.size).any { it == phrase } } }
        )
    }

    val code = deviceLanguage.trim().substringBefore('-').substringBefore('_')
    for (preferred in preferredLanguages) {
        val preferredCode = preferred.trim().substringBefore('-').substringBefore('_').lowercase()
        matching(preferredCode, LANGUAGE_NAMES[preferredCode].orEmpty())?.let { return it }
    }
    return matching(code, deviceLanguageName) ?: matching("en", "english") ?: pick(subtitles.indices.toList())
}
