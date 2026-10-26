package com.programmersbox.cloudstream

import android.app.Application
import com.lagradost.cloudstream3.APIHolder
import com.programmersbox.cloudstream.plugins.InstalledPlugin
import com.programmersbox.cloudstream.plugins.PluginLoader
import com.programmersbox.kmpextensionloader.DexConverter
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JvmPluginLoaderTest {
    private val tempDir = createTempDirectory("cloudstream-loader").toFile()
    private val jarCacheDir = File(tempDir, "jars")
    private val registrations = GlobalRegistrations()
    private val loader = PluginLoader(jarCacheDir, Application("cloudstream-test", File(tempDir, "data")))

    @AfterTest
    fun cleanUp() {
        registrations.removeAdded()
        tempDir.deleteRecursively()
    }

    private fun fixture(): InstalledPlugin {
        CloudstreamFixture.assumeAvailable()
        return InstalledPlugin(
            repoUrl = CloudstreamFixture.REPO_URL,
            internalName = CloudstreamFixture.INTERNAL_NAME,
            version = CloudstreamFixture.VERSION,
            path = CloudstreamFixture.cs3.absolutePath,
        )
    }

    private fun testPlugin(name: String, cs3: File) = InstalledPlugin("https://repo.test/repo.json", name, 1, cs3.absolutePath)

    @Test
    fun loadsFixtureAndRegistersApis() {
        val loaded = loader.load(fixture())

        assertEquals(CloudstreamFixture.PLUGIN_CLASS, loaded.instance.javaClass.name)
        assertTrue(loaded.apis.isNotEmpty(), "plugin registered no MainAPI")
        assertTrue(loaded.apis.all { api -> APIHolder.allProviders.withLock { APIHolder.allProviders.any { it === api } } })
        val names = serviceNamesFor(loaded.apis.map { CloudstreamFixture.INTERNAL_NAME to it })
        assertEquals(loaded.apis.map { "cloudstream:${it.name}" }, names)

        loader.unload(loaded)
        assertEquals(emptyList(), registrations.addedApis())
        assertEquals(emptyList(), registrations.addedExtractors())
    }

    @Test
    fun throwingPluginLeavesNoRegistrations() {
        val cs3 = TestPlugins.cs3(tempDir, jarCacheDir, "Throwing", ThrowingTestPlugin::class.java)

        val e = assertFailsWith<IllegalStateException> { loader.load(testPlugin("Throwing", cs3)) }

        assertEquals("ThrowingTestPlugin load failed", e.message)
        assertEquals(emptyList(), registrations.addedApis())
        assertEquals(emptyList(), registrations.addedExtractors())
    }

    @Test
    fun requiresResourcesGivesClearError() {
        val cs3 = TestPlugins.cs3(tempDir, jarCacheDir, "Resources", ConfigurableTestPlugin::class.java, requiresResources = true)

        val e = assertFailsWith<IllegalStateException> { loader.load(testPlugin("Resources", cs3)) }

        assertContains(e.message.orEmpty(), "Needs Android resources (not supported on desktop)")
        // Rejected from the manifest, before any plugin code runs.
        assertEquals(emptyList(), registrations.addedApis())
    }

    @Test
    fun reloadReusesLoaderForSameJar() {
        val first = loader.load(fixture())
        val second = loader.load(fixture())

        val classLoader = first.instance.javaClass.classLoader
        assertSame(classLoader, second.instance.javaClass.classLoader)
        assertNotSame(first.instance, second.instance)

        // The loader stays open while any plugin from its jar is loaded.
        loader.unload(first)
        assertNotNull(classLoader.getResource("manifest.json"))
        assertTrue(second.apis.isNotEmpty())
        assertFalse(first.apis.any { api -> APIHolder.allProviders.withLock { APIHolder.allProviders.any { it === api } } })

        loader.unload(second)
        assertNull(classLoader.getResource("manifest.json"), "class loader still open")
    }

    @Test
    fun cleanupStaleJarsKeepsOnlyJarsOfInstalledPlugins() {
        val kept = testPlugin("Kept", TestPlugins.cs3(tempDir, jarCacheDir, "Kept", ConfigurableTestPlugin::class.java))
        val gone = TestPlugins.cs3(tempDir, jarCacheDir, "Gone", ThrowingTestPlugin::class.java)
        val keptJar = DexConverter.jarFor(File(kept.path), jarCacheDir)
        val goneJar = DexConverter.jarFor(gone, jarCacheDir)
        assertTrue(keptJar.isFile && goneJar.isFile)
        val unrelated = File(jarCacheDir, "notes.txt").also { it.writeText("x") }

        loader.cleanupStaleJars(listOf(kept))

        assertTrue(keptJar.isFile, "installed plugin's jar was deleted")
        assertFalse(goneJar.exists(), "stale jar should be cleaned up")
        assertTrue(unrelated.isFile, "only converted jars are touched")
    }
}
