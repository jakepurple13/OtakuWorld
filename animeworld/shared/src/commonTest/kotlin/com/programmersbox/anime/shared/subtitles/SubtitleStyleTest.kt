package com.programmersbox.anime.shared.subtitles

import kotlin.test.Test
import kotlin.test.assertEquals

class SubtitleStyleTest {

    @Test
    fun defaultsMatchSpec() {
        val s = SubtitleStyle()
        assertEquals(100, s.sizePercent)
        assertEquals(SubtitleFont.Default, s.font)
        assertEquals(SubtitleColor.White, s.textColor)
        assertEquals(SubtitleEdge.Outline, s.edge)
        assertEquals(0, s.backgroundPercent)
        assertEquals(6, s.raisePercent)
        assertEquals(false, s.useEmbeddedStyles)
        assertEquals(true, s.autoEnable)
        assertEquals(emptyList(), s.preferredLanguages)
        assertEquals(true, s.skipSignsAndSongs)
        assertEquals(0xFFFFFFFFL, SubtitleColor.White.argb)
        assertEquals(0xFFFFE14DL, SubtitleColor.Yellow.argb)
        assertEquals(0xFF7FE7FFL, SubtitleColor.Cyan.argb)
        assertEquals(0xFF9BF08AL, SubtitleColor.Green.argb)
        assertEquals(0xFFFFB3D9L, SubtitleColor.Pink.argb)
        assertEquals(0xFF111111L, SubtitleColor.Black.argb)
    }

    @Test
    fun coercedClampsAndSteps() {
        fun size(v: Int) = SubtitleStyle(sizePercent = v).coerced().sizePercent
        fun bg(v: Int) = SubtitleStyle(backgroundPercent = v).coerced().backgroundPercent
        fun raise(v: Int) = SubtitleStyle(raisePercent = v).coerced().raisePercent
        assertEquals(50, size(37))
        assertEquals(200, size(205))
        assertEquals(110, size(113))
        assertEquals(100, bg(103))
        assertEquals(5, bg(7))
        assertEquals(0, raise(-1))
        assertEquals(30, raise(31))
    }

    @Test
    fun jsonRoundTrip() {
        val s = SubtitleStyle(
            sizePercent = 130,
            font = SubtitleFont.Mono,
            textColor = SubtitleColor.Cyan,
            edge = SubtitleEdge.Raised,
            backgroundPercent = 40,
            raisePercent = 12,
            useEmbeddedStyles = true,
            autoEnable = false,
            preferredLanguages = listOf("ja", "en"),
            skipSignsAndSongs = false,
        )
        assertEquals(s, decodeSubtitleStyle(encodeSubtitleStyle(s)))
    }

    @Test
    fun unknownJsonFallsBackToDefaults() {
        assertEquals(SubtitleStyle(), decodeSubtitleStyle("""{"sizePercent":"x"}"""))
        assertEquals(SubtitleStyle(), decodeSubtitleStyle(null))
        assertEquals(SubtitleStyle(), decodeSubtitleStyle("not json"))
    }
}
