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

fun streamMime(storage: KmpStorage): String? = storage.mimeType

/** Label fragments marking a track that only covers part of the dialogue. */
private val PARTIAL_SUBTITLE_MARKERS = listOf("sign", "song", "forced", "dubtitle", "cc only")

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
 * Tries the device language, then English, then any track. Within each step, full-dialogue
 * tracks win over partial ones (signs/songs, forced, dubtitles, "CC only"); a partial track is
 * picked only when it is the only match.
 */
fun defaultSubtitleIndex(
    subtitles: List<KmpSubtitle>,
    deviceLanguage: String,
    deviceLanguageName: String = "",
): Int? {
    if (subtitles.isEmpty()) return null
    val labels = subtitles.map { letterTokens(it.lang) }
    val partial = subtitles.map { s -> PARTIAL_SUBTITLE_MARKERS.any { s.lang.contains(it, ignoreCase = true) } }

    fun pick(candidates: List<Int>): Int? = candidates.firstOrNull { !partial[it] } ?: candidates.firstOrNull()

    fun matching(vararg queries: String): Int? {
        val phrases = queries.map(::letterTokens).filter { it.isNotEmpty() }
        return pick(
            labels.indices.filter { i -> phrases.any { phrase -> labels[i].windowed(phrase.size).any { it == phrase } } }
        )
    }

    val code = deviceLanguage.trim().substringBefore('-').substringBefore('_')
    return matching(code, deviceLanguageName) ?: matching("en", "english") ?: pick(subtitles.indices.toList())
}
