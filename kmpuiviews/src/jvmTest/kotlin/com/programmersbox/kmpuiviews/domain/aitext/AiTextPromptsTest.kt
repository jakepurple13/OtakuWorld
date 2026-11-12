package com.programmersbox.kmpuiviews.domain.aitext

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiTextPromptsTest {
    @Test
    fun `recapTail keeps last 3000 chars at word boundary`() {
        val chapter = (1..2_000).joinToString(" ") { "word$it" }
        val tail = AiTextPrompts.recapTail(chapter)
        assertTrue(tail.length <= AiTextPrompts.RECAP_TAIL_CHARS)
        assertTrue(tail.startsWith("word"))
        assertTrue(chapter.endsWith(tail))
        assertTrue(tail.length > AiTextPrompts.RECAP_TAIL_CHARS - 20)
    }

    @Test
    fun `tldrHead keeps first chars at word boundary`() {
        val description = (1..2_000).joinToString(" ") { "word$it" }
        val head = AiTextPrompts.tldrHead(description)
        assertTrue(head.length <= AiTextPrompts.NANO_TEXT_LIMIT)
        assertTrue(description.startsWith(head))
        // Ends on a whole word: the next character in the description is the gap after it
        assertEquals(' ', description[head.length])
        assertTrue(head.length > AiTextPrompts.NANO_TEXT_LIMIT - 20)
    }

    @Test
    fun `tldrHead leaves short descriptions alone`() {
        assertEquals("short text", AiTextPrompts.tldrHead("short text"))
    }

    @Test
    fun `tldrHead without whitespace hard-cuts`() {
        val description = "x".repeat(AiTextPrompts.NANO_TEXT_LIMIT + 50)
        assertEquals(AiTextPrompts.NANO_TEXT_LIMIT, AiTextPrompts.tldrHead(description).length)
    }

    @Test
    fun `parseBullets strips markers and caps at 3`() {
        assertEquals(listOf("a", "b", "c"), AiTextPrompts.parseBullets("- a\n• b\n3. c\n* d"))
    }

    @Test
    fun `parseNames strips markers quotes and dupes`() {
        assertEquals(
            listOf("Cozy", "Slow Evenings"),
            AiTextPrompts.parseNames("1. Cozy\n\"cozy\"\n- Slow Evenings\n\n"),
        )
    }

    @Test
    fun `system prompt per rewrite style differs`() {
        val prompts = RewriteStyle.entries.map { AiTextPrompts.system(AiFeature.Rewrite, it) }
        assertEquals(6, prompts.toSet().size)
        assertTrue(prompts.all { it.isNotBlank() })
    }
}
