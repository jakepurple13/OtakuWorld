package com.programmersbox.kmpextensionloader

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.ModelMapper
import com.programmersbox.kmpmodels.NativeCatalogs
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.models.ApiService
import com.programmersbox.models.ApiServicesCatalog
import com.programmersbox.models.ExternalApiServicesCatalog
import com.programmersbox.models.ExternalCustomApiServicesCatalog
import com.programmersbox.models.SourceInformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


private const val METADATA_NAME = "programmersbox.otaku.name"
private const val METADATA_CLASS = "programmersbox.otaku.class"
private const val EXTENSION_FEATURE = "programmersbox.otaku.extension"

// Legacy bridges register their own Injekt bindings (e.g. NetworkHelper) that clash with the app's native Mihon runtime.
private val LEGACY_TACHIYOMI_BRIDGES = setOf(
    "com.programmersbox.tachiyomibridge",
    "com.programmersbox.customtachiyomibridge",
)

//TODO: Abstract this out to a kmp library
// expect/actual class
// koin for DI
// It'll have the load and blockingLoad functions
actual class SourceLoader(
    application: Application,
    private val context: Context,
    sourceType: String,
    private val sourceRepository: SourceRepository,
    private val nativeCatalogs: NativeCatalogs = NativeCatalogs(emptyList()),
    private val modelMapper: ModelMapper = ModelMapper(application),
) {
    private val extensionLoader = ExtensionLoader<Any, List<KmpSourceInformation>>(
        context,
        "$EXTENSION_FEATURE.$sourceType",
        METADATA_CLASS,
    ) { t, a, p ->
        if (nativeCatalogs.catalogs.isNotEmpty() && p.packageName in LEGACY_TACHIYOMI_BRIDGES) {
            return@ExtensionLoader emptyList()
        }
        when (t) {
            is ApiService -> listOf(
                SourceInformation(
                    apiService = t,
                    name = a.metaData.getString(METADATA_NAME) ?: "Nothing",
                    icon = context.packageManager.getApplicationIcon(p.packageName),
                    packageName = p.packageName,
                )
            )

            is ExternalCustomApiServicesCatalog -> {
                runBlocking { t.initialize(application) }
                t.getSources().map { it.copy(catalog = t) }
            }

            is ExternalApiServicesCatalog -> {
                runBlocking { t.initialize(application) }
                t.getSources().map { it.copy(catalog = t) }
            }

            is ApiServicesCatalog -> t.createSources().map {
                SourceInformation(
                    apiService = it,
                    name = a.metaData.getString(METADATA_NAME) ?: "Nothing",
                    icon = context.packageManager.getApplicationIcon(p.packageName),
                    packageName = p.packageName,
                    catalog = t,
                )
            }

            else -> emptyList()
        }.map { modelMapper.mapSourceInformation(it) }
    }

    private val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or PackageManager.GET_SIGNING_CERTIFICATES

    private val extensionType = "$EXTENSION_FEATURE.$sourceType"

    init {
        val scope = CoroutineScope(Dispatchers.IO)
        val uninstallApplication: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (intent.dataString == null) return
                val packageString = intent.dataString.orEmpty().removePrefix("package:")
                if (intent.action == Intent.ACTION_PACKAGE_REMOVED &&
                    sourceRepository.list.any { it.packageName == packageString }
                ) {
                    scope.launch { blockingLoad() }
                    return
                }
                val isOtakuExtension = runCatching {
                    val p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context?.packageManager?.getPackageInfo(
                            packageString,
                            PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong())
                        )
                    } else {
                        context?.packageManager?.getPackageInfo(packageString, PACKAGE_FLAGS)
                    }
                    checkNotNull(p)
                    val s = sourceRepository.list
                        .filter { it.catalog is ExternalApiServicesCatalog }
                        .any { (it.catalog as ExternalApiServicesCatalog).shouldReload(packageString, p) }
                    p.reqFeatures?.any { it.name == extensionType } == true || s ||
                        nativeCatalogs.catalogs.any { it.shouldReload(packageString) }
                }.getOrDefault(false)

                if (!isOtakuExtension) return
                scope.launch {
                    when (intent.action) {
                        Intent.ACTION_PACKAGE_REPLACED -> blockingLoad()
                        Intent.ACTION_PACKAGE_ADDED -> blockingLoad()
                        Intent.ACTION_PACKAGE_REMOVED -> blockingLoad()
                    }
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        intentFilter.addDataScheme("package")
        context.registerReceiver(uninstallApplication, intentFilter)
    }

    private suspend fun nativeSources(): List<KmpSourceInformation> = withContext(Dispatchers.IO) {
        nativeCatalogs.catalogs.flatMap { catalog ->
            runCatching { catalog.initialize(); catalog.getSources() }
                .onFailure { it.printStackTrace() }
                .getOrDefault(emptyList())
        }
    }

    actual fun load() {
        sourceRepository.setSources(
            (extensionLoader.loadExtensions().flatten() + runBlocking { nativeSources() })
                .sortedBy { it.apiService.serviceName }
        )
    }

    actual suspend fun blockingLoad() {
        sourceRepository.setSources(
            (extensionLoader.loadExtensionsBlocking().flatten() + nativeSources())
                .sortedBy { it.apiService.serviceName }
        )
    }
}