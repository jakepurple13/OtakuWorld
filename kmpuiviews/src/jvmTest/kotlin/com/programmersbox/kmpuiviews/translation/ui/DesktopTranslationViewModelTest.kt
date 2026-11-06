package com.programmersbox.kmpuiviews.translation.ui

import androidx.lifecycle.ViewModelStore
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.kmpuiviews.domain.ModelState
import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
import com.programmersbox.kmpuiviews.testing.createTestNewSettingsHandling
import com.programmersbox.kmpuiviews.translation.DesktopTranslationSettings
import com.programmersbox.kmpuiviews.translation.EngineKind
import com.programmersbox.kmpuiviews.translation.LanguageDetector
import com.programmersbox.kmpuiviews.translation.TranslationEngine
import com.programmersbox.kmpuiviews.translation.bergamot.BergamotModelStore
import com.programmersbox.kmpuiviews.translation.bergamot.FakeRuntime
import com.programmersbox.kmpuiviews.translation.bergamot.FakeServer
import com.programmersbox.kmpuiviews.translation.bergamot.fakePair
import com.programmersbox.kmpuiviews.translation.custom.OpenAiCompatClient
import com.programmersbox.kmpuiviews.translation.ollama.OllamaClient
import com.programmersbox.sharedcomponents.notifier.DefaultInAppNotifier
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import java.util.Collections
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

class DesktopTranslationViewModelTest {

    private class FakeEngine(
        override val kind: EngineKind,
        var targetList: List<String>,
        private val onTranslate: (String) -> String = { "T:$it" },
    ) : TranslationEngine {
        val calls = mutableListOf<Triple<String, String, String>>()
        override suspend fun targets(): List<String> = targetList
        override suspend fun translate(text: String, source: String, target: String): String {
            calls += Triple(text, source, target)
            return onTranslate(text)
        }
    }

    private val viewModelStore = ViewModelStore()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dir = Files.createTempDirectory("desktop-translation-vm").toFile()
    private val server = FakeServer(fakePair("ja", "en", 10) + fakePair("ko", "en", 20))
    private val notifier = DefaultInAppNotifier(scope)
    private val appSettings = createTestNewSettingsHandling()
    private lateinit var settings: DesktopTranslationSettings

