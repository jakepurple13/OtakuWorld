package com.programmersbox.anime.shared.downloads

import kotlinx.serialization.Serializable

private val illegalFileChars = Regex("""[\\/:*?"<>|\u0000-\u001F\u007F]""")

private fun Int.utf8Size() = when {
    this < 0x80 -> 1
    this < 0x800 -> 2
    this < 0x10000 -> 3
    else -> 4
}

/** Truncates to at most [maxBytes] UTF-8 bytes without splitting a code point. */
private fun String.takeUtf8Bytes(maxBytes: Int): String {
    var bytes = 0
    var i = 0
    while (i < length) {
        val cp = codePointAtCompat(i)
        val size = cp.utf8Size()
        if (bytes + size > maxBytes) break
        bytes += size
        i += if (cp >= 0x10000) 2 else 1
    }
    return substring(0, i)
}

private fun String.codePointAtCompat(i: Int): Int {
    val c = this[i]
    if (c.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) {
        return 0x10000 + ((c.code - 0xD800) shl 10) + (this[i + 1].code - 0xDC00)
    }
    return c.code
}

fun sanitizeFileName(name: String, maxBytes: Int = 180): String {
    val cleaned = name
        .replace(illegalFileChars, " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .takeUtf8Bytes(maxBytes)
        .trimEnd { it == '.' || it.isWhitespace() }
    return cleaned.trimStart().ifEmpty { "download" }
}

fun uniqueBaseName(base: String, exists: (String) -> Boolean): String {
    if (!exists(base)) return base
    var n = 2
    while (exists("$base ($n)")) n++
    return "$base ($n)"
}

private val videoExtensions = listOf("mp4", "ts", "mkv", "webm")

/** True when [base] is used by any video container (or its `.part`), so containers never share a sidecar base. */
fun isBaseTaken(base: String, exists: (String) -> Boolean): Boolean =
    videoExtensions.any { exists("$base.$it") || exists("$base.$it.part") } || exists(sidecarIndexName(base))

enum class SubtitleFormat(val ext: String, val mime: String) {
    Vtt("vtt", "text/vtt"),
    Srt("srt", "application/x-subrip"),
    Ass("ass", "text/x-ssa"),
}

fun subtitleFormat(url: String, firstBytes: String): SubtitleFormat {
    val path = url.substringBefore('#').substringBefore('?')
    when (path.substringAfterLast('.', "").lowercase()) {
        "vtt" -> return SubtitleFormat.Vtt
        "srt" -> return SubtitleFormat.Srt
        "ass", "ssa" -> return SubtitleFormat.Ass
    }
    val head = firstBytes.trimStart('﻿', ' ', '\n', '\r', '\t')
    return when {
        head.startsWith("WEBVTT") -> SubtitleFormat.Vtt
        head.startsWith("[Script Info]", ignoreCase = true) -> SubtitleFormat.Ass
        else -> SubtitleFormat.Srt
    }
}

fun sidecarName(videoBase: String, index: Int, label: String, format: SubtitleFormat): String =
    "$videoBase.$index.${sanitizeFileName(label, 40)}.${format.ext}"

fun sidecarIndexName(videoBase: String): String = "$videoBase.subtitles.json"

@Serializable
data class SidecarEntry(val file: String, val lang: String, val label: String)
