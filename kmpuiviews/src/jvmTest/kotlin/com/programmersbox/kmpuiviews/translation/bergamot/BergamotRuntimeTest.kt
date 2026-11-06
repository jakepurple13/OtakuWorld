package com.programmersbox.kmpuiviews.translation.bergamot

import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BergamotRuntimeTest {
    @Test
    fun `platform keys`() {
        assertEquals("macos-arm64", bergamotPlatformKey("Mac OS X", "aarch64"))
        assertEquals("macos-x64", bergamotPlatformKey("Mac OS X", "x86_64"))
        assertEquals("windows-x64", bergamotPlatformKey("Windows 11", "amd64"))
        assertEquals("linux-x64", bergamotPlatformKey("Linux", "amd64"))
        assertNull(bergamotPlatformKey("Windows 11", "aarch64"))
        assertNull(bergamotPlatformKey("Linux", "aarch64"))
        assertNull(bergamotPlatformKey("FreeBSD", "amd64"))
        assertNull(bergamotPlatformKey("Linux", "riscv64"))
    }

    @Test
    fun `pinned builds cover every platform except windows`() {
        assertEquals(setOf("macos-arm64", "macos-x64", "linux-x64"), pinnedBergamotCliAssets.keys)
        pinnedBergamotCliAssets.forEach { (key, asset) ->
            assertTrue(asset.url.endsWith("/bergamot-cli-0.1.1/translator-cli-$key"), asset.url)
            assertTrue(Regex("[0-9a-f]{64}").matches(asset.sha256), key)
        }
    }

    @Test
    fun `unsupported platform is reported, not offered for install`() = runBlocking {
        val root = Files.createTempDirectory("bergamot-unsupported").toFile()
        try {
            val runtime = BergamotCliRuntime(installRoot = root, cliOverride = null, platformKey = "windows-x64", asset = null)
            assertFalse(runtime.isSupported)
            assertEquals("Bergamot isn't available for Windows yet. Use Ollama or Custom instead.", runtime.unavailableReason())

            val mac = BergamotCliRuntime(installRoot = root, cliOverride = null, platformKey = "macos-arm64")
            assertTrue(mac.isSupported)
            assertEquals("Install the translator (7.7 MB)", mac.unavailableReason())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `config lists the model files and Firefox options`() {
        val vocab = File("/m/vocab.jaen.spm")
        val yaml = bergamotConfigYaml(
            BergamotModelFiles(File("/m/model.bin"), File("/m/lex.bin"), vocab, vocab)
        ).lines()
        assertTrue("models: [\"${File("/m/model.bin").absolutePath}\"]" in yaml)
        assertTrue("vocabs: [\"${vocab.absolutePath}\", \"${vocab.absolutePath}\"]" in yaml)
        assertTrue("shortlist: [\"${File("/m/lex.bin").absolutePath}\", false]" in yaml)
        assertTrue("gemm-precision: int8shiftAlphaAll" in yaml)
        assertTrue("beam-size: 1" in yaml)
    }

    @Test
    fun `config without lex has no shortlist and escapes quotes`() {
        val odd = File("/m/a \"b\".bin")
        val yaml = bergamotConfigYaml(BergamotModelFiles(odd, null, odd, odd))
        assertFalse("shortlist" in yaml)
        assertTrue("models: [\"${odd.absolutePath.replace("\"", "\\\"")}\"]" in yaml.lines())
    }

    @Test
    fun `adds the missing space between glued sentences`() {
        assertEquals("He pulled out his sword quietly. Then she laughed.", spaceBetweenSentences("He pulled out his sword quietly.Then she laughed."))
        assertEquals("Run! Now? Yes.", spaceBetweenSentences("Run!Now?Yes."))
        assertEquals("He said “go.” Then left.", spaceBetweenSentences("He said “go.”Then left."))
        assertEquals("It was 3. “Fine,” she said.", spaceBetweenSentences("It was 3.“Fine,” she said."))
        assertEquals("Done. (Then more.)", spaceBetweenSentences("Done.(Then more.)"))
        assertEquals("Wait. I know.", spaceBetweenSentences("Wait.I know."))
    }

    @Test
    fun `leaves numbers abbreviations and spaced text alone`() {
        listOf(
            "3.14",
            "e.g. this",
            "U.S.A. is big.",
            "Version 1.2.3 ships.",
            "See example.com today.",
            "Already spaced. Fine.",
            "He said \"go.\" Then left.",
            "\"Go.\" she said",
            // A straight quote can open or close, so glued text next to one is left as is.
            "quietly.\"Then she laughed.\"",
            "Built on Node.JS today.",
            "Visit example.COM now.",
            "",
        ).forEach { assertEquals(it, spaceBetweenSentences(it)) }
    }
}
