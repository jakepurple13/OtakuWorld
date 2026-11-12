package com.programmersbox.kmpuiviews.dailyoverview

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.OllamaLLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyFacts
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryPrompt
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import com.programmersbox.koogintegration.AgentMaker
import com.programmersbox.koogintegration.KoogDataStore
import com.programmersbox.koogintegration.PlatformAgents
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KoogEnginesTest {

    private fun dataStore(company: String, model: String = "llama3.2:3b") = KoogDataStore(
        getApiKey = { "" },
        getModelCompany = { company },
        getModelName = { model },
        storeApiKey = {},
        storeModelCompany = {},
        storeModelName = {},
        apiKeyFlow = flowOf(""),
        modelCompanyFlow = flowOf(company),
        modelNameFlow = flowOf(model),
    )

    private fun agentMaker(company: String, model: String = "llama3.2:3b") =
        AgentMaker(dataStore(company, model), PlatformAgents())

    private val requested = mutableListOf<String>()

    private fun tagsClient(models: List<String> = listOf("llama3.2:3b"), handler: suspend () -> Boolean) = HttpClient(MockEngine { request ->
        requested += request.url.toString()
        if (handler()) {
            respond("""{"models":[${models.joinToString(",") { """{"name":"$it","model":"$it"}""" }}]}""")
        } else respondError(HttpStatusCode.InternalServerError)
    })

    private val facts = DailyFacts(
        date = LocalDate(2026, 11, 10),
        unit = UpdateUnit.Episodes,
        readCount = 2,
        titles = emptyList(),
        readsByHour = List(24) { 0 },
        updates = emptyList(),
        failedSources = emptyList(),
    )

    @Test
    fun `ollama ready when company is Ollama and tags responds`() = runTest {
        val engine = OllamaEngine(agentMaker(LLMProvider.Ollama.display), tagsClient { true })

        assertEquals(EngineStatus.Ready, engine.status())
        assertEquals(listOf("http://localhost:11434/api/tags"), requested)
        assertEquals(SummaryEngineId.Ollama, engine.id)
        assertTrue(engine.onDevice)
        assertEquals("Ollama · llama3.2:3b", engine.label())
    }

    @Test
    fun `ollama not running when tags fails`() = runTest {
        val engine = OllamaEngine(agentMaker(LLMProvider.Ollama.display), tagsClient { false })

        assertEquals(EngineStatus.NeedsSetup("Ollama isn't running"), engine.status())
    }

    @Test
    fun `ollama not running when tags throws`() = runTest {
        val engine = OllamaEngine(agentMaker(LLMProvider.Ollama.display), tagsClient { error("connection refused") })

        assertEquals(EngineStatus.NeedsSetup("Ollama isn't running"), engine.status())
    }

    @Test
    fun `ollama not running when tags takes longer than 2s`() = runTest {
        val engine = OllamaEngine(agentMaker(LLMProvider.Ollama.display), tagsClient { delay(3_000); true })

        assertEquals(EngineStatus.NeedsSetup("Ollama isn't running"), engine.status())
    }

    @Test
    fun `ollama needs setup when company is not Ollama`() = runTest {
        val engine = OllamaEngine(agentMaker("Google"), tagsClient { true })

        val status = engine.status()
        assertIs<EngineStatus.NeedsSetup>(status)
        assertEquals("Choose Ollama in AI Helper", status.reason)
        assertTrue(requested.isEmpty())
    }

    @Test
    fun `ollama ready and client built for a model missing from Koog's static list`() = runTest {
        val engine = OllamaEngine(agentMaker(LLMProvider.Ollama.display, "gemma4:26b"), tagsClient(listOf("gemma4:26b", "llama3.2:latest")) { true })

        assertEquals(EngineStatus.Ready, engine.status())
        val (_, model) = assertNotNull(engine.client())
        assertEquals("gemma4:26b", model.id)
        assertEquals(OllamaLLMProvider, model.provider)
    }

    @Test
    fun `ollama needs setup when selected model is not installed`() = runTest {
        val engine = OllamaEngine(agentMaker(LLMProvider.Ollama.display, "gemma4:26b"), tagsClient(listOf("llama3.2:latest")) { true })

        val status = engine.status()
        assertIs<EngineStatus.NeedsSetup>(status)
        assertEquals("Pick an Ollama model in AI Helper", status.reason)
    }

    private class FakeClient(
        private val frames: List<StreamFrame>,
        private val reply: String = "",
    ) : LLMClient() {
        val prompts = mutableListOf<Prompt>()
        var closed = false

        override fun executeStreaming(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): Flow<StreamFrame> {
            prompts += prompt
            return flowOf(*frames.toTypedArray())
        }

        override suspend fun execute(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): Message.Assistant {
            prompts += prompt
            return Message.Assistant(reply, ResponseMetaInfo.Empty)
        }

        override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult = error("unused")
        override fun llmProvider(): LLMProvider = OllamaLLMProvider
        override fun close() {
            closed = true
        }
    }

    private class TestEngine(
        private val fake: FakeClient?,
        override val streams: Boolean = true,
    ) : KoogSummaryEngine() {
        override val id = SummaryEngineId.Ollama
        override val onDevice = true
        override suspend fun label() = "test"
        override suspend fun status() = EngineStatus.Ready
        override suspend fun client() = fake?.let { it to LLModel(OllamaLLMProvider, "m", emptyList()) }
    }

    @Test
    fun `koog engine emits only text frames in order`() = runTest {
        val fake = FakeClient(
            listOf(
                StreamFrame.TextDelta("You read "),
                StreamFrame.ReasoningDelta(text = "thinking"),
                StreamFrame.TextDelta("2 episodes."),
                StreamFrame.ToolCallDelta(id = null, name = "x", content = "{}"),
                StreamFrame.TextComplete("You read 2 episodes."),
                StreamFrame.End(),
            )
        )

        assertEquals(listOf("You read ", "2 episodes."), TestEngine(fake).summarize(facts).toList())
        assertTrue(fake.closed)
    }

    @Test
    fun `koog engine sends the summary prompt`() = runTest {
        val fake = FakeClient(listOf(StreamFrame.TextDelta("ok")))
        TestEngine(fake).summarize(facts).toList()

        val prompt = fake.prompts.single()
        assertEquals("daily-overview", prompt.id)
        val messages = prompt.messages
        assertIs<Message.System>(messages[0])
        assertEquals(SummaryPrompt.system(UpdateUnit.Episodes), messages[0].textContent())
        assertIs<Message.User>(messages[1])
        assertEquals(SummaryPrompt.user(facts), messages[1].textContent())
    }

    @Test
    fun `generate sends system and user text and streams deltas`() = runTest {
        val fake = FakeClient(listOf(StreamFrame.TextDelta("a"), StreamFrame.TextDelta("b")))

        assertEquals(listOf("a", "b"), TestEngine(fake).generate("S", "U").toList())

        val messages = fake.prompts.single().messages
        assertIs<Message.System>(messages[0])
        assertEquals("S", messages[0].textContent())
        assertIs<Message.User>(messages[1])
        assertEquals("U", messages[1].textContent())
    }

    @Test
    fun `koog engine uses complete text when no deltas arrive`() = runTest {
        val fake = FakeClient(listOf(StreamFrame.TextComplete("Whole reply."), StreamFrame.End()))

        assertEquals(listOf("Whole reply."), TestEngine(fake).summarize(facts).toList())
    }

    @Test
    fun `koog engine that does not stream emits the whole reply once`() = runTest {
        val fake = FakeClient(frames = emptyList(), reply = "Whole reply.")

        assertEquals(listOf("Whole reply."), TestEngine(fake, streams = false).summarize(facts).toList())
        assertTrue(fake.closed)
    }

    @Test
    fun `koog engine without a client fails`() = runTest {
        val result = runCatching { TestEngine(null).summarize(facts).toList() }
        assertFalse(result.isSuccess)
    }
}
