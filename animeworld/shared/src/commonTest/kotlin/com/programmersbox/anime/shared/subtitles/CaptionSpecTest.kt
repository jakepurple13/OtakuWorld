package com.programmersbox.anime.shared.subtitles

import kotlin.test.Test
import kotlin.test.assertEquals

class CaptionSpecTest {
    @Test
    fun toCaptionSpec_mapsValues() {
        val spec = SubtitleStyle(
            sizePercent = 150,
            backgroundPercent = 40,
            raisePercent = 10,
            textColor = SubtitleColor.Yellow,
            edge = SubtitleEdge.Shadow,
            font = SubtitleFont.Mono,
            useEmbeddedStyles = true,
        ).toCaptionSpec()
        assertEquals(0x66000000L, spec.backgroundArgb)
        assertEquals(1.5f, spec.textScale)
        assertEquals(0.10f, spec.bottomPaddingFraction)
        assertEquals(0xFFFFE14DL, spec.foregroundArgb)
        assertEquals(SubtitleEdge.Shadow, spec.edge)
        assertEquals(SubtitleFont.Mono, spec.font)
        assertEquals(true, spec.applyEmbedded)
    }
}
