package com.programmersbox.jsextensionloader

import com.programmersbox.extensioninterfaces.ExtensionUpdateInfo

class JsExtensionUpdateRunner(
    private val repository: JsExtensionRepository,
    private val discovery: ExtensionDiscovery,
    private val loader: JSExtensionLoader,
    private val updateChecker: ExtensionUpdateChecker,
    private val settings: JsExtensionUpdateSettings,
    private val registryEndpoint: String?,
    private val onUpdateAvailable: suspend (ExtensionUpdateInfo) -> Unit,
) {
    suspend fun run() {
        val mode = settings.getMode()
        if (mode == ExtensionUpdateMode.DISABLED) return

        val installed = repository.extensions.value.map {
            InstalledExtension(
                id = it.manifest.id,
                currentVersion = it.manifest.version,
                updateUrl = it.manifest.updateUrl,
            )
        }
        val updates = updateChecker.findAvailableUpdates(installed, registryEndpoint)

        when (mode) {
            ExtensionUpdateMode.AUTOMATIC -> updates.forEach { update ->
                val source = discovery.fetchRemote(update.downloadUrl)
                val extension = loader.load(source.scriptText, source.fileName, source.companionManifestJson)
                repository.register(extension)
            }
            ExtensionUpdateMode.NOTIFY -> updates.forEach { onUpdateAvailable(it) }
            ExtensionUpdateMode.DISABLED -> Unit
        }
    }
}
