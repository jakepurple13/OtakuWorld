package com.programmersbox.anime.shared.subtitles

/** Platform-neutral description of how captions are drawn. */
data class CaptionSpec(
    val foregroundArgb: Long,
    val backgroundArgb: Long,
    val edge: SubtitleEdge,
    val font: SubtitleFont,
    val textScale: Float,
    val bottomPaddingFraction: Float,
    val applyEmbedded: Boolean,
)

fun SubtitleStyle.toCaptionSpec(): CaptionSpec = CaptionSpec(
    foregroundArgb = textColor.argb,
    backgroundArgb = (backgroundPercent * 255 / 100).toLong() shl 24,
    edge = edge,
    font = font,
    textScale = sizePercent / 100f,
    bottomPaddingFraction = raisePercent / 100f,
    applyEmbedded = useEmbeddedStyles,
)
