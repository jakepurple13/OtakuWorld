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
        val engine = FakeEngine(EngineKind.Ollama)
        assertEquals("hello", handler("en", engine).translate("hello"))
        assertTrue(engine.calls.isEmpty())
    }

    @Test
    fun `undetected returns input`() = runBlocking {
        val engine = FakeEngine(EngineKind.Ollama)
        assertEquals("???", handler(null, engine).translate("???"))
        assertTrue(engine.calls.isEmpty())
    }

    @Test
    fun `routes to selected engine`() = runBlocking {
        settings.engine.set("Custom")
        val ollama = FakeEngine(EngineKind.Ollama)
        val custom = FakeEngine(EngineKind.Custom)
        assertEquals("T:konnichiwa", handler("ja", ollama, custom).translate("konnichiwa"))
        assertEquals(listOf(Triple("konnichiwa", "ja", "en")), custom.calls)
        assertTrue(ollama.calls.isEmpty())
    }

    @Test
    fun `stored Bergamot engine falls back to Ollama`() = runBlocking {
        settings.engine.set("Bergamot") // the engine removed from the app; old installs may still have it stored
        val ollama = FakeEngine(EngineKind.Ollama)
        val custom = FakeEngine(EngineKind.Custom)
        assertEquals("T:konnichiwa", handler("ja", ollama, custom).translate("konnichiwa"))
        assertEquals(1, ollama.calls.size)
        assertTrue(custom.calls.isEmpty())
    }

    @Test
    fun `unavailable returns input and shows one error banner with the message`() = runBlocking {
        notifier.dismissAll()
        val engine = FakeEngine(EngineKind.Ollama) { throw TranslationUnavailable("No model") }
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
    fun `generic engine exception returns input with banner`() = runBlocking {
        notifier.dismissAll()
        val engine = FakeEngine(EngineKind.Ollama) { throw IllegalStateException("bad json") }
        assertEquals("hola", handler("es", engine).translate("hola"))
        assertEquals("bad json", notifier.banners.value.single().subtitle)
    }

    @Test
    fun `cancellation propagates`() = runBlocking {
        notifier.dismissAll()
        val engine = FakeEngine(EngineKind.Ollama) { throw kotlin.coroutines.cancellation.CancellationException("c") }
        var thrown: Throwable? = null
        try {
            handler("es", engine).translate("hola")
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            thrown = e
        }
        assertTrue(thrown != null)
        assertTrue(notifier.banners.value.isEmpty())
    }

    @Test
    fun `clear cancels in-flight translateDescription`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val done = CompletableDeferred<Unit>()
        val engine = object : TranslationEngine {
            override val kind = EngineKind.Ollama
            override suspend fun targets() = listOf("en")
            override suspend fun translate(text: String, source: String, target: String): String {
                started.complete(Unit)
                kotlinx.coroutines.awaitCancellation()
            }
        }
        var out: String? = null
        val progress = mutableListOf<Boolean>()
        val h = handler("es", engine)
        h.translateDescription(
            "hola",
            progress = {
                progress += it
                if (!it) done.complete(Unit)
            },
            translatedText = { out = it },
        )
        withTimeout(5_000) { started.await() }
        h.clear()
        withTimeout(5_000) { done.await() }
        assertEquals(false, progress.last())
        assertEquals(null, out)
    }

    @Test
    fun `translateDescription always ends progress false`()= runBlocking {
        val engine = FakeEngine(EngineKind.Ollama) { throw TranslationUnavailable("boom") }
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
