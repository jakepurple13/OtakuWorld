package com.programmersbox.jsextensionloader

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.File
import java.net.URL
import java.util.jar.JarFile

actual class ExtensionDiscovery(
    private val extensionsDir: () -> File,
    private val bundledResourcesDir: String,
    private val client: HttpClient,
    private val classLoader: ClassLoader? = ExtensionDiscovery::class.java.classLoader,
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
        val loader = classLoader ?: return emptyList()
        val resourceUrl = loader.getResource(bundledResourcesDir) ?: return emptyList()

        // Resources shipped by a dependency module (like this module's own bundled sample
        // extension, consumed by an app that depends on it) arrive on the runtime classpath
        // packaged inside that dependency's jar, not as a plain exploded directory — a
        // `jar:` URL, which `File(url.toURI())` cannot handle ("URI is not hierarchical").
        // Enumerate jar entries directly in that case; fall back to a plain directory
        // listing otherwise (e.g. exploded classes during local test/IDE runs).
        val fileNames = if (resourceUrl.protocol == "jar") {
            listJarEntryFileNames(resourceUrl)
        } else {
            File(resourceUrl.toURI())
                .listFiles { file -> file.extension == "js" || file.extension == "ts" }
                .orEmpty()
                .map { it.name }
        }

        return fileNames.map { fileName ->
            val scriptText = loader.getResourceAsStream("$bundledResourcesDir/$fileName")
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            DiscoveredExtensionSource(
                sourceId = fileName.substringBeforeLast("."),
                fileName = fileName,
                scriptText = scriptText,
                companionManifestJson = null,
            )
        }
    }

    private fun listJarEntryFileNames(resourceUrl: URL): List<String> {
        val jarPath = resourceUrl.path.substringAfter("file:").substringBefore("!")
        return JarFile(jarPath).use { jarFile ->
            jarFile.entries().asSequence()
                .map { it.name }
                .filter { it.startsWith("$bundledResourcesDir/") && !it.endsWith("/") }
                .map { it.substringAfterLast("/") }
                .filter { it.endsWith(".js") || it.endsWith(".ts") }
                .toList()
        }
    }
}
