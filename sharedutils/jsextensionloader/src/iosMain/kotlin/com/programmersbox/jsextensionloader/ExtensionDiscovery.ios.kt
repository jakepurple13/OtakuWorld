package com.programmersbox.jsextensionloader

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

@OptIn(ExperimentalForeignApi::class)
actual class ExtensionDiscovery(
    private val extensionsDirectoryPath: String,
    private val bundledResourcesSubdirectory: String,
    private val client: HttpClient,
) {
    actual suspend fun scanLocalDirectory(): List<DiscoveredExtensionSource> {
        val fileManager = NSFileManager.defaultManager
        val fileNames = (fileManager.contentsOfDirectoryAtPath(extensionsDirectoryPath, null) as? List<String>)
            .orEmpty()
            .filter { it.endsWith(".js") || it.endsWith(".ts") }
        return fileNames.map { fileName ->
            val fullPath = "$extensionsDirectoryPath/$fileName"
            val scriptText = NSString.stringWithContentsOfFile(fullPath, NSUTF8StringEncoding, null) as String
            val manifestPath = "$extensionsDirectoryPath/${fileName.substringBeforeLast(".")}.manifest.json"
            val manifestText = if (fileManager.fileExistsAtPath(manifestPath)) {
                NSString.stringWithContentsOfFile(manifestPath, NSUTF8StringEncoding, null) as String?
            } else {
                null
            }
            DiscoveredExtensionSource(
                sourceId = fileName.substringBeforeLast("."),
                fileName = fileName,
                scriptText = scriptText,
                companionManifestJson = manifestText,
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
        val bundlePath = NSBundle.mainBundle.pathForResource(bundledResourcesSubdirectory, null) ?: return emptyList()
        val fileManager = NSFileManager.defaultManager
        val fileNames = (fileManager.contentsOfDirectoryAtPath(bundlePath, null) as? List<String>)
            .orEmpty()
            .filter { it.endsWith(".js") || it.endsWith(".ts") }
        return fileNames.map { fileName ->
            val fullPath = "$bundlePath/$fileName"
            val scriptText = NSString.stringWithContentsOfFile(fullPath, NSUTF8StringEncoding, null) as String
            DiscoveredExtensionSource(
                sourceId = fileName.substringBeforeLast("."),
                fileName = fileName,
                scriptText = scriptText,
                companionManifestJson = null,
            )
        }
    }
}
