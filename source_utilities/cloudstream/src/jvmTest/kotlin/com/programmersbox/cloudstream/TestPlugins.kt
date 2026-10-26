package com.programmersbox.cloudstream

import android.content.Context
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.extractorApis
import com.programmersbox.cloudstream.plugins.PluginRegistrations
import com.programmersbox.kmpextensionloader.DexConverter
import java.io.File
import java.util.Collections
import java.util.IdentityHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Registers an API, then fails like a plugin whose load hits a missing class. */
class ThrowingTestPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(FakeMainApi("Throwing"))
        error("ThrowingTestPlugin load failed")
    }
}

/** Offers a settings screen, as most real plugins do. */
class ConfigurableTestPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(FakeMainApi("Configurable"))
        openSettings = { }
    }
}

/**
 * Test plugins whose classes live in the test sources. Their `.cs3` holds only a manifest, so the converted
 * jar is seeded where [DexConverter] caches it; the loader then finds the classes through its parent.
 */
object TestPlugins {
    fun cs3(
        dir: File,
        jarCacheDir: File,
        name: String,
        pluginClass: Class<out BasePlugin>,
        requiresResources: Boolean = false,
    ): File {
        val manifest = """{"pluginClassName":"${pluginClass.name}","name":"$name","version":1,"requiresResources":$requiresResources}"""
        val cs3 = File(dir.also { it.mkdirs() }, "$name.cs3")
        writeManifestZip(cs3, manifest)
        writeManifestZip(DexConverter.jarFor(cs3, jarCacheDir.also { it.mkdirs() }), manifest)
        return cs3
    }

    private fun writeManifestZip(file: File, manifest: String) {
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toByteArray())
            zip.closeEntry()
        }
    }
}

/** What is in CloudStream's global registries; [removeAdded] drops whatever a test added since. */
class GlobalRegistrations {
    private val apis = identitySet(APIHolder.allProviders.withLock { APIHolder.allProviders.toList() })
    private val extractors = identitySet(extractorApis.withLock { extractorApis.toList() })

    fun addedApis(): List<MainAPI> = APIHolder.allProviders.withLock { APIHolder.allProviders.filterNot { it in apis } }

    fun addedExtractors(): List<ExtractorApi> = extractorApis.withLock { extractorApis.filterNot { it in extractors } }

    fun removeAdded() = PluginRegistrations.remove(addedApis(), addedExtractors())

    private fun <T> identitySet(items: List<T>): Set<T> =
        Collections.newSetFromMap(IdentityHashMap<T, Boolean>()).apply { addAll(items) }
}
