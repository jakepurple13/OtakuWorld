package com.programmersbox.kmpuiviews.translation

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
import com.programmersbox.kmpuiviews.testing.createTestNewSettingsHandling
import com.programmersbox.sharedcomponents.notifier.DefaultInAppNotifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopTranslationHandlerTest {

    private class FakeEngine(
        override val kind: EngineKind,
        private val result: (String) -> String = { "T:$it" },
    ) : TranslationEngine {
        val calls = mutableListOf<Triple<String, String, String>>()
        override suspend fun targets(): List<String> = listOf("en")
        override suspend fun translate(text: String, source: String, target: String): String {
            calls += Triple(text, source, target)
            return result(text)
        }
    }

    private class FakeDetector(private val code: String?) : LanguageDetector {
        override fun detect(text: String): String? = code
    }

    private val appSettings = createTestNewSettingsHandling()
    private val notifier = DefaultInAppNotifier(CoroutineScope(SupervisorJob() + Dispatchers.Default))
    private lateinit var settings: DesktopTranslationSettings

    @BeforeTest
    fun setUp() {
        createTestDataStoreHandling()
        settings = DesktopTranslationSettings(
            AppDirs {
                appName = "test"
                appAuthor = "test"
            }
        )
    }

    private fun handler(detected: String?, vararg engines: TranslationEngine) = DesktopTranslationHandler(
        engines = engines.associateBy { it.kind },
        detector = FakeDetector(detected),
        settings = settings,
        appSettings = appSettings,
        notifier = notifier,
    )

    @Test
    fun `same language returns input without engine call`() = runBlocking {
        val engine = FakeEngine(EngineKind.Bergamot)
        assertEquals("hello", handler("en", engine).translate("hello"))
        assertTrue(engine.calls.isEmpty())
    }

    @Test
    fun `undetected returns input`() = runBlocking {
        val engine = FakeEngine(EngineKind.Bergamot)
        assertEquals("???", handler(null, engine).translate("???"))
        assertTrue(engine.calls.isEmpty())
    }

    @Test
    fun `routes to selected engine`() = runBlocking {
        settings.engine.set("Ollama")
        val bergamot = FakeEngine(EngineKind.Bergamot)
        val ollama = FakeEngine(EngineKind.Ollama)
        assertEquals("T:konnichiwa", handler("ja", bergamot, ollama).translate("konnichiwa"))
        assertEquals(listOf(Triple("konnichiwa", "ja", "en")), ollama.calls)
        assertTrue(bergamot.calls.isEmpty())
    }

    @Test
    fun `unavailable returns input and shows one error banner with the message`() = runBlocking {
        notifier.dismissAll()
        val engine = FakeEngine(EngineKind.Bergamot) { throw TranslationUnavailable("No model") }
        assertEquals("hola", handler("es", engine).translate("hola"))
        val banner = notifier.banners.value.single()
        assertEquals("Couldn't translate", banner.title)
        assertEquals("No model", banner.subtitle)
    }

    @Test
    fun `missing engine shows pick engine message`() = runBlocking {
        notifier.dismissAll()
        assertEquals("hola", handler("es").translate("hola"))
        assertEquals(
            "Pick a translation engine in Settings → Translation",
            notifier.banners.value.single().subtitle
        )
    }

    @Test
    fun `translateDescription always ends progress false`() = runBlocking {
        val engine = FakeEngine(EngineKind.Bergamot) { throw TranslationUnavailable("boom") }
        val done = CompletableDeferred<Unit>()
        val progress = mutableListOf<Boolean>()
        var out: String? = null
        handler("es", engine).translateDescription(
            "hola",
            progress = {
                progress += it
                if (!it) done.complete(Unit)
            },
            translatedText = { out = it },
        )
        withTimeout(5_000) { done.await() }
        assertEquals(false, progress.last())
        assertEquals("hola", out)
    }
}
