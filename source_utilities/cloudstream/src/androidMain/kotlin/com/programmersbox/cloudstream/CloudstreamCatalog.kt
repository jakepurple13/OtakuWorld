package com.programmersbox.cloudstream

import android.app.Application
import com.lagradost.cloudstream3.plugins.Plugin
import com.programmersbox.cloudstream.plugins.InstalledPlugin
import com.programmersbox.cloudstream.plugins.LoadedPlugin
import com.programmersbox.cloudstream.plugins.PluginLoader
import com.programmersbox.cloudstream.plugins.PluginStore
import com.programmersbox.cloudstream.repo.CloudstreamRepoClient
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CloudstreamCatalog(
    private val application: Application,
    private val settings: NewSettingsHandling,
    private val repoClient: CloudstreamRepoClient,
    private val store: PluginStore,
    private val loader: PluginLoader,
) : KmpExternalCustomApiServicesCatalog {
    override val name: String = "CloudStream"
    override val hasRemoteSources: Boolean = true

    /** Guards [shimsInstalled], [loaded] and plugin (un)loads; the source loader may initialize concurrently. */
    private val mutex = Mutex()
    private var shimsInstalled = false
    private val loaded = mutableMapOf<String, LoadedPlugin>()

    @Volatile
    private var sources: List<KmpSourceInformation> = emptyList()

    override suspend fun initialize() = mutex.withLock {
        if (!shimsInstalled) {
            CloudstreamShims.install(application)
            shimsInstalled = true
        }
        val trusted = settings.trustedCloudstreamRepos.first().toSet()
        val installed = store.installed()
        // Drop plugins that were uninstalled, updated (record changed) or are no longer trusted.
        loaded.values
            .filter { it.plugin !in installed || it.plugin.repoUrl !in trusted }
            .forEach { unloadLocked(it.plugin.internalName) }

        val placeholders = installed.mapNotNull { plugin ->
            when {
                plugin.internalName in loaded -> null
                plugin.repoUrl !in trusted -> placeholder(
                    plugin,
                    reason = "Untrusted repo: ${plugin.repoUrl.ifEmpty { "unknown" }}",
                    // A plugin whose repo was lost (rebuilt index) is trusted again by reinstalling it.
                    canTrust = plugin.repoUrl.isNotEmpty(),
                )

                else -> try {
                    loaded[plugin.internalName] = loader.load(plugin)
                    null
                } catch (t: Throwable) {
                    // Plugin code throws Errors too (NoClassDefFoundError for a missing shim).
                    t.printStackTrace()
                    placeholder(plugin, reason = "Failed to load: ${t.message ?: t}", canTrust = false)
                }
            }
        }
        sources = loadedSources() + placeholders
    }

    override fun getSources(): List<KmpSourceInformation> = sources

    override suspend fun getRemoteSources(customUrls: List<String>): List<KmpRemoteSources> = repoClient.fetch(customUrls)

    override suspend fun install(remote: KmpRemoteSources): Boolean {
        val (repoUrl, plugin) = repoClient.pluginFor(remote.downloadLink) ?: return false
        store.install(repoUrl, plugin)
        return true
    }

    override suspend fun uninstall(source: KmpSourceInformation): Boolean {
        val internalName = source.packageName
        if (store.installed().none { it.internalName == internalName }) return false
        mutex.withLock {
            unloadLocked(internalName)
            store.uninstall(internalName)
            sources = sources.filterNot { it.packageName == internalName }
        }
        return true
    }

    private fun unloadLocked(internalName: String) {
        loaded.remove(internalName)?.let(loader::unload)
    }

    private fun loadedSources(): List<KmpSourceInformation> {
        val apis = loaded.values.flatMap { l -> l.apis.map { l to it } }
        val names = serviceNamesFor(apis.map { (l, api) -> l.plugin.internalName to api })
        return apis.zip(names) { (l, api), serviceName ->
            val internalName = l.plugin.internalName
            val openSettings = (l.instance as? Plugin)?.openSettings
            val service = if (openSettings != null) {
                ConfigurableCloudstreamApiService(api, internalName, serviceName, openSettings)
            } else {
                CloudstreamApiService(api, internalName, serviceName)
            }
            KmpSourceInformation(
                apiService = service,
                name = service.displayName,
                icon = null,
                packageName = internalName,
                catalog = this,
            )
        }
    }

    private fun placeholder(plugin: InstalledPlugin, reason: String, canTrust: Boolean) = KmpSourceInformation(
        apiService = UntrustedCloudstreamApiService(
            internalName = plugin.internalName,
            reason = reason,
            canTrust = canTrust,
            onTrust = { settings.addTrustedCloudstreamRepo(plugin.repoUrl) },
        ),
        name = "${plugin.internalName} ($reason)",
        icon = null,
        packageName = plugin.internalName,
        catalog = this,
    )
}
