package com.programmersbox.cloudstream.plugins

import android.content.Context
import com.lagradost.cloudstream3.mapper
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.programmersbox.kmpextensionloader.DexConverter
import com.programmersbox.kmpextensionloader.JarLoaderCache
import java.io.File
import java.util.IdentityHashMap

/**
 * Desktop counterpart of Android's PluginLoader: loads `.cs3` plugins through jars converted into [jarCacheDir].
 * A jar keeps one class loader while any plugin loaded from it is loaded; [unload] of the last one closes it.
 */
class PluginLoader(
    private val jarCacheDir: File,
    private val context: Context,
    parentClassLoader: ClassLoader = PluginLoader::class.java.classLoader,
) {
    private class Entry(val manifest: BasePlugin.Manifest, val classLoader: ClassLoader)

    private val jars = JarLoaderCache<Entry>(parentClassLoader, CLOUDSTREAM_PARENT_FIRST)

    /** Jar path of each loaded plugin. */
    private val liveJars = IdentityHashMap<LoadedPlugin, String>()

    /** Throws if the plugin cannot be loaded; nothing it registered stays registered. */
    @Synchronized
    fun load(plugin: InstalledPlugin): LoadedPlugin {
        val file = File(plugin.path)
        val jar = DexConverter.convert(file, jarCacheDir) ?: error("${plugin.internalName}: could not convert to a jar")
        try {
            val entry = jars.getOrLoad(jar) { classLoader ->
                val manifest = classLoader.getResourceAsStream("manifest.json")
                    ?.use { mapper.readValue(it, BasePlugin.Manifest::class.java) }
                    ?: error("${plugin.internalName}: no manifest.json")
                Entry(manifest, classLoader)
            }
            // Rejected from the manifest, before any plugin code runs.
            check(!entry.manifest.requiresResources) { NEEDS_RESOURCES }
            val className = entry.manifest.pluginClassName
                ?: error("${plugin.internalName}: manifest has no pluginClassName")
            // A new instance per load, as on Android; only the class loader is reused.
            val instance = entry.classLoader.loadClass(className).getDeclaredConstructor().newInstance() as BasePlugin
            instance.filename = file.absolutePath
            val (apis, extractors) = PluginRegistrations.capture {
                if (instance is Plugin) instance.load(context) else instance.load()
            }
            return LoadedPlugin(plugin, instance, apis, extractors).also { liveJars[it] = jar.absolutePath }
        } catch (t: Throwable) {
            // A failed load must not keep its jar's class loader open.
            jars.retain(liveJars.values.toSet())
            throw t
        }
    }

    @Synchronized
    fun unload(loaded: LoadedPlugin) {
        try {
            loaded.instance.beforeUnload()
        } catch (t: Throwable) {
            println("PluginLoader: ${loaded.plugin.internalName} beforeUnload failed: $t")
        }
        PluginRegistrations.remove(loaded.apis, loaded.extractors)
        liveJars.remove(loaded)
        jars.retain(liveJars.values.toSet())
    }

    /** Deletes converted jars that back no [installed] plugin (uninstalled or updated) and no loaded one. */
    @Synchronized
    fun cleanupStaleJars(installed: List<InstalledPlugin>) {
        val keep = installed
            .mapNotNull { runCatching { DexConverter.jarFor(File(it.path), jarCacheDir).name }.getOrNull() }
            .toSet() + liveJars.values.map { File(it).name }
        jarCacheDir.listFiles { f -> DexConverter.isCachedJar(f) }
            ?.filter { it.name !in keep }
            ?.forEach { runCatching { it.delete() } }
    }

    private companion object {
        const val NEEDS_RESOURCES = "Needs Android resources (not supported on desktop)"
    }
}
