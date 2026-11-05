package com.programmersbox.anime.shared.subtitlegen

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.datastore.otakuDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import okio.Path.Companion.toOkioPath
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WhisperModelStoreTest {
    private val root = createTempDirectory("model-store-test").toFile()
    private val modelsDir = File(root, "models")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val server = MockWebServer().apply { start() }
    private val previousStore = runCatching { otakuDataStore }.getOrNull()
    private val settings: AnimeDesktopSettings

    init {
        // A fresh DataStore per test; the previous global one is restored in cleanup().
        otakuDataStore = PreferenceDataStoreFactory.createWithPath(
            scope = scope,
            produceFile = { File(root, "settings.preferences_pb").toOkioPath() },
        )
        settings = AnimeDesktopSettings(AppDirs { appName = "ModelStoreTest"; appAuthor = "test" })
    }

    private fun newStore() = WhisperModelStore(settings, modelsDir, OkHttpClient(), scope, server.url("/").toString())

    private val store by lazy { newStore() }

    @AfterTest
    fun cleanup() {
        scope.cancel()
        server.close()
        if (previousStore != null) otakuDataStore = previousStore
        root.deleteRecursively()
    }

    private fun serve(size: Int) = server.enqueue(MockResponse.Builder().body(Buffer().write(ByteArray(size))).build())

    private fun userModel(name: String, header: String): File =
        File(root, name).apply { writeBytes(header.encodeToByteArray() + ByteArray(2_000)) }

    private fun waitFor(block: () -> Boolean) = runBlocking {
        withTimeout(10_000) { while (!block()) delay(20) }
    }

    @Test
    fun defaultSelectionIsSmall() {
        assertEquals(WhisperModelChoice.BuiltIn(BuiltInWhisperModel.SMALL), store.rows.value.single { it.selected }.choice)
    }

    @Test
    fun downloadWritesPartThenRenames() {
        serve(1_000)
        store.download(BuiltInWhisperModel.TINY)
        waitFor { store.rows.value.first().download == ModelDownload.Ready }

        assertEquals(1_000L, File(modelsDir, "ggml-tiny.bin").length())
        assertFalse(File(modelsDir, "ggml-tiny.bin.part").exists())
        assertEquals("/ggml-tiny.bin", server.takeRequest().url.encodedPath)
        assertEquals(1_000L, store.downloadedBytes.value)
    }

    @Test
    fun stalePartIsNotReady() {
        modelsDir.mkdirs()
        File(modelsDir, "ggml-tiny.bin.part").writeText("stale")

        assertEquals(ModelDownload.Missing, store.rows.value.first().download)

        serve(1_000)
        store.download(BuiltInWhisperModel.TINY)
        waitFor { store.rows.value.first().download == ModelDownload.Ready }
        assertEquals(1_000L, File(modelsDir, "ggml-tiny.bin").length())
        assertFalse(File(modelsDir, "ggml-tiny.bin.part").exists())
    }

    @Test
    fun ensureSelectedDownloadsMissingBuiltIn() = runBlocking {
        serve(1_000)
        store.select(WhisperModelChoice.BuiltIn(BuiltInWhisperModel.BASE))
        var last = 0f
        val file = store.ensureSelected { last = it }

        assertTrue(file.exists())
        assertEquals("ggml-base.bin", file.name)
        assertEquals(1f, last)
    }

    @Test
    fun addCustomRejectsNonModel() = runBlocking {
        val before = store.rows.value
        val result = store.addCustom(userModel("notes.bin", "WEBVTT"))

        assertEquals(AddModelResult.Rejected("notes.bin isn't a whisper.cpp model. Pick a ggml .bin file."), result)
        assertEquals(before, store.rows.value)
    }

    @Test
    fun addCustomAddsAndSelects() = runBlocking {
        val file = userModel("ggml-kotoba-whisper-v2.0.bin", "lmgg")
        assertEquals(AddModelResult.Added, store.addCustom(file))

        val row = store.rows.value.last()
        assertIs<WhisperModelChoice.Custom>(row.choice)
        assertTrue(row.selected)
        assertEquals("kotoba-whisper-v2.0", row.name)
        assertEquals(file.absolutePath, row.path)
        assertEquals(1, store.rows.value.count { it.selected })
        assertTrue(store.summary.value.translatesWell)
        assertEquals("kotoba-whisper-v2.0", store.summary.value.name)

        // Adding the same path again does not duplicate it.
        store.addCustom(file)
        assertEquals(BuiltInWhisperModel.entries.size + 1, store.rows.value.size)
    }

    @Test
    fun removeCustomKeepsFileAndFallsBack() = runBlocking {
        val file = userModel("ggml-mine.bin", "lmgg")
        store.addCustom(file)
        store.removeCustom(file.absolutePath)

        assertTrue(file.exists())
        assertEquals(WhisperModelChoice.BuiltIn(BuiltInWhisperModel.SMALL), store.rows.value.single { it.selected }.choice)
        assertEquals(BuiltInWhisperModel.entries.size, store.rows.value.size)
    }

    @Test
    fun missingCustomFileThrows() = runBlocking {
        val file = userModel("ggml-mine.bin", "lmgg")
        store.addCustom(file)
        file.delete()

        val e = assertFailsWith<ModelMissingException> { store.ensureSelected {} }
        assertEquals("Model file not found", e.message)
    }
}
