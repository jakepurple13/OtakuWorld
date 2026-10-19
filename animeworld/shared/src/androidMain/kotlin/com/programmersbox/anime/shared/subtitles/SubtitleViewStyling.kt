package com.programmersbox.anime.shared.subtitles

import android.graphics.Color
import android.graphics.Typeface
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun SubtitleView.apply(spec: CaptionSpec) {
    val edgeType = when (spec.edge) {
        SubtitleEdge.None -> CaptionStyleCompat.EDGE_TYPE_NONE
        SubtitleEdge.Outline -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
        SubtitleEdge.Shadow -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
        SubtitleEdge.Raised -> CaptionStyleCompat.EDGE_TYPE_RAISED
    }
    val typeface = when (spec.font) {
        SubtitleFont.Default -> Typeface.DEFAULT
        SubtitleFont.Bold -> Typeface.DEFAULT_BOLD
        SubtitleFont.Serif -> Typeface.SERIF
        SubtitleFont.Mono -> Typeface.MONOSPACE
    }
    setStyle(
        CaptionStyleCompat(
            spec.foregroundArgb.toInt(),
            spec.backgroundArgb.toInt(),
            Color.TRANSPARENT,
            edgeType,
            Color.BLACK,
            typeface,
        )
    )
    setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * spec.textScale)
    setBottomPaddingFraction(spec.bottomPaddingFraction)
    setApplyEmbeddedStyles(spec.applyEmbedded)
    setApplyEmbeddedFontSizes(spec.applyEmbedded)
}
