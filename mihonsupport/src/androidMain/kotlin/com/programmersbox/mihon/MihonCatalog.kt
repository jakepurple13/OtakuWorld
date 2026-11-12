package com.programmersbox.mihon

import android.app.Application
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.mihon.loader.MihonExtensionLoader
import com.programmersbox.mihon.repo.MihonRepoClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class MihonCatalog(
    private val application: Application,
    private val settings: NewSettingsHandling,
    private val repoClient: MihonRepoClient,
) : KmpExternalCustomApiServicesCatalog {
    override val name: String = "Mihon"
    override val hasRemoteSources: Boolean = true

    private val loader = MihonExtensionLoader(application)

    @Volatile
    private var sources: List<KmpSourceInformation> = emptyList()

    private val signingKeyStore = RepoSigningKeyStore(application, "mihon_repo_signing_keys")

    override suspend fun initialize() {
        MihonRuntime.init(application)
        // Refresh repo signing keys; a repo that fails keeps its last known key, also from earlier runs.
        repoClient.restoreSigningKeys(signingKeyStore.load())
        runCatching { repoClient.fetch(settings.customUrls.first()) }
            .onFailure { if (it is CancellationException) throw it }
        signingKeyStore.save(repoClient.signingKeysByRepo)
        sources = loader.load(
            catalog = this,
            repoSigningKeys = repoClient.signingKeys.map { it.lowercase() }.toSet(),
            trustedIds = settings.trustedMihonExtensions.first().toSet(),
            onTrust = { settings.addTrustedMihonExtension(it) },
        )
    }

    override fun getSources(): List<KmpSourceInformation> = sources

    override fun shouldReload(packageName: String): Boolean =
        sources.any { it.packageName == packageName } || loader.isExtension(packageName)

    override suspend fun getRemoteSources(customUrls: List<String>): List<KmpRemoteSources> = repoClient.fetch(customUrls)
}
