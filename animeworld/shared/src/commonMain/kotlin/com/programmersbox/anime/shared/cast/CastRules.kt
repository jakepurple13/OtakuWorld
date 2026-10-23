package com.programmersbox.anime.shared.cast

import com.programmersbox.anime.shared.subtitles.SubtitleEdge
import com.programmersbox.anime.shared.subtitles.SubtitleFont
import com.programmersbox.anime.shared.subtitles.SubtitleStyle
import com.programmersbox.anime.shared.videoplayer.PARTIAL_SUBTITLE_MARKERS
import com.programmersbox.kmpmodels.KmpSubtitle
import kotlin.math.roundToInt

sealed interface CastCompat {
    data object Ok : CastCompat
    data object Blocked : CastCompat
}

private fun urlExtension(url: String): String =
    url.substringBefore('#').substringBefore('?').substringAfterLast('/').substringAfterLast('.', "").lowercase()

private fun isHls(url: String, mimeType: String?): Boolean =
    urlExtension(url) == "m3u8" || mimeType?.contains("mpegurl", ignoreCase = true) == true

/** Cast receivers cannot play Matroska. */
fun castCompatibility(url: String, mimeType: String?): CastCompat {
    val mime = mimeType?.lowercase()
    return if (mime == "video/x-matroska" || mime == "video/mkv" || urlExtension(url) == "mkv") {
        CastCompat.Blocked
    } else {
        CastCompat.Ok
    }
}

fun castContentType(url: String, mimeType: String?): String {
    if (!mimeType.isNullOrBlank()) return mimeType
    return when (urlExtension(url)) {
        "m3u8" -> "application/x-mpegURL"
        "mpd" -> "application/dash+xml"
        "webm" -> "video/webm"
        else -> "video/mp4"
    }
}

/** HLS always goes through the proxy (playlist rewrite); other streams only when headers must be added. */
fun needsProxy(url: String, mimeType: String?, headers: Map<String, String>): Boolean =
    isHls(url, mimeType) || headers.isNotEmpty()

/**
 * Index of the subtitle closest to the active track, or null if none match.
 *
 * [language] is an ISO code matched as a prefix of `lang` (`en` vs `en-US`); failing that,
 * [languageName] or [label] contained in `lang` (case-insensitive). Signs/songs-style tracks lose to full ones.
 */
fun closestSubtitleIndex(
    language: String?,
    label: String?,
    subtitles: List<KmpSubtitle>,
    languageName: String? = null,
): Int? {
    fun pick(candidates: List<Int>): Int? =
        candidates.firstOrNull { i -> PARTIAL_SUBTITLE_MARKERS.none { subtitles[i].lang.contains(it, ignoreCase = true) } }
            ?: candidates.firstOrNull()

    val code = language?.trim()?.substringBefore('-')?.substringBefore('_')?.lowercase().orEmpty()
    if (code.isNotEmpty()) {
        pick(
            subtitles.indices.filter { i ->
                val lang = subtitles[i].lang.trim().lowercase()
                lang == code || lang.startsWith("$code-") || lang.startsWith("${code}_")
            }
        )?.let { return it }
    }
    val names = listOfNotNull(languageName, label).map { it.trim() }.filter { it.isNotEmpty() }
    return pick(subtitles.indices.filter { i -> names.any { subtitles[i].lang.contains(it, ignoreCase = true) } })
}

enum class CastEdge { None, Outline, DropShadow, Raised }

enum class CastFont { SansSerif, Serif, Monospace }

data class CastTextStyle(
    val fontScale: Float,
    val foregroundArgb: Long,
    val backgroundArgb: Long,
    val edge: CastEdge,
    val edgeArgb: Long,
    val font: CastFont,
)

fun SubtitleStyle.toCastTextStyle(): CastTextStyle = CastTextStyle(
    fontScale = sizePercent / 100f,
    foregroundArgb = textColor.argb,
    backgroundArgb = (backgroundPercent * 255 / 100.0).roundToInt().toLong() shl 24,
    edge = when (edge) {
        SubtitleEdge.None -> CastEdge.None
        SubtitleEdge.Outline -> CastEdge.Outline
        SubtitleEdge.Shadow -> CastEdge.DropShadow
        SubtitleEdge.Raised -> CastEdge.Raised
    },
    edgeArgb = 0xFF000000L,
    font = when (font) {
        SubtitleFont.Default, SubtitleFont.Bold -> CastFont.SansSerif
        SubtitleFont.Serif -> CastFont.Serif
        SubtitleFont.Mono -> CastFont.Monospace
    },
)

object CastJumps {
    const val BACK_15 = -15_000L
    const val BACK_10 = -10_000L
    const val OPENING = 85_000L
    const val FORWARD_15 = 15_000L
}
