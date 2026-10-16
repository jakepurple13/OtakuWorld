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

/**
 * Index of the subtitle to show by default: first whose lang starts with [deviceLanguage]
 * (case-insensitive, so "en" matches "en", "en-US" and "English"), else the first English one,
 * else the first. Null when [subtitles] is empty.
 */
fun defaultSubtitleIndex(subtitles: List<KmpSubtitle>, deviceLanguage: String): Int? {
    if (subtitles.isEmpty()) return null
    val langs = subtitles.map { it.lang.trim().lowercase() }
    val device = deviceLanguage.trim().lowercase()
    if (device.isNotEmpty()) {
        langs.indexOfFirst { it.startsWith(device) }.takeIf { it >= 0 }?.let { return it }
    }
    langs.indexOfFirst { it.contains("english") || it == "en" || it.startsWith("en-") || it.startsWith("en_") }
        .takeIf { it >= 0 }?.let { return it }
    return 0
}
