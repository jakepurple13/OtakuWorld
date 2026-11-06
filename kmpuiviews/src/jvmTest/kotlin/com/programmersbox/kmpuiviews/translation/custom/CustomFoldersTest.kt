package com.programmersbox.kmpuiviews.translation.custom

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.kmpuiviews.testing.createTestDataStoreHandling
import com.programmersbox.kmpuiviews.translation.DesktopTranslationSettings
import com.programmersbox.kmpuiviews.translation.TranslationUnavailable
import com.programmersbox.kmpuiviews.translation.bergamot.BergamotModelFiles
import com.programmersbox.kmpuiviews.translation.bergamot.BergamotRuntime
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CustomFoldersTest {
    private lateinit var settings: DesktopTranslationSettings

    @BeforeTest
    fun setUp() {
        createTestDataStoreHandling()
        settings = DesktopTranslationSettings(AppDirs { appName = "test"; appAuthor = "test" })
        runBlocking { settings.customFolders.set(emptySet()); settings.customMode.set("folder") }
    }

    private fun folder(pair: String, size: Int = 10): File {
        val dir = Files.createTempDirectory("custom").toFile().apply { deleteOnExit() }
        listOf("model.$pair.intgemm.alphas.bin", "lex.50.50.$pair.s2t.bin", "vocab.$pair.spm")
            .forEach { File(dir, it).writeBytes(ByteArray(size)) }
        return dir
    }

    @Test
    fun `valid folder detects pair and size`() {
        val v = assertIs<FolderCheck.Valid>(inspectFolder(folder("jaen")))
        assertEquals("ja", v.from)
        assertEquals("en", v.to)
        assertEquals(30L, v.sizeBytes)
        assertEquals(v.files.srcVocab, v.files.trgVocab)
        assertTrue(v.files.lex != null)
    }

    @Test
    fun `separate src and trg vocabs are used`() {
        val dir = Files.createTempDirectory("custom").toFile()
        listOf("model.enes.bin", "srcvocab.enes.spm", "trgvocab.enes.spm").forEach { File(dir, it).writeBytes(ByteArray(5)) }
        val v = assertIs<FolderCheck.Valid>(inspectFolder(dir))
        assertEquals("srcvocab.enes.spm", v.files.srcVocab.name)
        assertEquals("trgvocab.enes.spm", v.files.trgVocab.name)
        assertEquals(null, v.files.lex)
        assertEquals(15L, v.sizeBytes)
    }

    @Test
    fun `missing vocab is invalid with reason`() {
        val dir = folder("jaen")
        File(dir, "vocab.jaen.spm").delete()
        val i = assertIs<FolderCheck.Invalid>(inspectFolder(dir))
        assertTrue(i.reason.contains("vocab"), i.reason)
    }

    @Test
    fun `missing model is invalid`() {
        val dir = folder("jaen")
        File(dir, "model.jaen.intgemm.alphas.bin").delete()
        assertTrue(assertIs<FolderCheck.Invalid>(inspectFolder(dir)).reason.contains("model"))
    }

    @Test
    fun `unknown pair name is invalid`() {
        val dir = Files.createTempDirectory("custom").toFile()
        listOf("model.bin", "vocab.spm").forEach { File(dir, it).writeBytes(ByteArray(5)) }
        assertTrue(assertIs<FolderCheck.Invalid>(inspectFolder(dir)).reason.contains("languages"))
    }

    @Test
    fun `not a directory is invalid`() {
        assertIs<FolderCheck.Invalid>(inspectFolder(File("/definitely/not/here")))
    }

    private class Recording : BergamotRuntime {
        override suspend fun unavailableReason(): String? = null
        override suspend fun install(onProgress: (Float) -> Unit) {}
        override suspend fun translate(text: String, model: BergamotModelFiles) =
            "$text|${model.model.name.substringAfter('.').take(4)}"
    }

    private fun engine() = CustomEngine(
        settings, Recording(), OpenAiCompatClient(HttpClient(MockEngine { respond("") }), settings)
    )

    @Test
    fun `folders chain through english`() = runBlocking {
        settings.customFolders.set(setOf(folder("jaen").path, folder("enes").path))
        assertEquals("x|jaen|enes", engine().translate("x", "ja", "es"))
        assertEquals(listOf("en", "es"), engine().targets().sorted())
    }

    @Test
    fun `no route throws unavailable with exact message`() = runBlocking {
        settings.customFolders.set(setOf(folder("jaen").path))
        val e = assertFailsWith<TranslationUnavailable> { engine().translate("x", "ja", "fr") }
        assertEquals("Add a folder for Japanese → French, or Japanese → English and English → French", e.message)
    }

    @Test
    fun `invalid folders are ignored by engine`() = runBlocking {
        val bad = Files.createTempDirectory("custom").toFile()
        settings.customFolders.set(setOf(bad.path))
        assertEquals(emptyList(), engine().targets())
    }
}
