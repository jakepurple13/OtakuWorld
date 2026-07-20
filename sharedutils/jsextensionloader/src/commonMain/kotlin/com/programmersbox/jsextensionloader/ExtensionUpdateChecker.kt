package com.programmersbox.jsextensionloader

import com.programmersbox.extensioninterfaces.ExtensionUpdateInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

data class InstalledExtension(
    val id: String,
    val currentVersion: String,
    val updateUrl: String?,
)

sealed interface ExtensionUpdateSource {
    data class CentralizedRegistry(val endpoint: String) : ExtensionUpdateSource
    data class PerExtensionUrl(val url: String) : ExtensionUpdateSource
}

class ExtensionUpdateChecker(private val client: HttpClient) {

    suspend fun checkCentralizedRegistry(source: ExtensionUpdateSource.CentralizedRegistry): List<ExtensionUpdateInfo> =
        client.get(source.endpoint).body()

    suspend fun checkPerExtensionUrl(source: ExtensionUpdateSource.PerExtensionUrl): ExtensionUpdateInfo =
        client.get(source.url).body()

    suspend fun findAvailableUpdates(
        installed: List<InstalledExtension>,
        registryEndpoint: String?,
    ): List<ExtensionUpdateInfo> {
        val registryUpdates = registryEndpoint
            ?.let { checkCentralizedRegistry(ExtensionUpdateSource.CentralizedRegistry(it)) }
            .orEmpty()

        val coveredIds = registryUpdates.map { it.id }.toSet()
        val perExtensionUpdates = installed
            .filter { it.id !in coveredIds && it.updateUrl != null }
            .map { checkPerExtensionUrl(ExtensionUpdateSource.PerExtensionUrl(it.updateUrl!!)) }

        return (registryUpdates + perExtensionUpdates).filter { update ->
            val current = installed.find { it.id == update.id }?.currentVersion
            current != null && SemVerCompare.isNewer(current, update.latestVersion)
        }
    }
}
