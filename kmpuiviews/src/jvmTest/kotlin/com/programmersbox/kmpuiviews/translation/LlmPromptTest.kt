package com.programmersbox.kmpuiviews.translation

import kotlin.test.Test
import kotlin.test.assertEquals

class LlmPromptTest {
    @Test
    fun `system prompt is exact`() {
        assertEquals(
            "You are a translation engine. Translate the user's text from Japanese to English. Reply with only the translation, no notes, no quotes.",
            translationSystemPrompt("Japanese", "English")
        )
    }

    @Test
    fun `clean trims and strips one pair of quotes`() {
        assertEquals("Hello", cleanLlmReply("  \"Hello\"\n"))
        assertEquals("\"Hi\"", cleanLlmReply("\"\"Hi\"\""))
        assertEquals("Hello", cleanLlmReply("Hello"))
        assertEquals("\"", cleanLlmReply("\""))
    }
}
