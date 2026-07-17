package com.programmersbox.jsextensionloader

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.File

actual class ExtensionDiscovery(
    private val extensionsDir: () -> File,
    private val bundledResourcesDir: String,
    private val client: HttpClient,
) {
    actual suspend fun scanLocalDirectory(): List<DiscoveredExtensionSource> {
        val dir = extensionsDir()
        val files = dir.listFiles { file -> file.extension == "js" || file.extension == "ts" }.orEmpty()
        return files.map { file ->
            val manifestFile = File(dir, "${file.nameWithoutExtension}.manifest.json")
            DiscoveredExtensionSource(
                sourceId = file.nameWithoutExtension,
                fileName = file.name,
                scriptText = file.readText(),
                companionManifestJson = manifestFile.takeIf { it.exists() }?.readText(),
            )
        }
    }

    actual suspend fun fetchRemote(url: String): DiscoveredExtensionSource {
        val scriptText = client.get(url).bodyAsText()
        val fileName = url.substringAfterLast("/")
        return DiscoveredExtensionSource(
            sourceId = fileName.substringBeforeLast("."),
            fileName = fileName,
            scriptText = scriptText,
            companionManifestJson = null,
        )
    }

    actual suspend fun scanBundledResources(): List<DiscoveredExtensionSource> {
        val resourceUrl = ExtensionDiscovery::class.java.classLoader?.getResource(bundledResourcesDir)
            ?: return emptyList()
        val dir = File(resourceUrl.toURI())
        val files = dir.listFiles { file -> file.extension == "js" || file.extension == "ts" }.orEmpty()
        return files.map { file ->
            DiscoveredExtensionSource(
                sourceId = file.nameWithoutExtension,
                fileName = file.name,
                scriptText = file.readText(),
                companionManifestJson = null,
            )
        }
    }
}
