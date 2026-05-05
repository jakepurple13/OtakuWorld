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
import java.net.URLClassLoader

class ExtensionLoader<T, R>(
    private val extensionsDir: File,
    private val cacheDir: File,
    private val extensionFeature: String,
    private val metadataClass: String,
    private val mapping: suspend (T, ApplicationInfo, PackageInfo) -> R,
) {
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

    private fun findExtensionJars(): List<File> {
        if (!extensionsDir.exists() || !extensionsDir.isDirectory) return emptyList()
        return extensionsDir
            .listFiles { f -> f.isFile && f.extension.equals("jar", ignoreCase = true) }
            ?.toList()
            ?: emptyList()
    }

    private suspend fun loadExtension(jarFile: File, mapped: suspend (T, ApplicationInfo, PackageInfo) -> R): List<R> {
        return runCatching {
            val manifest = JarManifestParser.parse(jarFile)

            println("$jarFile: $manifest")

            println(manifest.features)
            println(manifest.metaData)
            println("Extension Feature: $extensionFeature")

            if (!manifest.features.contains(extensionFeature) && !manifest.metaData.contains(extensionFeature)) return emptyList()

            val classLoader = URLClassLoader(
                arrayOf(jarFile.toURI().toURL()),
                this::class.java.classLoader,
            )

            println("Classloader: $classLoader")

            val packageInfo = PackageInfo().apply {
                packageName = manifest.packageName
                versionName = manifest.versionName
                reqFeatures = manifest.features.map { FeatureInfo().apply { name = it } }.toTypedArray()
            }

            println("PackageInfo: $packageInfo")

            val metaBundle = Bundle().apply {
                manifest.metaData.forEach { (k, v) -> putString(k, v) }
            }

            println("MetaBundle: $metaBundle")

            val appInfo = ApplicationInfo().apply {
                packageName = manifest.packageName
                sourceDir = jarFile.absolutePath
                metaData = metaBundle
            }

            println("AppInfo: $appInfo")

            val classNames = metaBundle.getString(metadataClass)
                .also { println("Metadata Class: $metadataClass") }
                .orEmpty()
                .split(";")
                .map { cls ->
                    val trimmed = cls.trim()
                    if (trimmed.startsWith(".")) manifest.packageName + trimmed else trimmed
                }
                .filter { it.isNotBlank() }

            println("ClassNames: $classNames")

            classNames.mapNotNull { className ->
                runCatching {
                    val loadedClass = classLoader.loadClass(className)

                    // 1. Try to get the singleton INSTANCE if it's a Kotlin `object`
                    val instance = try {
                        loadedClass.getDeclaredField("INSTANCE").get(null)
                    } catch (e: NoSuchFieldException) {
                        // 2. If there is no INSTANCE field, it's a normal `class`. Instantiate it.
                        loadedClass.getDeclaredConstructor().newInstance()
                    }

                    @Suppress("UNCHECKED_CAST")
                    instance as? T
                }
                    .onFailure { e ->
                        e.printStackTrace()
                        val cause = e.cause?.let { " caused by: ${it.message}" } ?: ""
                        println("ExtensionLoader: failed to load $className: ${e.message}$cause")
                    }
                    .getOrNull()
            }.map { mapped(it, appInfo, packageInfo) }
        }
            .onFailure { println("ExtensionLoader: failed to load ${jarFile.name}: ${it.message}") }
            .getOrElse { emptyList() }
    }
}
