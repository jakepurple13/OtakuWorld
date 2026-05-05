package com.programmersbox.kmpextensionloader

import android.app.Application
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpApiServicesCatalog
import com.programmersbox.kmpmodels.KmpExternalApiServicesCatalog
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.models.ApiService
import com.programmersbox.models.ApiServicesCatalog
import com.programmersbox.models.ExternalApiServicesCatalog
import com.programmersbox.models.ExternalCustomApiServicesCatalog
import com.programmersbox.models.SourceInformation
import java.io.File

private const val METADATA_NAME = "programmersbox-otaku-name"
private const val METADATA_CLASS = "programmersbox-otaku-class"
private const val EXTENSION_FEATURE = "programmersbox.otaku.extension"

actual class SourceLoader(
    private val extensionsDir: File,
    sourceType: String,
    private val sourceRepository: SourceRepository,
    private val appDirs: AppDirs,
) {
    private val cacheDir = File(appDirs.getUserCacheDir(), "otaku-plugin-cache")
    private val dataDir = File(appDirs.getUserDataDir())

    private val extensionLoader = ExtensionLoader<Any, List<KmpSourceInformation>>(
        extensionsDir = extensionsDir,
        cacheDir = cacheDir,
        extensionFeature = "$EXTENSION_FEATURE.$sourceType",
        metadataClass = METADATA_CLASS,
        mapping = suspend { t, appInfo, packageInfo ->
            println("Mapping $t")
            val metaName = appInfo.metaData?.getString(METADATA_NAME) ?: "Unknown"
            val pkgName = packageInfo.packageName
            val pluginApp = Application(pkgName, dataDir, appInfo.sourceDir)
            val mapper = JvmModelMapper(pluginApp)

            when (t) {
                is ApiService -> listOf(
                    SourceInformation(
                        apiService = t,
                        name = metaName,
                        icon = null,
                        packageName = pkgName,
                    )
                )

                is ExternalCustomApiServicesCatalog -> {
                    t.initialize(pluginApp)
                    t.getSources().map { it.copy(catalog = t) }
                }

                is ExternalApiServicesCatalog -> {
                    t.initialize(pluginApp)
                    t.getSources().map { it.copy(catalog = t) }
                }

                is ApiServicesCatalog -> t.createSources().map { service ->
                    SourceInformation(
                        apiService = service,
                        name = metaName,
                        icon = null,
                        packageName = pkgName,
                        catalog = t,
                    )
                }

                is KmpApiService -> listOf(
                    KmpSourceInformation(
                        apiService = t,
                        name = metaName,
                        icon = null,
                        packageName = pkgName,
                    )
                )

                is KmpExternalCustomApiServicesCatalog -> {
                    t.initialize()
                    t.createSources().map { service ->
                        KmpSourceInformation(
                            apiService = service,
                            name = metaName,
                            icon = null,
                            packageName = pkgName,
                            catalog = t,
                        )
                    }
                }

                is KmpExternalApiServicesCatalog -> {
                    t.initialize()
                    t.getSources().map { it.copy(catalog = t) }
                }

                is KmpApiServicesCatalog -> t.createSources().map { service ->
                    KmpSourceInformation(
                        apiService = service,
                        name = metaName,
                        icon = null,
                        packageName = pkgName,
                        catalog = t,
                    )
                }

                else -> emptyList()
            }
                .mapNotNull {
                    when (it) {
                        is KmpSourceInformation -> it
                        is SourceInformation -> mapper.mapSourceInformation(it)
                        else -> null
                    }
                }
                .also { println(it) }
        }
    )

    actual fun load() {
        sourceRepository.setSources(
            extensionLoader.loadExtensions().flatten().sortedBy { it.apiService.serviceName }
        )
    }

    actual suspend fun blockingLoad() {
        sourceRepository.setSources(
            extensionLoader.loadExtensionsBlocking().flatten().sortedBy { it.apiService.serviceName }
        )
    }
}
