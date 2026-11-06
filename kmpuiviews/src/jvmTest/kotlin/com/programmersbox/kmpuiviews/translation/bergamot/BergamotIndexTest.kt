package com.programmersbox.kmpuiviews.translation.bergamot

import com.programmersbox.kmpuiviews.translation.LanguagePair
import com.programmersbox.kmpuiviews.translation.planChain
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BergamotIndexTest {

    private data class P(override val from: String, override val to: String) : LanguagePair

    private val pairs = listOf(P("ja", "en"), P("ko", "en"), P("en", "fr"), P("ja", "ko"))

    @Test
    fun `planChain direct when available`() {
        assertEquals(listOf(P("ja", "ko")), planChain("ja", "ko", pairs))
        assertEquals(listOf(P("ja", "en")), planChain("ja", "en", pairs))
    }

    @Test
    fun `planChain pivots via en`() {
        assertEquals(listOf(P("ja", "en"), P("en", "fr")), planChain("ja", "fr", pairs))
    }

    @Test
    fun `planChain null when no route`() {
        assertNull(planChain("de", "fr", pairs))
        assertNull(planChain("ja", "de", pairs))
        assertNull(planChain("en", "ja", pairs))
    }

    @Test
    fun `planChain en to en is empty list`() {
        assertEquals(emptyList<P>(), planChain("en", "en", pairs))
    }

    private fun fixture(): String =
        requireNotNull(javaClass.getResource("/bergamot/records.json")) { "fixture missing" }.readText()

    @Test
    fun `index keeps latest version per pair`() = runBlocking {
        val server = FakeServer(emptyList(), indexBody = ::fixture)
        val pairs = server.index().pairs().associateBy { "${it.from}-${it.to}" }

        assertEquals(setOf("bn-en", "en-fr", "en-ja", "fr-en", "ja-en", "ko-en"), pairs.keys)
        val jaEn = pairs.getValue("ja-en")
        // v2.1 (model 43,977,787 bytes) beats 2.0 and 2.0a1 (59,504,955).
        assertEquals(43_977_787L, jaEn.files.single { it.type == "model" }.sizeBytes)
        assertEquals(setOf("model", "lex", "vocab"), jaEn.files.map { it.type }.toSet())
        assertTrue(jaEn.files.all { it.pair == "ja-en" })
        assertEquals(31_561_787L, pairs.getValue("en-fr").files.single { it.type == "model" }.sizeBytes)
        assertEquals(
            setOf("model", "lex", "srcvocab", "trgvocab"),
            pairs.getValue("en-ja").files.map { it.type }.toSet()
        )
        val model = jaEn.files.single { it.type == "model" }
        assertTrue(model.url.startsWith(TEST_ATTACHMENT_BASE + "main-workspace/translations-models/"), model.url)
        assertEquals(64, model.sha256.length)
    }

    @Test
    fun `index skips prerelease versions when a release exists`() = runBlocking {
        // Real data: bn-en has 1.0 and a nightly-only 2.0a1 (Firefox release ships 1.0).
        val bnEn = FakeServer(emptyList(), indexBody = ::fixture).index().pairs()
            .single { it.from == "bn" && it.to == "en" }
        assertEquals(3, bnEn.files.size)
        val prerelease = FakeServer(emptyList(), indexBody = {
            recordsJson(listOf(FakeFile("bn", "en", "model", byteArrayOf(1), version = "2.0a1")))
        }).index().pairs()
        assertEquals(1, prerelease.single().files.size) // prerelease used when it's all there is
        val released = FakeServer(emptyList(), indexBody = {
            recordsJson(
                listOf(
                    FakeFile("bn", "en", "model", byteArrayOf(1), version = "1.0"),
                    FakeFile("bn", "en", "model", byteArrayOf(2), version = "2.0a1"),
                )
            )
        }).index().pairs().single()
        assertTrue(released.files.single().url.endsWith("bnen-1.0-model.bin"))
    }

    @Test
    fun `index falls back to the disk cache offline`() = runBlocking {
        val cache = Files.createTempDirectory("bergamot-index").toFile().resolve("index.json")
        val online = FakeServer(emptyList(), indexBody = ::fixture)
        val first = online.index(cache).pairs()

        val offline = FakeServer(emptyList(), indexBody = { error("offline") })
        assertEquals(first, offline.index(cache).pairs())

        cache.delete()
        assertFailsWith<Exception> { offline.index(cache).pairs() }
        Unit
    }
}
