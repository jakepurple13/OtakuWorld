package com.programmersbox.anime.shared.downloads

import kotlinx.serialization.Serializable

private val illegalFileChars = Regex("""[\\/:*?"<>|\u0000-\u001F]""")

fun sanitizeFileName(name: String, maxLength: Int = 120): String {
    val cleaned = name
        .replace(illegalFileChars, " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(maxLength)
        .trim()
        .trimEnd('.')
    return cleaned.ifEmpty { "download" }
}

fun uniqueBaseName(base: String, exists: (String) -> Boolean): String {
    if (!exists(base)) return base
    var n = 2
    while (exists("$base ($n)")) n++
    return "$base ($n)"
}

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
