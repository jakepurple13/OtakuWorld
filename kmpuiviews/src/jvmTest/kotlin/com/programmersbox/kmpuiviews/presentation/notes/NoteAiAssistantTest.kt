package com.programmersbox.kmpuiviews.presentation.notes

import com.programmersbox.kmpuiviews.domain.aitext.AiEngineInfo
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.kmpuiviews.domain.aitext.AiTextPrompts
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.aitext.DiffSpan
import com.programmersbox.kmpuiviews.domain.aitext.RewriteStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class NoteAiAssistantTest {
    private class Fake(
        var nanoOnly: Boolean = false,
        var proofreadOutput: () -> Flow<String> = { flowOf("Stopped at ch 45") },
        var rewriteOutput: () -> Flow<String> = { flowOf("Rewritten") },
    ) : AiText {
        override suspend fun engineFor(feature: AiFeature, inputLength: Int): AiEngineInfo? =
            if (nanoOnly && inputLength > AiTextPrompts.NANO_TEXT_LIMIT) null else ENGINE

        override fun proofread(text: String) = proofreadOutput()
        override fun rewrite(text: String, style: RewriteStyle) = rewriteOutput()
        override fun recap(previousChapterText: String): Flow<String> = flowOf()
        override fun tldr(description: String): Flow<String> = flowOf()
        override fun listNames(titles: List<String>): Flow<String> = flowOf()
        override fun listDescription(titles: List<String>): Flow<String> = flowOf()
    }

    private fun TestScope.assistant(fake: Fake = Fake()) =
        NoteAiAssistant(fake, TestScope(StandardTestDispatcher(testScheduler)))

    @Test
    fun `blank note disables tools`() = runTest {
        val ai = assistant()
        ai.onTextChanged("   ")
        advanceUntilIdle()
        assertEquals(false, ai.toolsEnabled.value)
        assertNull(ai.disabledReason.value)
    }

    @Test
    fun `note with text enables tools`() = runTest {
        val ai = assistant()
        ai.onTextChanged("hello")
        advanceUntilIdle()
        assertEquals(true, ai.toolsEnabled.value)
    }

    @Test
    fun `long note with only nano disables with reason`() = runTest {
        val ai = assistant(Fake(nanoOnly = true))
        ai.onTextChanged("a".repeat(AiTextPrompts.NANO_TEXT_LIMIT + 1))
        advanceUntilIdle()
        assertEquals(false, ai.toolsEnabled.value)
        assertEquals("Note is too long for on-device tools", ai.disabledReason.value)
    }

    @Test
    fun `fixGrammar preview has diff and accept returns corrected text`() = runTest {
        val ai = assistant()
        ai.fixGrammar("Stoped at ch 45")
        advanceUntilIdle()
        val preview = assertNotNull(ai.preview.value)
        assertIs<AiTextState.Done>(preview.state)
        assertEquals("Stoped at ch 45", preview.original)
        assertEquals(
            listOf(DiffSpan.Deleted("Stoped"), DiffSpan.Inserted("Stopped"), DiffSpan.Same(" at ch 45")),
            preview.diff,
        )
        assertEquals("Stopped at ch 45", ai.accept())
        advanceUntilIdle()
        assertNull(ai.preview.value)
    }

    @Test
    fun `rewrite preview has no diff`() = runTest {
        val ai = assistant()
        ai.rewrite("some note", RewriteStyle.Shorten)
        advanceUntilIdle()
        val preview = assertNotNull(ai.preview.value)
        assertIs<AiTextState.Done>(preview.state)
        assertNull(preview.diff)
        assertEquals("Rewritten", ai.accept())
    }

    @Test
    fun `discard returns null preview and accept after discard returns null`() = runTest {
        val ai = assistant()
        ai.fixGrammar("Stoped at ch 45")
        advanceUntilIdle()
        ai.discard()
        advanceUntilIdle()
        assertNull(ai.preview.value)
        assertNull(ai.accept())
    }

    @Test
    fun `fixGrammar error shows Error state and leaves text unchanged`() = runTest {
        val ai = assistant(Fake(proofreadOutput = { flow { throw IllegalStateException("boom") } }))
        ai.fixGrammar("Stoped")
        advanceUntilIdle()
        val preview = assertNotNull(ai.preview.value)
        assertIs<AiTextState.Error>(preview.state)
        assertEquals("Stoped", preview.original)
        assertNull(ai.accept())
        assertNotNull(ai.preview.value)
    }

    private companion object {
        val ENGINE = AiEngineInfo("Fake", onDevice = true, destination = null)
    }
}
