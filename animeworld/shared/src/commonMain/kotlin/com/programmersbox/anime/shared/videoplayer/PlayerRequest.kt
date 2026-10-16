package com.programmersbox.anime.shared.videoplayer

import com.programmersbox.kmpmodels.KmpStorage

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
