package com.programmersbox.mihon

import android.app.Application
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.mihon.loader.MihonExtensionLoader
import com.programmersbox.mihon.repo.MihonRepoClient
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

    override suspend fun initialize() {
        MihonRuntime.init(application)
        // Refresh repo signing keys; on failure keep whatever was fetched earlier this session.
        runCatching { repoClient.fetch(settings.customUrls.first()) }
        sources = loader.load(
            catalog = this,
            repoSigningKeys = repoClient.signingKeys.map { it.lowercase() }.toSet(),
            trustedIds = settings.trustedMihonExtensions.first().toSet(),
            onTrust = { settings.addTrustedMihonExtension(it) },
        )
    }

    override fun getSources(): List<KmpSourceInformation> = sources

    override fun shouldReload(packageName: String): Boolean = loader.isExtension(packageName)

    override suspend fun getRemoteSources(customUrls: List<String>): List<KmpRemoteSources> = repoClient.fetch(customUrls)
}
