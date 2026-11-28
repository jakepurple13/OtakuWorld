package com.programmersbox.kmpuiviews.domain.aitext

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiFeatureTest {
    @Test
    fun `each rewrite style has its own prompt`() {
        val prompts = RewriteStyle.entries.map { AiFeature.Rewrite(it).system }
        assertEquals(RewriteStyle.entries.size, prompts.toSet().size)
        assertTrue(prompts.all { it.isNotBlank() })
    }

    @Test
    fun `tldr prepares head and wraps prompt`() {
        val prepared = AiFeature.Tldr.prepare("a ".repeat(3_000))
        assertTrue(prepared.length <= AiTextPrompts.NANO_TEXT_LIMIT)
        assertTrue(AiFeature.Tldr.userPrompt(prepared).startsWith("Description:\n"))
    }

    @Test
    fun `recap prepares tail and wraps prompt`() {
        val prepared = AiFeature.Recap.prepare("b ".repeat(3_000))
        assertTrue(prepared.length <= AiTextPrompts.RECAP_TAIL_CHARS)
        assertTrue(AiFeature.Recap.userPrompt(prepared).startsWith("Chapter ending:\n"))
    }

    @Test
    fun `list names caps titles and parses on finish`() {
        assertEquals(AiTextPrompts.MAX_LIST_TITLES, AiFeature.ListNames.prepare(List(40) { "t$it" }).lines().size)
        assertTrue(AiFeature.ListNames.userPrompt("x").startsWith("Titles in the list:\n"))
        assertEquals("A\nB", AiFeature.ListNames.finish("1. A\n2. a\n- B"))
    }

    @Test
    fun `list description keeps raw output`() {
        assertEquals("A nice list.", AiFeature.ListDescription.finish("A nice list."))
        assertTrue(AiFeature.ListDescription.userPrompt("x").startsWith("Titles in the list:\n"))
    }

    @Test
    fun `only proofread and rewrite cap nano length`() {
        assertEquals(AiTextPrompts.NANO_TEXT_LIMIT, AiFeature.Proofread.nanoMaxChars)
        assertEquals(AiTextPrompts.NANO_TEXT_LIMIT, AiFeature.Rewrite(RewriteStyle.Shorten).nanoMaxChars)
        listOf(AiFeature.Recap, AiFeature.Tldr, AiFeature.ListNames, AiFeature.ListDescription)
            .forEach { assertNull(it.nanoMaxChars) }
    }

    @Test
    fun `explain error prepares source and capped detail`() {
        val prepared = AiFeature.ExplainError.prepare(ErrorDetails("MangaDex", "x".repeat(5_000)))
        assertTrue(prepared.startsWith("Source: MangaDex\nError:\n"))
        assertEquals("Source: MangaDex\nError:\n".length + 1_500, prepared.length)
        assertNull(AiFeature.ExplainError.nanoTask)
        assertNull(AiFeature.ExplainError.nanoMaxChars)
    }

    @Test
    fun `nano tasks map to ml kit apis`() {
        assertEquals(NanoTask.Summarize, AiFeature.Tldr.nanoTask)
        assertEquals(NanoTask.Proofread, AiFeature.Proofread.nanoTask)
        assertEquals(NanoTask.Rewrite(RewriteStyle.Emojify), AiFeature.Rewrite(RewriteStyle.Emojify).nanoTask)
        listOf(AiFeature.Recap, AiFeature.ListNames, AiFeature.ListDescription).forEach { assertNull(it.nanoTask) }
    }

    @Test
    fun `update summary prepares at most 25 lines and wraps prompt`() {
        val prepared = AiFeature.UpdateSummary.prepare(List(40) { "Title $it · 1 new chapter: Ch $it" })
        assertEquals(25, prepared.lines().size)
        assertTrue(AiFeature.UpdateSummary.userPrompt(prepared).startsWith("New updates:\n"))
        assertNull(AiFeature.UpdateSummary.nanoTask)
        assertTrue("episodes" in AiFeature.UpdateSummary.system)
    }

    @Test
    fun `search keywords finish cleans and caps`() {
        assertEquals(
            "slime\nreincarnated\nisekai\nmonster\nnation\ngoblin",
            AiFeature.SearchKeywords.finish("1. Slime\n- \"reincarnated\"\n* Isekai\nslime\n\nmonster\nnation\ngoblin\nextra"),
        )
        assertEquals(300, AiFeature.SearchKeywords.prepare("a".repeat(500)).length)
        assertTrue(AiFeature.SearchKeywords.userPrompt("x").startsWith("Description: "))
    }

    @Test
    fun `search keywords finish handles comma and preamble replies`() {
        assertEquals("slime\nisekai\nreincarnation", AiFeature.SearchKeywords.finish("slime, isekai, reincarnation"))
        assertEquals("slime\nisekai", AiFeature.SearchKeywords.finish("Here are the keywords:\nslime\nisekai"))
        assertEquals("slime\nisekai", AiFeature.SearchKeywords.finish("Keywords: slime; isekai"))
        assertEquals("", AiFeature.SearchKeywords.finish("***\n—\n\"\""))
    }

    @Test
    fun explainExceptionPrepareTrims() {
        val p = AiFeature.ExplainException.prepare(ErrorDetails("Comick", "x".repeat(2000)))
        assertTrue(p.startsWith("Source: Comick\nError:\n"))
        assertEquals(1500, p.substringAfter("Error:\n").length)
    }

    @Test
    fun digestTakesTwenty() {
        val p = AiFeature.ExceptionDigest.prepare(List(30) { "line $it" })
        assertEquals(20, p.lines().size)
        assertEquals("- line 0", p.lines().first())
    }

    @Test
    fun exceptionFeaturesUseErrorToggle() {
        assertEquals(AiToggle.ErrorExplanations, AiFeature.ExplainException.toggle)
        assertEquals(AiToggle.ErrorExplanations, AiFeature.ExceptionDigest.toggle)
    }

    @Test
    fun exceptionTelemetryNames() {
        assertEquals("explain_exception", AiFeature.ExplainException.telemetryName)
        assertEquals("exception_digest", AiFeature.ExceptionDigest.telemetryName)
    }
}
