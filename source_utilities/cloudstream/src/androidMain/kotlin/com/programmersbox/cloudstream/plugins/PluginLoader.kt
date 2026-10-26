package com.programmersbox.cloudstream.plugins

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import com.lagradost.cloudstream3.mapper
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.Plugin
import dalvik.system.PathClassLoader
import java.io.File

/** Loads `.cs3` plugins the way CloudStream's PluginManager does. */
class PluginLoader(private val context: Context) {

    /** Throws if the plugin cannot be loaded; nothing it registered stays registered. */
    fun load(plugin: InstalledPlugin): LoadedPlugin {
        val file = File(plugin.path)
        val classLoader = PathClassLoader(file.absolutePath, context.classLoader)
        val manifest = classLoader.getResourceAsStream("manifest.json")
            ?.use { mapper.readValue(it, BasePlugin.Manifest::class.java) }
            ?: error("${plugin.internalName}: no manifest.json")
        val className = manifest.pluginClassName ?: error("${plugin.internalName}: manifest has no pluginClassName")
        val instance = classLoader.loadClass(className).getDeclaredConstructor().newInstance() as BasePlugin
        instance.filename = file.absolutePath
        if (manifest.requiresResources && instance is Plugin) {
            instance.resources = resourcesFor(file)
        }
        val (apis, extractors) = PluginRegistrations.capture {
            if (instance is Plugin) instance.load(context) else instance.load()
        }
        return LoadedPlugin(plugin, instance, apis, extractors)
    }

    fun unload(loaded: LoadedPlugin) {
        try {
            loaded.instance.beforeUnload()
        } catch (t: Throwable) {
            println("PluginLoader: ${loaded.plugin.internalName} beforeUnload failed: $t")
        }
        PluginRegistrations.remove(loaded.apis, loaded.extractors)
    }

    // As upstream: AssetManager.addAssetPath is hidden API, reached by reflection.
    private fun resourcesFor(file: File): Resources {
        val assets = AssetManager::class.java.getDeclaredConstructor().newInstance()
        AssetManager::class.java.getMethod("addAssetPath", String::class.java).invoke(assets, file.absolutePath)
        @Suppress("DEPRECATION")
        return Resources(assets, context.resources.displayMetrics, context.resources.configuration)
    }
}
