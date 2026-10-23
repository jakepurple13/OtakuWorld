package com.programmersbox.aniyomi

import android.app.Application
import com.programmersbox.aniyomi.repo.AniyomiRepoClient
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class AniyomiCatalog internal constructor(
    private val application: Application,
    private val repoClient: AniyomiRepoClient,
    private val store: AniyomiApkStore,
    private val customUrls: suspend () -> List<String>,
    private val trustedIds: suspend () -> Set<String>,
    private val addTrusted: suspend (String) -> Unit,
) : KmpExternalCustomApiServicesCatalog {

    constructor(
        application: Application,
        settings: NewSettingsHandling,
        repoClient: AniyomiRepoClient,
        store: AniyomiApkStore,
    ) : this(
        application = application,
        repoClient = repoClient,
        store = store,
        customUrls = { settings.customUrls.first() },
        trustedIds = { settings.trustedAniyomiExtensions.first().toSet() },
        addTrusted = { settings.addTrustedAniyomiExtension(it) },
    )

    override val name: String = "Aniyomi"
    override val hasRemoteSources: Boolean = true

    private val loader = JvmAniyomiExtensionLoader(store)

    @Volatile
    private var sources: List<KmpSourceInformation> = emptyList()

    override suspend fun initialize() {
        AniyomiRuntime.init(application)
        // Refresh repo signing keys; on failure keep whatever was fetched earlier this session.
        runCatching { repoClient.fetch(customUrls()) }
            .onFailure { if (it is CancellationException) throw it }
        sources = loader.load(
            catalog = this,
            repoSigningKeys = repoClient.signingKeys.map { it.lowercase() }.toSet(),
            trustedIds = trustedIds(),
            onTrust = addTrusted,
        )
    }

    override fun getSources(): List<KmpSourceInformation> = sources

    override fun shouldReload(packageName: String): Boolean =
        sources.any { it.packageName == packageName } || loader.isExtension(packageName)

    override suspend fun getRemoteSources(customUrls: List<String>): List<KmpRemoteSources> = repoClient.fetch(customUrls)

    override suspend fun uninstall(source: KmpSourceInformation): Boolean = store.uninstall(source.packageName)
}
