package com.programmersbox.mihon

import android.app.Application
import com.programmersbox.kmpextensionloader.DexConverter
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Converts the real fixture APK and checks the jar actually links on the JVM. */
class MihonDexConversionTest {
    private val root = Files.createTempDirectory("mihondex").toFile()

    init {
        MihonRuntime.init(Application("mihon-test", File(root, "data")))
    }

    @AfterTest fun cleanup() {
        root.deleteRecursively()
    }

    private fun convertFixture(): File = assertNotNull(DexConverter.convert(MihonFixture.apk, File(root, "jars")))

    private fun classEntries(jar: File): Map<String, ByteArray> = ZipFile(jar).use { zip ->
        zip.entries().toList()
            .filter { it.name.endsWith(".class") }
            .associate { e -> e.name.removeSuffix(".class").replace('/', '.') to zip.getInputStream(e).use { it.readBytes() } }
    }

    @Test fun kotlinMangledNamesKeepTheirDash() {
        val texts = classEntries(convertFixture()).values.map { String(it, Charsets.ISO_8859_1) }
        assertTrue(texts.any { "maxAge-LRDsOJo" in it }, "expected a reference to okhttp's maxAge-LRDsOJo")
        assertFalse(texts.any { "maxAge_LRDsOJo" in it }, "dex2jar sanitized '-' to '_'")
    }

    @Test fun everyConvertedClassVerifiesAndInitializes() {
        val jar = convertFixture()
        ChildFirstClassLoader(arrayOf(jar.toURI().toURL()), javaClass.classLoader).use { loader ->
            val failures = classEntries(jar).keys.mapNotNull { name ->
                runCatching { Class.forName(name, true, loader) }.exceptionOrNull()?.let { "$name: $it" }
            }
            assertEquals(emptyList(), failures)
        }
    }

    @Test fun cacheKeyCarriesConverterVersion() {
        assertTrue(DexConverter.jarFor(MihonFixture.apk, root).name.endsWith("-v2.jar"))
    }
}
