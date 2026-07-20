package com.programmersbox.jsextensionloader

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.File

actual class ExtensionDiscovery(
    private val context: Context,
    private val extensionsSubDir: String,
    private val client: HttpClient,
) {
    actual suspend fun scanLocalDirectory(): List<DiscoveredExtensionSource> {
        val dir = File(context.filesDir, extensionsSubDir)
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

    // Bundled scripts ship as res/raw entries rather than assets/ - the AGP Kotlin
    // Multiplatform Android Library plugin (com.android.kotlin.multiplatform.library)
    // has no assets-merging support, so anything placed under androidMain/assets is
    // silently dropped from the APK. res/raw strips extensions from resource names,
    // so every bundled entry is treated as a plain .js source.
    actual suspend fun scanBundledResources(): List<DiscoveredExtensionSource> {
        return R.raw::class.java.fields.map { field ->
            val resId = field.getInt(null)
            val resourceName = context.resources.getResourceEntryName(resId)
            val scriptText = context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
            DiscoveredExtensionSource(
                sourceId = resourceName,
                fileName = "$resourceName.js",
                scriptText = scriptText,
                companionManifestJson = null,
            )
        }
    }
}
