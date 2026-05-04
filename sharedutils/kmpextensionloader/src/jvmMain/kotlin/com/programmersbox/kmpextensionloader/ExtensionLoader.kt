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
            findExtensionApks()
                .map { async { loadExtension(it, mapped) } }
                .flatMap { it.await() }
        }

    suspend fun loadExtensionsBlocking(mapped: suspend (T, ApplicationInfo, PackageInfo) -> R = mapping): List<R> =
        coroutineScope {
            findExtensionApks()
                .map { async { loadExtension(it, mapped) } }
                .awaitAll()
                .flatten()
        }

    private fun findExtensionApks(): List<File> {
        if (!extensionsDir.exists() || !extensionsDir.isDirectory) return emptyList()
        return extensionsDir
            .listFiles { f -> f.isFile && f.extension.equals("apk", ignoreCase = true) }
            ?.toList()
            ?: emptyList()
    }

    private suspend fun loadExtension(apkFile: File, mapped: suspend (T, ApplicationInfo, PackageInfo) -> R): List<R> {
        return runCatching {
            val manifest = ApkManifestParser.parse(apkFile)

            if (!manifest.features.contains(extensionFeature)) return emptyList()

            //TODO: Need to only read jar files

            val jar = DexConverter.convert(apkFile, cacheDir) ?: return emptyList()

            val classLoader = URLClassLoader(
                arrayOf(jar.toURI().toURL()),
                this::class.java.classLoader,
            )

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
                sourceDir = apkFile.absolutePath
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

            classNames.mapNotNull { className ->
                runCatching {
                    @Suppress("UNCHECKED_CAST")
                    Class.forName(className, false, classLoader)
                        .getDeclaredConstructor()
                        .newInstance() as? T
                }
                    .onFailure { e ->
                        val cause = e.cause?.let { " caused by: ${it.message}" } ?: ""
                        println("ExtensionLoader: failed to load $className: ${e.message}$cause")
                    }
                    .getOrNull()
            }.map { mapped(it, appInfo, packageInfo) }
        }
            .onFailure { println("ExtensionLoader: failed to load ${apkFile.name}: ${it.message}") }
            .getOrElse { emptyList() }
    }
}
