package com.programmersbox.kmpuiviews.translation.bergamot

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Runs a real ja→en translation. Opt-in (JUnit 4, so an assumption instead of `@EnabledIfEnvironmentVariable`):
 *
 * `BERGAMOT_SMOKE=1 BERGAMOT_CLI=/path/to/translator-cli BERGAMOT_JAEN_DIR=/dir/with/jaen/files ./gradlew --no-daemon :kmpuiviews:jvmTest --tests '*BergamotCliRuntimeSmokeTest*'`
 *
 * The second test downloads the pinned build instead: set `BERGAMOT_INSTALL_SMOKE=1` (and `BERGAMOT_JAEN_DIR`).
 * Use `--no-daemon`: a reused Gradle daemon may not pass these variables to the test JVM.
 *
 * The dir holds Firefox's ja→en files: `model.jaen.intgemm.alphas.bin`, `lex.50.50.jaen.s2t.bin`, `vocab.jaen.spm`.
 */
class BergamotCliRuntimeSmokeTest {
    private fun jaEnModel(): BergamotModelFiles {
        val dir = File(requireNotNull(System.getenv("BERGAMOT_JAEN_DIR")) { "BERGAMOT_JAEN_DIR not set" })
        val vocab = File(dir, "vocab.jaen.spm")
        return BergamotModelFiles(
            model = File(dir, "model.jaen.intgemm.alphas.bin"),
            lex = File(dir, "lex.50.50.jaen.s2t.bin"),
            srcVocab = vocab,
            trgVocab = vocab,
        )
    }

    private suspend fun assertTranslatesJapanese(runtime: BergamotRuntime) {
        val start = System.nanoTime()
        val english = runtime.translate("彼は静かに剣を抜いた。", jaEnModel())
        println("bergamot ja→en: \"$english\" in ${(System.nanoTime() - start) / 1_000_000} ms")

        assertTrue(english.isNotBlank())
        assertTrue(english.any { it in 'a'..'z' || it in 'A'..'Z' }, "not English: $english")
    }

    @Test
    fun `translates one japanese sentence`() = runBlocking {
        assumeTrue("set BERGAMOT_SMOKE=1 to run", System.getenv("BERGAMOT_SMOKE") == "1")
        val runtime = BergamotCliRuntime(installRoot = Files.createTempDirectory("bergamot").toFile())
        assertTrue(runtime.unavailableReason() == null, "translator-cli not found; set BERGAMOT_CLI")
        assertTranslatesJapanese(runtime)
    }

    /** Downloads this platform's pinned build from the GitHub release, checks its sha256, and translates with it. */
    @Test
    fun `installs the pinned build and translates`() = runBlocking {
        assumeTrue("set BERGAMOT_INSTALL_SMOKE=1 to run", System.getenv("BERGAMOT_INSTALL_SMOKE") == "1")
        val root = Files.createTempDirectory("bergamot-install").toFile()
        try {
            val runtime = BergamotCliRuntime(installRoot = root, cliOverride = null)
            assertTrue(runtime.isSupported, "no pinned build for ${bergamotPlatformKey()}")
            println("before install: ${runtime.unavailableReason()}")

            var last = 0f
            val start = System.nanoTime()
            runtime.install { last = it }
            val exe = File(root, "translator-cli")
            println("installed ${exe.length()} bytes in ${(System.nanoTime() - start) / 1_000_000} ms, executable=${exe.canExecute()}")

            assertEquals(1f, last)
            assertTrue(exe.canExecute())
            assertNull(runtime.unavailableReason())
            assertTranslatesJapanese(runtime)
        } finally {
            root.deleteRecursively()
        }
    }
}
