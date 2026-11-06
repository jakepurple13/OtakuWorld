package com.programmersbox.kmpuiviews.translation.bergamot

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Runs a real ja→en translation. Opt-in (JUnit 4, so an assumption instead of `@EnabledIfEnvironmentVariable`):
 *
 * `BERGAMOT_SMOKE=1 BERGAMOT_CLI=/path/to/translator-cli BERGAMOT_JAEN_DIR=/dir/with/jaen/files ./gradlew :kmpuiviews:jvmTest --tests '*BergamotCliRuntimeSmokeTest*'`
 *
 * The dir holds Firefox's ja→en files: `model.jaen.intgemm.alphas.bin`, `lex.50.50.jaen.s2t.bin`, `vocab.jaen.spm`.
 */
class BergamotCliRuntimeSmokeTest {
    @Test
    fun `translates one japanese sentence`() = runBlocking {
        assumeTrue("set BERGAMOT_SMOKE=1 to run", System.getenv("BERGAMOT_SMOKE") == "1")
        val dir = File(requireNotNull(System.getenv("BERGAMOT_JAEN_DIR")) { "BERGAMOT_JAEN_DIR not set" })
        val vocab = File(dir, "vocab.jaen.spm")
        val model = BergamotModelFiles(
            model = File(dir, "model.jaen.intgemm.alphas.bin"),
            lex = File(dir, "lex.50.50.jaen.s2t.bin"),
            srcVocab = vocab,
            trgVocab = vocab,
        )
        val runtime = BergamotCliRuntime(installRoot = Files.createTempDirectory("bergamot").toFile())
        assertTrue(runtime.unavailableReason() == null, "translator-cli not found; set BERGAMOT_CLI")

        val start = System.nanoTime()
        val english = runtime.translate("彼は静かに剣を抜いた。", model)
        println("bergamot ja→en: \"$english\" in ${(System.nanoTime() - start) / 1_000_000} ms")

        assertTrue(english.isNotBlank())
        assertTrue(english.any { it in 'a'..'z' || it in 'A'..'Z' }, "not English: $english")
    }
}
