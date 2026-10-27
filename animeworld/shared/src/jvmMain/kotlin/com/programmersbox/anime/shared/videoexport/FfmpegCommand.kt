package com.programmersbox.anime.shared.videoexport

import java.io.File

enum class ExportFormat(val ext: String, val muxer: String, val label: String) {
    Mp4("mp4", "mp4", "MP4"),
    Mkv("mkv", "matroska", "MKV"),
}

sealed interface SubtitleMode {
    data object None : SubtitleMode
    /** Every sidecar as a selectable track. */
    data object AsTracks : SubtitleMode
    /** The sidecar at [index] drawn into the picture; re-encodes the video. */
    data class BurnIn(val index: Int) : SubtitleMode
}

data class ExportOptions(val format: ExportFormat, val subtitles: SubtitleMode)

data class ExportSubtitle(val file: File, val label: String)

/**
 * The ffmpeg command line exporting [input] (plus its [subtitles] sidecars) to [output] as [options] asks.
 * Streams are copied unless a subtitle is burned in, which needs [burnSubtitle]: a copy of the chosen
 * sidecar at a filter-safe path. Progress is written to stdout as `key=value` blocks.
 */
fun ffmpegArgs(
    ffmpeg: String,
    input: File,
    subtitles: List<ExportSubtitle>,
    options: ExportOptions,
    output: File,
    burnSubtitle: File? = null,
): List<String> = buildList {
    val tracks = if (options.subtitles == SubtitleMode.AsTracks) subtitles else emptyList()
    add(ffmpeg)
    addAll(listOf("-hide_banner", "-nostdin", "-y", "-i", input.path))
    tracks.forEach { addAll(listOf("-i", it.file.path)) }
    addAll(listOf("-map", "0:v:0", "-map", "0:a?"))
    tracks.indices.forEach { addAll(listOf("-map", "${it + 1}:0")) }
    if (options.subtitles is SubtitleMode.BurnIn) {
        val sub = requireNotNull(burnSubtitle) { "burnSubtitle is required to burn in subtitles" }
        addAll(listOf("-vf", "subtitles=filename=${escapeFilterPath(sub.path)}"))
        addAll(listOf("-c:v", "libx264", "-crf", "20", "-preset", "medium"))
    } else {
        addAll(listOf("-c:v", "copy"))
    }
    addAll(listOf("-c:a", "copy"))
    if (tracks.isNotEmpty()) {
        addAll(listOf("-c:s", if (options.format == ExportFormat.Mp4) "mov_text" else "copy"))
        tracks.forEachIndexed { i, t -> addAll(listOf("-metadata:s:s:$i", "title=${t.label}")) }
    }
    if (options.format == ExportFormat.Mp4) addAll(listOf("-movflags", "+faststart"))
    addAll(listOf("-f", options.format.muxer, "-progress", "pipe:1", "-nostats", output.path))
}

/** Escapes [path] for a filter option value inside a filtergraph (both of ffmpeg's escaping levels). */
fun escapeFilterPath(path: String): String {
    val value = path.replace('\\', '/').replace("'", "\\'").replace(":", "\\:")
    return buildString {
        value.forEach { c ->
            if (c in "\\'[],;") append('\\')
            append(c)
        }
    }
}

data class FfmpegProgress(val positionMs: Long, val bytes: Long, val speed: Float?, val finished: Boolean)

/** Folds ffmpeg's `-progress` lines into one [FfmpegProgress] per block. */
class FfmpegProgressParser {
    private var positionMs = 0L
    private var bytes = 0L
    private var speed: Float? = null

    /** Returns the block's progress on its closing `progress=` line, else null. */
    fun line(line: String): FfmpegProgress? {
        val key = line.substringBefore('=').trim()
        val value = line.substringAfter('=', "").trim()
        when (key) {
            "out_time_us" -> value.toLongOrNull()?.let { positionMs = it / 1000 }
            "total_size" -> value.toLongOrNull()?.let { bytes = it }
            "speed" -> speed = value.removeSuffix("x").toFloatOrNull()
            "progress" -> return FfmpegProgress(positionMs, bytes, speed, finished = value == "end")
        }
        return null
    }
}

private val subtitlesFilterRegex = Regex("""^\s*\S+\s+subtitles\s""", RegexOption.MULTILINE)

/** Whether `ffmpeg -filters` output lists the `subtitles` filter. */
fun hasSubtitlesFilter(filters: String): Boolean = subtitlesFilterRegex.containsMatchIn(filters)

data class MediaInfo(val container: String, val durationMs: Long?, val video: String?, val audio: String?)

private val inputRegex = Regex("""Input #0, ([^,]+)""")
private val durationRegex = Regex("""Duration: (\d+):(\d{2}):(\d{2})\.(\d{2})""")
private val videoRegex = Regex("""Stream #0:\d+.*?: Video: (\w+).*?(\d{2,5})x(\d{2,5})""")
private val audioRegex = Regex("""Stream #0:\d+.*?: Audio: (\w+).*?Hz, ([\w.]+)""")

/** Reads the input summary `ffmpeg -i <file>` prints to stderr; null when ffmpeg couldn't open the file. */
fun parseProbe(stderr: String): MediaInfo? {
    val container = inputRegex.find(stderr)?.groupValues?.get(1) ?: return null
    val durationMs = durationRegex.find(stderr)?.destructured?.let { (h, m, s, cs) ->
        ((h.toLong() * 60 + m.toLong()) * 60 + s.toLong()) * 1000 + cs.toLong() * 10
    }
    val video = videoRegex.find(stderr)?.destructured?.let { (codec, w, h) -> "${codec.uppercase()} ${w}×$h" }
    val audio = audioRegex.find(stderr)?.destructured?.let { (codec, layout) -> "${codec.uppercase()} $layout" }
    return MediaInfo(container.substringBefore(',').uppercase(), durationMs, video, audio)
}
