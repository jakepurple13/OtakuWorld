package com.programmersbox.anime.shared.downloads

import kotlin.math.roundToInt

data class EpisodeTitle(val show: String, val episode: String?)

private const val EPISODE_SEPARATOR = " - Episode "
private const val DASH = " - "

/** Splits a file name (no extension) or request title into show and episode text. */
fun splitEpisodeTitle(name: String): EpisodeTitle {
    val episodeAt = name.indexOf(EPISODE_SEPARATOR)
    val splitAt = if (episodeAt >= 0) episodeAt else name.indexOf(DASH)
    if (splitAt < 0) return EpisodeTitle(name, null)
    val show = name.substring(0, splitAt)
    val episode = name.substring(splitAt + DASH.length).replaceFirst(DASH, " · ")
    return EpisodeTitle(show, episode)
}

fun isInFolder(filePath: String?, folder: String): Boolean {
    if (filePath == null) return false
    val prefix = if (folder.endsWith("/")) folder else "$folder/"
    return filePath.startsWith(prefix)
}

private val byteUnits = listOf("B", "KB", "MB", "GB")

private fun unitIndex(bytes: Long): Int {
    var value = bytes
    var index = 0
    while (value >= 1000 && index < byteUnits.lastIndex) {
        value /= 1000
        index++
    }
    return index
}

private fun formatInUnit(bytes: Long, unit: Int): String {
    var divisor = 1L
    repeat(unit) { divisor *= 1000 }
    return if (unit == byteUnits.lastIndex) {
        val tenths = bytes * 10 / divisor
        "${tenths / 10}.${tenths % 10}"
    } else {
        (bytes / divisor).toString()
    }
}

fun formatBytes(bytes: Long): String {
    val unit = unitIndex(bytes)
    return "${formatInUnit(bytes, unit)} ${byteUnits[unit]}"
}

enum class QueuedDownloadState { Running, Waiting, Failed }

data class QueuedDownload(
    val key: String,
    val title: String,
    val state: QueuedDownloadState,
    val progress: Float? = null,
    val bytesDownloaded: Long? = null,
    val totalBytes: Long? = null,
    val error: String? = null,
)

fun QueuedDownload.statusLabel(): String = when (state) {
    QueuedDownloadState.Waiting -> "Waiting"
    QueuedDownloadState.Failed -> error?.let { "Failed: $it" } ?: "Failed"
    QueuedDownloadState.Running -> {
        val downloadedSoFar = bytesDownloaded
        if (progress == null && downloadedSoFar != null) return formatBytes(downloadedSoFar)
        val percent = "${((progress ?: 0f) * 100).roundToInt()}%"
        val downloaded = bytesDownloaded
        val total = totalBytes
        when {
            downloaded == null -> percent
            total == null -> "$percent · ${formatBytes(downloaded)}"
            else -> {
                val unit = unitIndex(total)
                "$percent · ${formatInUnit(downloaded, unit)} of ${formatBytes(total)}"
            }
        }
    }
}
