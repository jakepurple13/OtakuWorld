package com.programmersbox.jsextensionloader

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExtensionDiscoveryTest {

    @Test
    fun scanLocalDirectoryFindsJsAndTsFilesWithCompanionManifests() = runTest {
        val tempDir = kotlin.io.path.createTempDirectory().toFile()
        try {
            File(tempDir, "one.js").writeText("// name: One\n// version: 1.0.0\n")
            File(tempDir, "one.manifest.json").writeText("""{"name":"One","version":"1.0.0"}""")
            File(tempDir, "two.ts").writeText("// name: Two\n// version: 1.0.0\n")
            File(tempDir, "ignored.txt").writeText("not an extension")

            val discovery = ExtensionDiscovery(
                extensionsDir = { tempDir },
                bundledResourcesDir = "js_extensions",
                client = HttpClient(MockEngine { respond("") }),
            )

            val sources = discovery.scanLocalDirectory().sortedBy { it.sourceId }

            assertEquals(2, sources.size)
            assertEquals("one", sources[0].sourceId)
            assertEquals("""{"name":"One","version":"1.0.0"}""", sources[0].companionManifestJson)
            assertEquals("two", sources[1].sourceId)
            assertNull(sources[1].companionManifestJson)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun scanBundledResourcesFindsFilesOnAnExplodedClasspathDirectory() = runTest {
        val classesDir = kotlin.io.path.createTempDirectory().toFile()
        try {
            val resourceDir = File(classesDir, "js_extensions").apply { mkdirs() }
            File(resourceDir, "bundled.js").writeText(SampleExtensionFixture.SCRIPT_TEXT)

            val discovery = ExtensionDiscovery(
                extensionsDir = { kotlin.io.path.createTempDirectory().toFile() },
                bundledResourcesDir = "js_extensions",
                client = HttpClient(MockEngine { respond("") }),
                classLoader = URLClassLoader(arrayOf(classesDir.toURI().toURL()), null),
            )

            val sources = discovery.scanBundledResources()

            assertEquals(1, sources.size)
            assertEquals("bundled", sources.first().sourceId)
            assertEquals(SampleExtensionFixture.SCRIPT_TEXT, sources.first().scriptText)
        } finally {
            classesDir.deleteRecursively()
        }
    }

    @Test
    fun scanBundledResourcesFindsFilesWhenPackagedInsideAJar() = runTest {
        // This is the real-world case for any bundled resource shipped by a dependency
        // module rather than the running application's own exploded output — e.g. this
        // module's own sample extension, consumed by an app like mangaworld:desktop,
        // arrives on the runtime classpath packaged inside jsextensionloader's own jar,
        // not as a plain directory. `File(resourceUrl.toURI())` cannot handle a `jar:`
        // URI ("URI is not hierarchical") — this test guards that regression.
        val jarFile = kotlin.io.path.createTempFile(suffix = ".jar").toFile()
        try {
            JarOutputStream(jarFile.outputStream()).use { jar ->
                jar.putNextEntry(JarEntry("js_extensions/"))
                jar.closeEntry()
                jar.putNextEntry(JarEntry("js_extensions/bundled.js"))
                jar.write(SampleExtensionFixture.SCRIPT_TEXT.toByteArray())
                jar.closeEntry()
            }

            val discovery = ExtensionDiscovery(
                extensionsDir = { kotlin.io.path.createTempDirectory().toFile() },
                bundledResourcesDir = "js_extensions",
                client = HttpClient(MockEngine { respond("") }),
                classLoader = URLClassLoader(arrayOf(jarFile.toURI().toURL()), null),
            )

            val sources = discovery.scanBundledResources()

            assertEquals(1, sources.size)
            assertEquals("bundled", sources.first().sourceId)
            assertEquals(SampleExtensionFixture.SCRIPT_TEXT, sources.first().scriptText)
        } finally {
            jarFile.delete()
        }
    }

    @Test
    fun scanBundledResourcesReturnsEmptyWhenResourceDirIsMissing() = runTest {
        val emptyClasspathDir = kotlin.io.path.createTempDirectory().toFile()
        try {
            val discovery = ExtensionDiscovery(
                extensionsDir = { kotlin.io.path.createTempDirectory().toFile() },
                bundledResourcesDir = "js_extensions",
                client = HttpClient(MockEngine { respond("") }),
                classLoader = URLClassLoader(arrayOf(emptyClasspathDir.toURI().toURL()), null),
            )

            assertTrue(discovery.scanBundledResources().isEmpty())
        } finally {
            emptyClasspathDir.deleteRecursively()
        }
    }
}
