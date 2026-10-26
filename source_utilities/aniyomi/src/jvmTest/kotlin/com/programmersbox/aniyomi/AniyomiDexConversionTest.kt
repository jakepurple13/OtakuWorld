package com.programmersbox.aniyomi

import android.app.Application
import com.programmersbox.kmpextensionloader.ChildFirstClassLoader
import com.programmersbox.kmpextensionloader.DexConverter
import org.objectweb.asm.ClassReader
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Converts the real fixture APK and checks the jar actually links on the JVM. */
class AniyomiDexConversionTest {
    private val root = Files.createTempDirectory("aniyomidex").toFile()

    init {
        AniyomiRuntime.init(Application("aniyomi-test", File(root, "data")))
    }

    @AfterTest fun cleanup() {
        root.deleteRecursively()
    }

    private fun classEntries(jar: File): Map<String, ByteArray> = ZipFile(jar).use { zip ->
        zip.entries().toList()
            .filter { it.name.endsWith(".class") }
            .associate { e -> e.name.removeSuffix(".class").replace('/', '.') to zip.getInputStream(e).use { it.readBytes() } }
    }

    // Activity subclasses (deep-link UrlActivity) are skipped: desktop never loads them.
    @Test fun fixtureConvertsAndLinks() {
        val jar = assertNotNull(DexConverter.convert(AniyomiFixture.apk, File(root, "jars")))
        val entries = classEntries(jar)
        assertTrue(entries.isNotEmpty())
        ChildFirstClassLoader(
            arrayOf(jar.toURI().toURL()),
            javaClass.classLoader,
            JvmAniyomiExtensionLoader.ANIYOMI_PARENT_FIRST,
        ).use { loader ->
            val failures = entries.keys.filterNot { isActivity(it, entries) }.mapNotNull { name ->
                runCatching { Class.forName(name, true, loader) }.exceptionOrNull()?.let { "$name: $it" }
            }
            assertEquals(emptyList(), failures)
        }
    }

    private fun isActivity(name: String, entries: Map<String, ByteArray>): Boolean {
        var current: String? = name
        while (current != null) {
            if (current == "android.app.Activity") return true
            current = entries[current]?.let { ClassReader(it).superName?.replace('/', '.') }
        }
        return false
    }
}
