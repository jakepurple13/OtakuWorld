package com.programmersbox.anime.shared.subtitlegen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WhisperModelsTest {
    @Test
    fun choiceRoundTrips() {
        assertEquals(
            WhisperModelChoice.BuiltIn(BuiltInWhisperModel.MEDIUM),
            decodeModelChoice(WhisperModelChoice.BuiltIn(BuiltInWhisperModel.MEDIUM).encode())
        )
        assertEquals(WhisperModelChoice.Custom("/m/a b.bin"), decodeModelChoice("file:/m/a b.bin"))
    }

    @Test
    fun unknownChoiceFallsBackToSmall() {
        assertEquals(WhisperModelChoice.BuiltIn(BuiltInWhisperModel.SMALL), decodeModelChoice("builtin:huge"))
        assertEquals(WhisperModelChoice.BuiltIn(BuiltInWhisperModel.SMALL), decodeModelChoice(""))
    }

    @Test
    fun headerAcceptsGgmlAndGguf() {
        assertTrue(isWhisperModelHeader(byteArrayOf(0x6c, 0x6d, 0x67, 0x67)))
        assertTrue(isWhisperModelHeader("GGUF".encodeToByteArray()))
        assertFalse(isWhisperModelHeader("WEBV".encodeToByteArray()))
        assertFalse(isWhisperModelHeader(byteArrayOf(0x6c, 0x6d)))
    }

    @Test
    fun englishOnlyDetection() {
        assertTrue(isEnglishOnlyModel("ggml-small.en.bin"))
        assertFalse(isEnglishOnlyModel("ggml-small.bin"))
        assertFalse(isEnglishOnlyModel("ggml-kotoba-whisper-v2.0.bin"))
    }

    @Test
    fun customName() {
        assertEquals("kotoba-whisper-v2.0", customModelName("ggml-kotoba-whisper-v2.0.bin"))
    }

    @Test
    fun catalogMatchesSpec() {
        assertEquals(
            listOf("tiny", "base", "small", "medium", "large-v3", "large-v3-turbo"),
            BuiltInWhisperModel.entries.map { it.id }
        )
        assertEquals(listOf(BuiltInWhisperModel.SMALL), BuiltInWhisperModel.entries.filter { it.recommended })
        assertEquals(
            listOf(BuiltInWhisperModel.LARGE_V3_TURBO),
            BuiltInWhisperModel.entries.filter { it.sameLanguageOnly }
        )
    }
}
