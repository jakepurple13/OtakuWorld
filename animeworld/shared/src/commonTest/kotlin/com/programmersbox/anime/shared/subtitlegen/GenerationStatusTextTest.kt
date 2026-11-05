package com.programmersbox.anime.shared.subtitlegen

import kotlin.test.Test
import kotlin.test.assertEquals

class GenerationStatusTextTest {
    private fun lang(code: String) = whisperLanguages.first { it.code == code }

    @Test
    fun queuedNamesTheEpisodeAhead() {
        assertEquals("Queued · starts after Frieren · Episode 3", generationStatusText(GenerationStatus.Queued("Frieren · Episode 3")))
    }

    @Test
    fun extractingAudio() {
        assertEquals("Extracting audio…", generationStatusText(GenerationStatus.ExtractingAudio))
    }

    @Test
    fun downloadingModelShowsPercent() {
        assertEquals("Downloading Small · 42%", generationStatusText(GenerationStatus.DownloadingModel("Small", 0.425f)))
    }

    @Test
    fun translatingShowsBothLanguagesAndRoundsMinutesUp() {
        val status = GenerationStatus.Transcribing(lang("ja"), lang("en"), 0.3f, 4 * 60_000L + 1)
        assertEquals("Japanese → English · 30% · about 5 min left", generationStatusText(status))
    }

    @Test
    fun sameLanguageSaysTranscribing() {
        val status = GenerationStatus.Transcribing(lang("ja"), lang("ja"), 0.5f, 30_000L)
        assertEquals("Transcribing Japanese · 50% · less than a minute", generationStatusText(status))
    }

    @Test
    fun autoTargetSaysTranscribingAndNoEtaWhenUnknown() {
        val status = GenerationStatus.Transcribing(WhisperAuto, WhisperLanguage("auto", "Same as spoken"), 0f, null)
        assertEquals("Transcribing · 0%", generationStatusText(status))
    }

    @Test
    fun autoDetectTranslatingNamesTarget() {
        val status = GenerationStatus.Transcribing(WhisperAuto, lang("en"), 0.1f, null)
        assertEquals("Auto-detect → English · 10%", generationStatusText(status))
    }

    @Test
    fun failedShowsMessage() {
        val status = GenerationStatus.Failed("ffmpeg not found", GenerationRequest(lang("ja"), lang("en")))
        assertEquals("Couldn't generate subtitles: ffmpeg not found", generationStatusText(status))
    }

    @Test
    fun remainingTextBoundaries() {
        assertEquals("less than a minute", remainingText(59_999))
        assertEquals("about 1 min left", remainingText(60_000))
        assertEquals("about 2 min left", remainingText(60_001))
    }
}
