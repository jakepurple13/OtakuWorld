package com.programmersbox.kmpextensionloader

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.models.ApiService
import com.programmersbox.models.ApiServicesCatalog
import com.programmersbox.models.ExternalApiServicesCatalog
import com.programmersbox.models.ExternalCustomApiServicesCatalog
import com.programmersbox.models.SourceInformation
import kotlinx.coroutines.runBlocking
import java.io.File

private const val METADATA_NAME = "programmersbox.otaku.name"
private const val METADATA_CLASS = "programmersbox.otaku.class"
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
    ) { t, appInfo, packageInfo ->
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
                runBlocking { t.initialize(pluginApp) }
                t.getSources().map { it.copy(catalog = t) }
            }

            is ExternalApiServicesCatalog -> {
                runBlocking { t.initialize(pluginApp) }
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

            else -> emptyList()
        }.map { mapper.mapSourceInformation(it) }
    }

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
