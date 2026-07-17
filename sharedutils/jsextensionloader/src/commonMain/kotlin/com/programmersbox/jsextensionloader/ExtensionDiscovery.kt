package com.programmersbox.jsextensionloader

data class DiscoveredExtensionSource(
    val sourceId: String,
    val fileName: String,
    val scriptText: String,
    val companionManifestJson: String?,
)

expect class ExtensionDiscovery {
    suspend fun scanLocalDirectory(): List<DiscoveredExtensionSource>
    suspend fun fetchRemote(url: String): DiscoveredExtensionSource
    suspend fun scanBundledResources(): List<DiscoveredExtensionSource>
}
