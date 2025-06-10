package com.programmersbox.kmpextensionloader

import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import java.io.File

/**
 * JVM implementation of SourceLoader that can read Android APKs.
 * This implementation mocks the Android APIs to read the data.
 *
 * @param extensionsDir Directory where extension APK files are located
 * @param sourceType Type of source to load
 * @param sourceRepository Repository to store loaded sources
 */
actual class SourceLoader(
    private val extensionsDir: File,
    sourceType: String,
    private val sourceRepository: SourceRepository,
) {
    private val METADATA_NAME = "programmersbox.otaku.name"
    private val METADATA_CLASS = "programmersbox.otaku.class"
    private val EXTENSION_FEATURE = "programmersbox.otaku.extension"

    private val extensionLoader = ExtensionLoader<Any, List<KmpSourceInformation>>(
        extensionsDir,
        "$EXTENSION_FEATURE.$sourceType",
        METADATA_CLASS
    ) { t, a, p ->
        // Map the loaded class to KmpSourceInformation
        // This is a simplified version of the Android implementation
        listOf(
            KmpSourceInformation(
                apiService = when (t) {
                    is com.programmersbox.kmpmodels.KmpApiService -> t
                    else -> createMockApiService(t.toString())
                },
                name = a.metaData.getString(METADATA_NAME) ?: "Unknown",
                icon = null, // No icon support in JVM
                packageName = p.packageName
            )
        )
    }

    /**
     * Create a mock KmpApiService for testing
     */
    private fun createMockApiService(name: String): com.programmersbox.kmpmodels.KmpApiService {
        return object : com.programmersbox.kmpmodels.KmpApiService {
            override val baseUrl: String = "https://example.com"
            override val serviceName: String = name
            // Other properties and methods have default implementations in the interface
        }
    }

    /**
     * Load sources synchronously
     */
    actual fun load() {
        sourceRepository.setSources(
            extensionLoader
                .loadExtensions()
                .flatten()
                .sortedBy { it.apiService.serviceName }
        )
    }

    /**
     * Load sources asynchronously
     */
    actual suspend fun blockingLoad() {
        sourceRepository.setSources(
            extensionLoader
                .loadExtensionsBlocking()
                .flatten()
                .sortedBy { it.apiService.serviceName }
        )
    }
}
