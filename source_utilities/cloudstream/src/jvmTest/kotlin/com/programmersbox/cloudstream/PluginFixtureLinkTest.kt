package com.programmersbox.cloudstream

import android.app.Activity
import androidx.fragment.app.Fragment
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.programmersbox.cloudstream.plugins.CLOUDSTREAM_PARENT_FIRST
import com.programmersbox.kmpextensionloader.ChildFirstClassLoader
import com.programmersbox.kmpextensionloader.DexConverter
import org.objectweb.asm.ClassReader
import java.io.File
import java.util.zip.ZipFile
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/** A real plugin, converted for the JVM, links against the host: vendored library, shims and android stubs. */
class PluginFixtureLinkTest {
    private val tempDir = createTempDirectory("cloudstream-fixture").toFile()
    private val host: ClassLoader = PluginFixtureLinkTest::class.java.classLoader

    @AfterTest
    fun cleanUp() {
        tempDir.deleteRecursively()
    }

    @Test
    fun fixtureClassesLink() {
        val jar = assertNotNull(DexConverter.convert(CloudstreamFixture.cs3, tempDir), "conversion failed")
        val supers = ZipFile(jar).use { zip ->
            zip.entries().asSequence().filter { it.name.endsWith(".class") }
                .map { entry -> ClassReader(zip.getInputStream(entry).use { it.readBytes() }) }
                .associate { it.className to it.superName }
        }

        ChildFirstClassLoader(arrayOf(jar.toURI().toURL()), host, CLOUDSTREAM_PARENT_FIRST).use { loader ->
            val failures = supers.keys.filterNot { isScreen(it, supers) }.sorted().mapNotNull { name ->
                runCatching { Class.forName(name.replace('/', '.'), true, loader) }.exceptionOrNull()
                    ?.let { "$name: ${it.javaClass.simpleName}: ${it.message?.lineSequence()?.first()}" }
            }
            assertEquals(emptyList(), failures, "classes that fail to load or link")

            val plugin = loader.loadClass(CloudstreamFixture.PLUGIN_CLASS).getDeclaredConstructor().newInstance()
            assertIs<BasePlugin>(plugin)
        }
    }

    /** Activity/Fragment subclasses: desktop never creates screens, so they need not link. */
    private fun isScreen(internalName: String, supers: Map<String, String?>): Boolean {
        var current: String? = internalName
        while (current != null && current in supers) current = supers[current]
        // Deliberately strict (unlike the scanner's SCREEN_TYPES): a screen whose host supertype stub is missing is not skipped, so the missing stub fails the test.
        val hostType = current?.let { runCatching { Class.forName(it.replace('/', '.'), false, host) }.getOrNull() }
            ?: return false
        return Activity::class.java.isAssignableFrom(hostType) || Fragment::class.java.isAssignableFrom(hostType)
    }
}
