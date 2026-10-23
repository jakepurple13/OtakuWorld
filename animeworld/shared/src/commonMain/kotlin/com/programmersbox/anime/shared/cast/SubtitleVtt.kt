package com.programmersbox.anime.shared.cast

import com.programmersbox.anime.shared.videoplayer.subtitleMime

enum class SubtitleFormat {
    Vtt, Srt, Ass;

    companion object {
        fun fromUrl(url: String): SubtitleFormat = when (subtitleMime(url)) {
            "application/x-subrip" -> Srt
            "text/x-ssa" -> Ass
            else -> Vtt
        }
    }
}

data class VttCue(val startMs: Long, val endMs: Long, val text: String)

/** Converts [text] to WebVTT. VTT input is returned unchanged. */
fun toWebVtt(text: String, format: SubtitleFormat): String {
    if (format == SubtitleFormat.Vtt) return text
    val clean = text.removePrefix("﻿").replace("\r\n", "\n").replace('\r', '\n')
    return when (format) {
        SubtitleFormat.Srt -> srtToVtt(clean)
        else -> assToVtt(clean)
    }
}

private fun srtToVtt(text: String): String = buildString {
    append("WEBVTT\n\n")
    text.lines().forEach { line ->
        append(if ("-->" in line) line.replace(',', '.') else line).append('\n')
    }
}

private val assOverride = Regex("""\{[^}]*}""")

private fun assToVtt(text: String): String = buildString {
    append("WEBVTT\n\n")
    var columns = listOf("layer", "start", "end", "style", "name", "marginl", "marginr", "marginv", "effect", "text")
    var inEvents = false
    for (raw in text.lines()) {
        val line = raw.trim()
        if (line.startsWith("[")) {
            inEvents = line.equals("[Events]", ignoreCase = true)
            continue
        }
        if (!inEvents) continue
        if (line.startsWith("Format:", ignoreCase = true)) {
            columns = line.substringAfter(':').split(',').map { it.trim().lowercase() }
        } else if (line.startsWith("Dialogue:", ignoreCase = true)) {
            val start = columns.indexOf("start")
            val end = columns.indexOf("end")
            val textColumn = columns.indexOf("text")
            if (start < 0 || end < 0 || textColumn < 0) continue
            val fields = line.substringAfter(':').trimStart().split(',', limit = textColumn + 1)
            if (fields.size <= textColumn) continue
            val startMs = parseTimestamp(fields[start]) ?: continue
            val endMs = parseTimestamp(fields[end]) ?: continue
            val cueText = fields[textColumn]
                .replace(assOverride, "")
                .replace("\\N", "\n")
                .replace("\\n", "\n")
                .replace("\\h", " ")
                .trim()
            if (cueText.isEmpty()) continue
            append(formatTimestamp(startMs)).append(" --> ").append(formatTimestamp(endMs)).append('\n')
            append(cueText).append("\n\n")
        }
    }
}

/** Parses `[hh:]mm:ss[.,]fraction`; the fraction may have any number of digits (ASS uses 2). */
private fun parseTimestamp(raw: String): Long? {
    val value = raw.trim().replace(',', '.')
    val whole = value.substringBefore('.')
    val fraction = value.substringAfter('.', "")
    val parts = whole.split(':').map { it.toLongOrNull() ?: return null }
    if (parts.size !in 2..3) return null
    val seconds = parts.fold(0L) { acc, part -> acc * 60 + part }
    val millis = if (fraction.isEmpty()) 0L else fraction.take(3).padEnd(3, '0').toLongOrNull() ?: return null
    return seconds * 1000 + millis
}

private fun formatTimestamp(ms: Long): String {
    fun two(n: Long) = n.toString().padStart(2, '0')
    return "${two(ms / 3_600_000)}:${two(ms / 60_000 % 60)}:${two(ms / 1000 % 60)}.${(ms % 1000).toString().padStart(3, '0')}"
}

fun parseVttCues(vtt: String): List<VttCue> {
    val lines = vtt.removePrefix("﻿").replace("\r\n", "\n").replace('\r', '\n').lines()
    val cues = mutableListOf<VttCue>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        if ("-->" !in line) {
            i++
            continue
        }
        val start = parseTimestamp(line.substringBefore("-->"))
        val end = parseTimestamp(line.substringAfter("-->").trim().substringBefore(' '))
        i++
        val body = mutableListOf<String>()
        while (i < lines.size && lines[i].isNotBlank()) body += lines[i++]
        if (start != null && end != null) cues += VttCue(start, end, body.joinToString("\n"))
    }
    return cues
}

/** Text of the cue showing at [positionMs] (end exclusive), or null between cues. */
fun List<VttCue>.cueAt(positionMs: Long): String? =
    firstOrNull { positionMs >= it.startMs && positionMs < it.endMs }?.text