    /** Ollama models the fake server lists; `/api/delete` removes from it. */
    private val ollamaModels = Collections.synchronizedList(mutableListOf("gemma3:4b", "qwen2.5:7b", "llama3.2:3b"))
    private val ollamaDeletes = Collections.synchronizedList(mutableListOf<String>())
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private val ollamaHttp = HttpClient(MockEngine { req ->
        when (req.url.encodedPath) {
            "/api/version" -> respond("""{"version":"0.12.6"}""", headers = json)
            "/api/tags" -> respond(
                ollamaModels.toList().joinToString(",", """{"models":[""", "]}") { """{"name":"$it","size":1000}""" },
                headers = json,
            )

            "/api/delete" -> {
                val name = Regex(""""model":"([^"]+)"""").find(String(req.body.toByteArray()))!!.groupValues[1]
                ollamaDeletes += name
                ollamaModels -= name
                respond("", HttpStatusCode.OK)
            }

            else -> respond("", HttpStatusCode.NotFound)
        }
    })

    private val bergamot = FakeEngine(EngineKind.Bergamot, listOf("en"))
    private val ollama = FakeEngine(EngineKind.Ollama, listOf("en", "ja", "ko"))
    private val custom = FakeEngine(EngineKind.Custom, listOf("en", "fr"))
    private val timeSource = TestTimeSource()

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        createTestDataStoreHandling()
        settings = DesktopTranslationSettings(AppDirs { appName = "test"; appAuthor = "test" })
        runBlocking {
            settings.engine.set(EngineKind.Bergamot.name)
            settings.ollamaModel.set("qwen2.5:7b")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        scope.cancel()
        server.close()
        dir.deleteRecursively()
        Thread.sleep(50)
        Dispatchers.resetMain()
    }

    private val store = BergamotModelStore(server.index(), dir, server.client, scope)

    private fun viewModel(detected: String? = "ja", vararg overrides: FakeEngine) = DesktopTranslationViewModel(
        settings = settings,
        appSettings = appSettings,
        engines = (listOf(bergamot, ollama, custom) + overrides).associateBy { it.kind },
        store = store,
        runtime = FakeRuntime(),
        ollama = OllamaClient(ollamaHttp, settings),
        server = OpenAiCompatClient(HttpClient(MockEngine { respond("", HttpStatusCode.NotFound) }), settings),
        detector = object : LanguageDetector {
            override fun detect(text: String): String? = detected
        },
        notifier = notifier,
        timeSource = timeSource,
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    private suspend fun DesktopTranslationViewModel.awaitState(
        condition: (DesktopTranslationState) -> Boolean,
    ): DesktopTranslationState = withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(10_000) {
            while (!condition(state.value)) delay(10)
            state.value
        }
    }

    @Test
    fun `switching engine changes storage label`() = runBlocking<Unit> {
        val vm = viewModel()
        vm.awaitState { it.engine == EngineKind.Bergamot && it.storageLabel == "0 language pairs" }

        vm.setEngine(EngineKind.Ollama)
        val ollamaState = vm.awaitState { it.engine == EngineKind.Ollama && it.storageBytes == 3000L }
        assertEquals("Ollama models (managed by Ollama)", ollamaState.storageLabel)
        assertEquals(EngineKind.Ollama.name, settings.engine.get())

        vm.setEngine(EngineKind.Custom)
        val customState = vm.awaitState { it.engine == EngineKind.Custom }
        assertEquals("Your files aren't counted", customState.storageLabel)
        assertEquals(null, customState.storageBytes)

        store.refresh()
        vm.setEngine(EngineKind.Bergamot)
        store.download("ja")
        vm.awaitState { s -> s.bergamotRows.any { it.source == "ja" && it.state == ModelState.Downloaded } }
        val bergamotState = vm.awaitState { it.storageLabel == "1 language pair" }
        assertEquals(30L, bergamotState.storageBytes)
    }

    @Test
    fun `target list follows engine targets`() = runBlocking<Unit> {
        val vm = viewModel()
        vm.setEngine(EngineKind.Ollama)
        vm.awaitState { it.engine == EngineKind.Ollama && it.targets.toSet() == setOf("en", "ja", "ko") }

        vm.setEngine(EngineKind.Custom)
        vm.awaitState { it.engine == EngineKind.Custom && it.targets.toSet() == setOf("en", "fr") }

        vm.setTarget("fr")
        vm.awaitState { it.target == "fr" }
        withTimeout(10_000) { while (appSettings.translationTargetLanguage.get() != "fr") delay(10) }
    }

    @Test
    fun `delete ollama model in use selects next`() = runBlocking<Unit> {
        val vm = viewModel()
        vm.setEngine(EngineKind.Ollama)
        vm.awaitState { it.ollamaModels.size == 3 && it.ollamaModel == "qwen2.5:7b" }

        vm.askDelete(ollamaDeleteId("qwen2.5:7b"))
        vm.confirmDelete()

        val s = vm.awaitState { it.ollamaModels.size == 2 }
        assertEquals(listOf("qwen2.5:7b"), ollamaDeletes.toList())
        assertEquals("llama3.2:3b", s.ollamaModel)
        assertEquals("llama3.2:3b", settings.ollamaModel.get())
        assertEquals(null, s.confirmingId)
    }

    @Test
    fun `runTest records timing and engine label`() = runBlocking<Unit> {
        val slow = FakeEngine(EngineKind.Bergamot, listOf("en")) {
            timeSource += 1_234.milliseconds
            "He drew his sword."
        }
        val vm = viewModel("ja", slow)
        vm.setTestInput("彼は剣を抜いた。")
        vm.runTest()

        val s = vm.awaitState { !it.testRunning && it.testOutput != null }
        assertEquals("He drew his sword.", s.testOutput)
        assertEquals("Bergamot · ja→en · 1.2 s", s.testTiming)
        assertEquals("ja", s.detectedSource)
        assertEquals(listOf(Triple("彼は剣を抜いた。", "ja", "en")), slow.calls)

        val fast = FakeEngine(EngineKind.Custom, listOf("en", "fr")) {
            timeSource += 84.milliseconds
            "Il a tiré son épée."
        }
        val vm2 = viewModel("ja", fast)
        vm2.setEngine(EngineKind.Custom)
        vm2.setTarget("fr")
        vm2.awaitState { it.target == "fr" && it.engine == EngineKind.Custom }
        vm2.runTest()
        val s2 = vm2.awaitState { !it.testRunning && it.testOutput == "Il a tiré son épée." }
        assertEquals("Custom · ja→fr · 84 ms", s2.testTiming)
    }

    @Test
    fun `confirm needed before delete`() = runBlocking<Unit> {
        store.refresh()
        val vm = viewModel()
        store.download("ja")
        vm.awaitState { s -> s.bergamotRows.any { it.source == "ja" && it.state == ModelState.Downloaded } }

        vm.askDelete(bergamotDeleteId("ja"))
        val asked = vm.awaitState { it.confirmingId == bergamotDeleteId("ja") }
        delay(200)
        assertTrue(asked.bergamotRows.single { it.source == "ja" }.state == ModelState.Downloaded)
        assertEquals(ModelState.Downloaded, store.rows.value.single { it.source == "ja" }.state)
        assertTrue(dir.resolve("ja-en").exists())

        vm.cancelDelete()
        vm.awaitState { it.confirmingId == null }
        assertTrue(dir.resolve("ja-en").exists())

        vm.askDelete(bergamotDeleteId("ja"))
        vm.confirmDelete()
        vm.awaitState { s -> s.bergamotRows.single { it.source == "ja" }.state == ModelState.NotDownloaded }
        assertTrue(!dir.resolve("ja-en").exists())
    }
}
