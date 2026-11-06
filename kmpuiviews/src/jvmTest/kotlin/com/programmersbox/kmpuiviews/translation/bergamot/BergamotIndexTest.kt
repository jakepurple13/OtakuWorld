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
    fun `index keeps latest desktop version per pair`() = runBlocking {
        val server = FakeServer(emptyList(), indexBody = ::fixture)
        val pairs = server.index().pairs().associateBy { "${it.from}-${it.to}" }

        assertEquals(setOf("bn-en", "en-fr", "en-ja", "fr-en", "ja-en", "ko-en"), pairs.keys)
        val jaEn = pairs.getValue("ja-en")
        // 2.0 (model 59,504,955 bytes), what desktop Firefox release gets: 2.1 (43,977,787) is Android-only
        // and 2.0a1 is nightly/beta-only.
        assertEquals(59_504_955L, jaEn.files.single { it.type == "model" }.sizeBytes)
        assertEquals(9_346_816L, jaEn.files.single { it.type == "lex" }.sizeBytes)
        assertEquals(59_504_955L, pairs.getValue("ko-en").files.single { it.type == "model" }.sizeBytes)
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
    fun `index skips android-only, channel-gated and too-new records`() = runBlocking {
        suspend fun pick(vararg files: FakeFile) =
            FakeServer(emptyList(), indexBody = { recordsJson(files.toList()) }).index().pairs()

        val picked = pick(
            FakeFile("bn", "en", "model", byteArrayOf(1), version = "1.0"),
            FakeFile("bn", "en", "model", byteArrayOf(2), version = "2.0a1", filter = "env.channel == 'default' || env.channel == 'nightly'"),
            FakeFile("bn", "en", "model", byteArrayOf(3), version = "2.1", filter = "env.appinfo.OS == 'Android' "),
            FakeFile("bn", "en", "model", byteArrayOf(4), version = "3.0"),
        ).single()
        assertTrue(picked.files.single().url.endsWith("bnen-1.0-model.bin"), picked.files.toString())

        // Desktop matches this one, so it is kept.
        val desktop = pick(
            FakeFile("ja", "en", "model", byteArrayOf(1), version = "2.0", filter = "env.appinfo.OS != 'Android' || env.channel != 'release'"),
        ).single()
        assertTrue(desktop.files.single().url.endsWith("jaen-2.0-model.bin"))

        // A pair with nothing desktop can use is left out.
        assertEquals(emptyList<BergamotPair>(), pick(FakeFile("xx", "en", "model", byteArrayOf(1), version = "3.0")))
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
