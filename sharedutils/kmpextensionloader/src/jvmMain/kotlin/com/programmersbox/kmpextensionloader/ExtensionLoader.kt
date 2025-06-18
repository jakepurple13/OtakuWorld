package com.programmersbox.kmpextensionloader

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URLClassLoader

/**
 * JVM implementation of ExtensionLoader that can read APK files.
 * APK files are essentially JAR/ZIP files, so we can use JarFile to read them.
 *
 * @param extensionsDir Directory where extension APK files are located
 * @param extensionFeature Feature name to filter extensions
 * @param metadataClass Metadata class name to load
 * @param mapping Function to map loaded classes to source information
 */
class ExtensionLoader<T, R>(
    private val extensionsDir: File,
    private val extensionFeature: String,
    private val metadataClass: String,
    private val mapping: (T, MockApplicationInfo, MockPackageInfo) -> R,
) {
    /**
     * Load extensions synchronously
     */
    fun loadExtensions(mapped: (T, MockApplicationInfo, MockPackageInfo) -> R = mapping): List<R> {
        val extensions = findExtensionApks()

        return runBlocking {
            extensions
                .map { async { loadExtension(it, mapped) } }
                .flatMap { it.await() }
        }
    }

    /**
     * Load extensions asynchronously
     */
    suspend fun loadExtensionsBlocking(mapped: (T, MockApplicationInfo, MockPackageInfo) -> R = mapping): List<R> {
        val extensions = findExtensionApks()

        return runBlocking {
            extensions
                .map { async { loadExtension(it, mapped) } }
                .flatMap { it.await() }
        }
    }

    /**
     * Find all APK files in the extensions directory
     */
    private fun findExtensionApks(): List<File> {
        if (!extensionsDir.exists() || !extensionsDir.isDirectory) {
            return emptyList()
        }

        return extensionsDir.listFiles { file ->
            file.isFile && file.extension.equals("apk", ignoreCase = true)
        }?.toList() ?: emptyList()
    }

    /**
     * Load a single extension from an APK file
     */
    private fun loadExtension(apkFile: File, mapped: (T, MockApplicationInfo, MockPackageInfo) -> R): List<R> {
        try {
            // Create mock package info
            val packageInfo = MockPackageInfo(apkFile.nameWithoutExtension)

            // Create mock application info with metadata
            val appInfo = MockApplicationInfo(apkFile.absolutePath)
            appInfo.metaData.put(metadataClass, extractMainClassName(apkFile))

            // Check if this APK has the required feature
            if (!hasFeature(apkFile, extensionFeature)) {
                return emptyList()
            }

            // Load classes from the APK
            val classLoader = URLClassLoader(arrayOf(apkFile.toURI().toURL()), this::class.java.classLoader)

            return appInfo.metaData.getString(metadataClass)
                .orEmpty()
                .split(";")
                .map {
                    val sourceClass = it.trim()
                    if (sourceClass.startsWith(".")) {
                        packageInfo.packageName + sourceClass
                    } else {
                        sourceClass
                    }
                }
                .mapNotNull {
                    runCatching {
                        @Suppress("UNCHECKED_CAST")
                        Class.forName(it, false, classLoader)
                            .getDeclaredConstructor()
                            .newInstance() as? T
                    }
                        .onFailure { it.printStackTrace() }
                        .getOrNull()
                }
                .map { mapped(it, appInfo, packageInfo) }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    /**
     * Extract the main class name from the APK manifest
     */
    private fun extractMainClassName(apkFile: File): String {
        // In a real implementation, this would parse the APK manifest
        // For simplicity, we'll just return a default class name
        return "com.programmersbox.extension.MainClass"
    }

    /**
     * Check if the APK has the required feature
     */
    private fun hasFeature(apkFile: File, featureName: String): Boolean {
        // In a real implementation, this would check the APK manifest
        // For simplicity, we'll just return true
        return true
    }
}

/**
 * Mock of Android's ApplicationInfo class
 */
class MockApplicationInfo(val sourceDir: String) {
    val metaData = MockBundle()
}

/**
 * Mock of Android's PackageInfo class
 */
class MockPackageInfo(val packageName: String) {
    val reqFeatures: Array<MockFeatureInfo>? = arrayOf(MockFeatureInfo())
}

/**
 * Mock of Android's FeatureInfo class
 */
class MockFeatureInfo {
    var name: String? = null
}

/**
 * Mock of Android's Bundle class
 */
class MockBundle {
    private val data = mutableMapOf<String, String>()

    fun put(key: String, value: String) {
        data[key] = value
    }

    fun getString(key: String): String? {
        return data[key]
    }
}