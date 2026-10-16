package com.programmersbox.aniyomi

import android.app.Application
import com.programmersbox.aniyomi.loader.AniyomiExtensionLoader
import com.programmersbox.aniyomi.repo.AniyomiRepoClient
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class AniyomiCatalog(
    private val application: Application,
    private val settings: NewSettingsHandling,
    private val repoClient: AniyomiRepoClient,
) : KmpExternalCustomApiServicesCatalog {
    override val name: String = "Aniyomi"
    override val hasRemoteSources: Boolean = true

    private val loader = AniyomiExtensionLoader(application, ::aniyomiApiService)

    @Volatile
    private var sources: List<KmpSourceInformation> = emptyList()

    override suspend fun initialize() {
        AniyomiRuntime.init(application)
        // Refresh repo signing keys; on failure keep whatever was fetched earlier this session.
        runCatching { repoClient.fetch(settings.customUrls.first()) }
            .onFailure { if (it is CancellationException) throw it }
        sources = loader.load(
            catalog = this,
            repoSigningKeys = repoClient.signingKeys.map { it.lowercase() }.toSet(),
            trustedIds = settings.trustedAniyomiExtensions.first().toSet(),
            onTrust = { settings.addTrustedAniyomiExtension(it) },
        )
    }

    override fun getSources(): List<KmpSourceInformation> = sources

    override fun shouldReload(packageName: String): Boolean =
        sources.any { it.packageName == packageName } || loader.isExtension(packageName)

    override suspend fun getRemoteSources(customUrls: List<String>): List<KmpRemoteSources> = repoClient.fetch(customUrls)
}
