package com.programmersbox.anime.shared.subtitles

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class SubtitleFont { Default, Bold, Serif, Mono }

enum class SubtitleColor(val argb: Long) {
    White(0xFFFFFFFFL),
    Yellow(0xFFFFE14DL),
    Cyan(0xFF7FE7FFL),
    Green(0xFF9BF08AL),
    Pink(0xFFFFB3D9L),
    Black(0xFF111111L),
}

enum class SubtitleEdge { None, Outline, Shadow, Raised }

@Serializable
data class SubtitleStyle(
    val sizePercent: Int = 100,
    val font: SubtitleFont = SubtitleFont.Default,
    val textColor: SubtitleColor = SubtitleColor.White,
    val edge: SubtitleEdge = SubtitleEdge.Outline,
    val backgroundPercent: Int = 0,
    val raisePercent: Int = 6,
    val useEmbeddedStyles: Boolean = false,
    val autoEnable: Boolean = true,
    val preferredLanguages: List<String> = emptyList(),
    val skipSignsAndSongs: Boolean = true,
)

private fun Int.clampedStep(min: Int, max: Int, step: Int): Int {
    val clamped = coerceIn(min, max)
    return (((clamped - min) + step / 2) / step * step + min).coerceIn(min, max)
}

fun SubtitleStyle.coerced(): SubtitleStyle = copy(
    sizePercent = sizePercent.clampedStep(50, 200, 10),
    backgroundPercent = backgroundPercent.clampedStep(0, 100, 5),
    raisePercent = raisePercent.coerceIn(0, 30),
)

private val subtitleJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

fun encodeSubtitleStyle(style: SubtitleStyle): String = subtitleJson.encodeToString(SubtitleStyle.serializer(), style)

/** Unknown, missing, or malformed JSON yields defaults. */
fun decodeSubtitleStyle(json: String?): SubtitleStyle {
    if (json.isNullOrBlank()) return SubtitleStyle()
    return runCatching { subtitleJson.decodeFromString(SubtitleStyle.serializer(), json).coerced() }
        .getOrDefault(SubtitleStyle())
}
