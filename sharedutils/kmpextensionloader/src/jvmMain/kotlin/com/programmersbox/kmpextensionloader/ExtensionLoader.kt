package com.programmersbox.kmpextensionloader

import android.content.pm.ApplicationInfo
import android.content.pm.FeatureInfo
import android.content.pm.PackageInfo
import android.os.Bundle
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.File

class ExtensionLoader<T, R>(
    private val extensionsDir: suspend () -> File,
    private val cacheDir: File,
    private val extensionFeature: String,
    private val metadataClass: String,
    private val mapping: suspend (T, ApplicationInfo, PackageInfo) -> R,
) {
    // Fully parent-first like a plain URLClassLoader: every class name starts with "".
    private val loaders = JarLoaderCache<List<Any>>(this::class.java.classLoader, listOf(""))

    fun loadExtensions(mapped: suspend (T, ApplicationInfo, PackageInfo) -> R = mapping): List<R> =
        runBlocking {
            findExtensionJars()
                .map { async { loadExtension(it, mapped) } }
                .flatMap { it.await() }
        }

    suspend fun loadExtensionsBlocking(mapped: suspend (T, ApplicationInfo, PackageInfo) -> R = mapping): List<R> =
        coroutineScope {
            findExtensionJars()
                .map { async { loadExtension(it, mapped) } }
                .awaitAll()
                .flatten()
        }

    /** Also closes the class loaders of jars removed or replaced since the last call. */
    private suspend fun findExtensionJars(): List<File> {
        val extensionsDir = extensionsDir()
        val jars = if (!extensionsDir.exists() || !extensionsDir.isDirectory) {
            emptyList()
        } else {
            extensionsDir
                .listFiles { f -> f.isFile && f.extension.equals("jar", ignoreCase = true) }
                ?.toList()
                ?: emptyList()
        }
        loaders.retain(jars.map(::cacheKey).toSet())
        return jars
    }

    /** Jar names are not content hashes here, so the modification time is part of the key. */
    private fun cacheKey(jarFile: File): String = jarFile.absolutePath + "@" + jarFile.lastModified()

    private suspend fun loadExtension(jarFile: File, mapped: suspend (T, ApplicationInfo, PackageInfo) -> R): List<R> {
        return runCatching {
            val manifest = JarManifestParser.parse(jarFile)

            if (!manifest.features.contains(extensionFeature) && !manifest.metaData.contains(extensionFeature)) return emptyList()

            val packageInfo = PackageInfo().apply {
                packageName = manifest.packageName
                versionName = manifest.versionName
                reqFeatures = manifest.features.map { FeatureInfo().apply { name = it } }.toTypedArray()
            }

            val metaBundle = Bundle().apply {
                manifest.metaData.forEach { (k, v) -> putString(k, v) }
            }

            val appInfo = ApplicationInfo().apply {
                packageName = manifest.packageName
                sourceDir = jarFile.absolutePath
                metaData = metaBundle
            }

            val classNames = metaBundle.getString(metadataClass)
                .orEmpty()
                .split(";")
                .map { cls ->
                    val trimmed = cls.trim()
                    if (trimmed.startsWith(".")) manifest.packageName + trimmed else trimmed
                }
                .filter { it.isNotBlank() }

            // An unchanged jar keeps its class loader and instances across reloads.
            val instances = loaders.getOrLoad(jarFile, cacheKey(jarFile)) { classLoader ->
                classNames.mapNotNull { className ->
                    runCatching {
                        val loadedClass = classLoader.loadClass(className)

                        // 1. Try to get the singleton INSTANCE if it's a Kotlin `object`
                        try {
                            loadedClass.getDeclaredField("INSTANCE").get(null)
                        } catch (e: NoSuchFieldException) {
                            // 2. If there is no INSTANCE field, it's a normal `class`. Instantiate it.
                            loadedClass.getDeclaredConstructor().newInstance()
                        }
                    }
                        .onFailure { e ->
                            e.printStackTrace()
                            val cause = e.cause?.let { " caused by: ${it.message}" } ?: ""
                            println("ExtensionLoader: failed to load $className: ${e.message}$cause")
                        }
                        .getOrNull()
                }
            }

            @Suppress("UNCHECKED_CAST")
            instances.mapNotNull { it as? T }.map { mapped(it, appInfo, packageInfo) }
        }
            .onFailure { println("ExtensionLoader: failed to load ${jarFile.name}: ${it.message}") }
            .getOrElse { emptyList() }
    }
}
