package com.programmersbox.anime.shared.subtitlegen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TargetOptionsTest {
    private fun lang(code: String) = whisperLanguages.first { it.code == code }

    @Test
    fun japaneseOffersEnglishTranslatedAndJapaneseTranscribed() {
        val enabled = targetOptions(lang("ja")).filter { it.enabled }
        assertEquals(
            listOf("en" to "Translated", "ja" to "Transcribed as spoken"),
            enabled.map { it.language.code to it.note }
        )
    }

    @Test
    fun otherLanguagesAreDisabledWithAddOnNote() {
        val disabled = targetOptions(lang("ja")).filterNot { it.enabled }
        assertEquals(listOf("ko", "zh", "es"), disabled.map { it.language.code })
        assertTrue(disabled.all { it.note == "Needs a translation add-on" })
    }

    @Test
    fun englishFromOffersOnlyEnglishTranscribed() {
        val enabled = targetOptions(lang("en")).filter { it.enabled }
        assertEquals(listOf("en" to "Transcribed as spoken"), enabled.map { it.language.code to it.note })
    }

    @Test
    fun autoOffersEnglishAndSameAsSpoken() {
        val enabled = targetOptions(WhisperAuto).filter { it.enabled }
        assertEquals(listOf("en", "auto"), enabled.map { it.language.code })
        assertEquals("Same as spoken", enabled[1].language.name)
    }

    @Test
    fun englishOnlyModelLocksFromToEnglish() {
        assertEquals(listOf("en"), fromOptions(englishOnlyModel = true).map { it.code })
        assertEquals(6, fromOptions(englishOnlyModel = false).size)
    }

    @Test
    fun translateOnlyWhenTargetIsEnglishAndSourceIsNot() {
        assertTrue(GenerationRequest(lang("ja"), lang("en")).translate)
        assertFalse(GenerationRequest(lang("en"), lang("en")).translate)
        assertFalse(GenerationRequest(lang("ja"), lang("ja")).translate)
    }
}
