package com.programmersbox.anime.shared.subtitlegen

import kotlin.test.Test
import kotlin.test.assertEquals

class CoercedRequestTest {
    private fun lang(code: String) = whisperLanguages.first { it.code == code }

    @Test
    fun validRequestIsKept() {
        val request = GenerationRequest(lang("ja"), lang("en"))
        assertEquals(request, request.coerced(englishOnlyModel = false))
    }

    @Test
    fun englishOnlyModelFixesFromToEnglish() {
        val coerced = GenerationRequest(lang("ja"), lang("ja")).coerced(englishOnlyModel = true)
        assertEquals("en" to "en", coerced.from.code to coerced.to.code)
    }

    @Test
    fun unofferedTargetFallsBackToFirstEnabled() {
        // Japanese isn't offered for Korean speech, so To falls back to English.
        val coerced = GenerationRequest(lang("ko"), lang("ja")).coerced(englishOnlyModel = false)
        assertEquals("en", coerced.to.code)
    }

    @Test
    fun autoTargetUsesSameAsSpokenLabel() {
        val coerced = GenerationRequest(WhisperAuto, WhisperAuto).coerced(englishOnlyModel = false)
        assertEquals(WhisperLanguage("auto", "Same as spoken"), coerced.to)
    }
}
